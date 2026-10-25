package com.shoppingapp.feature.stats

import com.shoppingapp.data.model.PurchaseHistoryLine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Grafikteki tek bir dönem (hafta veya ay). */
data class PeriodSpend(
    val startEpochMs: Long,
    val label: String,
    val total: Double
)

data class CategorySpend(
    val categoryId: Long,
    val total: Double,
    val lineCount: Int
)

data class StoreSpend(
    val storeId: Long,
    val total: Double,
    val visits: Int
) {
    val averageBasket: Double get() = if (visits == 0) 0.0 else total / visits
}

/**
 * Fiyat trendi: aynı ürün için son iki farklı alışverişteki birim fiyat.
 * "Süt en son €1,09, önceki €0,99" bilgisini üretir.
 */
data class PriceTrend(
    val productId: Long,
    val name: String,
    val lastPrice: Double,
    val previousPrice: Double?,
    val lastPurchaseAt: Long,
    val samples: Int
) {
    val change: Double? get() = previousPrice?.let { lastPrice - it }

    val changePercent: Double?
        get() = previousPrice?.takeIf { it > 0.0 }?.let { (lastPrice - it) / it * 100.0 }
}

/**
 * Alışveriş geçmişinden türetilen tüm istatistikler. Saf fonksiyonlar: ekran
 * yalnızca biçimlendirme yapar, hesaplar testlerle doğrulanır.
 *
 * Fiyatlar alışveriş anında kaydedildiği için ürünün bugünkü fiyatı değişse de
 * geçmiş harcama ve trend bozulmaz.
 */
object SpendingAnalytics {

    private val TR = Locale("tr", "TR")

    fun total(lines: List<PurchaseHistoryLine>): Double =
        lines.sumOf { it.priceEuro * it.quantity }

    /** İçinde bulunulan takvim ayında yapılan harcama (bütçe takibi için). */
    fun currentMonthTotal(
        lines: List<PurchaseHistoryLine>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Double {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val month = YearMonth.of(now.year, now.month)
        return lines
            .filter {
                val date = Instant.ofEpochMilli(it.purchaseDate).atZone(zone)
                YearMonth.of(date.year, date.month) == month
            }
            .sumOf { it.priceEuro * it.quantity }
    }

    /** Son [weeks] hafta, Pazartesi başlangıçlı, eskiden yeniye. */
    fun weekly(
        lines: List<PurchaseHistoryLine>,
        weeks: Int = 6,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PeriodSpend> {
        if (weeks <= 0) return emptyList()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val formatter = DateTimeFormatter.ofPattern("d MMM", TR)

        return (weeks - 1 downTo 0).map { offset ->
            val start = currentWeekStart.minusWeeks(offset.toLong())
            val end = start.plusWeeks(1)
            PeriodSpend(
                startEpochMs = start.atStartOfDay(zone).toInstant().toEpochMilli(),
                label = start.format(formatter),
                total = sumBetween(lines, start, end, zone)
            )
        }
    }

    /** Son [months] ay, eskiden yeniye. */
    fun monthly(
        lines: List<PurchaseHistoryLine>,
        months: Int = 6,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PeriodSpend> {
        if (months <= 0) return emptyList()
        val thisMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone))
        val formatter = DateTimeFormatter.ofPattern("MMM", TR)

        return (months - 1 downTo 0).map { offset ->
            val month = thisMonth.minusMonths(offset.toLong())
            val start = month.atDay(1)
            val end = month.plusMonths(1).atDay(1)
            PeriodSpend(
                startEpochMs = start.atStartOfDay(zone).toInstant().toEpochMilli(),
                label = month.atDay(1).format(formatter),
                total = sumBetween(lines, start, end, zone)
            )
        }
    }

    fun byCategory(lines: List<PurchaseHistoryLine>): List<CategorySpend> =
        lines.groupBy { it.categoryId }
            .map { (categoryId, rows) ->
                CategorySpend(
                    categoryId = categoryId,
                    total = rows.sumOf { it.priceEuro * it.quantity },
                    lineCount = rows.size
                )
            }
            .filter { it.total > 0.0 }
            .sortedByDescending { it.total }

    fun byStore(lines: List<PurchaseHistoryLine>): List<StoreSpend> =
        lines.groupBy { it.storeId }
            .map { (storeId, rows) ->
                StoreSpend(
                    storeId = storeId,
                    total = rows.sumOf { it.priceEuro * it.quantity },
                    visits = rows.map { it.historyId }.distinct().size
                )
            }
            .filter { it.total > 0.0 }
            .sortedByDescending { it.total }

    /**
     * Ürün başına son iki farklı alışverişteki fiyat. Yalnızca en az iki kez
     * alınmış ve fiyatı girilmiş ürünler döner; tek seferlik alımlar trend sayılmaz.
     */
    fun priceTrends(lines: List<PurchaseHistoryLine>, limit: Int = 8): List<PriceTrend> {
        val usable = lines.filter { it.priceEuro > 0.0 }
        val deduped = usable
            .groupBy { "${it.historyId}-${it.productId}" }
            .map { (_, rows) -> rows.last() }

        return deduped
            .groupBy { it.productId }
            .mapNotNull { (productId, rows) ->
                val ordered = rows.sortedBy { it.purchaseDate }
                if (ordered.size < 2) return@mapNotNull null
                val last = ordered.last()
                val previous = ordered[ordered.size - 2]
                PriceTrend(
                    productId = productId,
                    name = last.name,
                    lastPrice = last.priceEuro,
                    previousPrice = previous.priceEuro,
                    lastPurchaseAt = last.purchaseDate,
                    samples = ordered.size
                )
            }
            .sortedByDescending { it.lastPurchaseAt }
            .take(limit)
    }

    private fun sumBetween(
        lines: List<PurchaseHistoryLine>,
        start: LocalDate,
        endExclusive: LocalDate,
        zone: ZoneId
    ): Double {
        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        return lines
            .filter { it.purchaseDate >= startMs && it.purchaseDate < endMs }
            .sumOf { it.priceEuro * it.quantity }
    }
}
