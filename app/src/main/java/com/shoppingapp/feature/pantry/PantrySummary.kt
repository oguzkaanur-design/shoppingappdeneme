package com.shoppingapp.feature.pantry

import com.shoppingapp.data.model.PantryRow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Stok seviyesi: bitti / kritik eşiğin altında / yeterli. */
enum class PantryLevel { EMPTY, LOW, OK }

/** Son kullanma tarihi durumu. */
enum class ExpiryLevel { NONE, EXPIRED, SOON, LATER }

data class PantryStatus(
    val level: PantryLevel,
    val expiryLevel: ExpiryLevel,
    val expiresInDays: Long?
) {
    val needsRestock: Boolean
        get() = level == PantryLevel.EMPTY || level == PantryLevel.LOW

    val isUrgent: Boolean
        get() = needsRestock ||
            expiryLevel == ExpiryLevel.EXPIRED ||
            expiryLevel == ExpiryLevel.SOON
}

/**
 * Kiler kuralları saf fonksiyonlar olarak burada durur: eşik altına düşen ürün,
 * son kullanma tarihi yaklaşan ürün ve sıralama tek yerden belirlenir. Böylece
 * ekran ve bildirim aynı kuralı kullanır.
 */
object PantrySummary {

    /** Bu kadar gün kala "son kullanma yaklaşıyor" uyarısı verilir. */
    const val EXPIRY_SOON_DAYS = 3L

    fun evaluate(
        row: PantryRow,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): PantryStatus {
        val level = when {
            row.currentQuantity <= 0.0 -> PantryLevel.EMPTY
            row.currentQuantity <= row.minThreshold -> PantryLevel.LOW
            else -> PantryLevel.OK
        }

        val expiryEpoch = row.expiryDate
            ?: return PantryStatus(level = level, expiryLevel = ExpiryLevel.NONE, expiresInDays = null)

        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val expiryDay = Instant.ofEpochMilli(expiryEpoch).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(today, expiryDay)
        val expiryLevel = when {
            days < 0 -> ExpiryLevel.EXPIRED
            days <= EXPIRY_SOON_DAYS -> ExpiryLevel.SOON
            else -> ExpiryLevel.LATER
        }
        return PantryStatus(level = level, expiryLevel = expiryLevel, expiresInDays = days)
    }

    /** Acil olan üstte: bitti → kritik → süresi geçmiş → süresi yaklaşan → diğerleri. */
    fun sort(
        rows: List<PantryRow>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PantryRow> = rows.sortedWith(
        compareBy(
            { rank(evaluate(it, nowMillis, zone)) },
            { it.name.lowercase() }
        )
    )

    /** Alışveriş listesine eklenmesi gerekenler (bitti veya eşik altında). */
    fun restockNeeded(
        rows: List<PantryRow>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PantryRow> = rows
        .filter { evaluate(it, nowMillis, zone).needsRestock }
        .sortedBy { it.name.lowercase() }

    /** Son kullanma tarihi geçmiş veya yaklaşan ürünler. */
    fun expiringSoon(
        rows: List<PantryRow>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PantryRow> = rows
        .filter {
            val expiry = evaluate(it, nowMillis, zone).expiryLevel
            expiry == ExpiryLevel.EXPIRED || expiry == ExpiryLevel.SOON
        }
        .sortedWith(compareBy({ it.expiryDate ?: Long.MAX_VALUE }, { it.name.lowercase() }))

    private fun rank(status: PantryStatus): Int = when {
        status.level == PantryLevel.EMPTY -> 0
        status.level == PantryLevel.LOW -> 1
        status.expiryLevel == ExpiryLevel.EXPIRED -> 2
        status.expiryLevel == ExpiryLevel.SOON -> 3
        else -> 4
    }
}
