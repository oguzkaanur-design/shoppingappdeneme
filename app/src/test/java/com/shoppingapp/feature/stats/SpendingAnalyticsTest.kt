package com.shoppingapp.feature.stats

import com.shoppingapp.data.model.PurchaseHistoryLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpendingAnalyticsTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val now = LocalDate.of(2026, 10, 24).atTime(12, 0)
        .atZone(zone).toInstant().toEpochMilli()

    private fun line(
        date: LocalDate,
        price: Double,
        quantity: Double = 1.0,
        historyId: Long = 1,
        productId: Long = 1,
        storeId: Long = 1,
        categoryId: Long = 2,
        name: String = "Süt"
    ) = PurchaseHistoryLine(
        historyId = historyId,
        productId = productId,
        name = name,
        unit = "L",
        categoryId = categoryId,
        pfandAmount = 0.0,
        priceEuro = price,
        quantity = quantity,
        purchaseDate = date.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000,
        storeId = storeId
    )

    @Test
    fun `haftalik toplamlar pazartesi baslangicli`() {
        val lines = listOf(
            line(LocalDate.of(2026, 9, 30), 2.0),
            line(LocalDate.of(2026, 10, 14), 5.0),
            line(LocalDate.of(2026, 10, 21), 10.0)
        )

        val weekly = SpendingAnalytics.weekly(lines, weeks = 2, nowMillis = now, zone = zone)

        assertEquals(2, weekly.size)
        assertEquals(5.0, weekly[0].total, 0.001)
        assertEquals(10.0, weekly[1].total, 0.001)
    }

    @Test
    fun `aylik toplamlar ay sinirinda dogru`() {
        val lines = listOf(
            line(LocalDate.of(2026, 9, 30), 2.0),
            line(LocalDate.of(2026, 10, 1), 3.0),
            line(LocalDate.of(2026, 10, 21), 10.0)
        )

        val monthly = SpendingAnalytics.monthly(lines, months = 2, nowMillis = now, zone = zone)

        assertEquals(2.0, monthly[0].total, 0.001)
        assertEquals(13.0, monthly[1].total, 0.001)
    }

    @Test
    fun `adet fiyatla carpilir`() {
        val lines = listOf(line(LocalDate.of(2026, 10, 20), 1.5, quantity = 4.0))
        assertEquals(6.0, SpendingAnalytics.total(lines), 0.001)
    }

    @Test
    fun `kategori dagilimi buyukten kucuge`() {
        val lines = listOf(
            line(LocalDate.of(2026, 10, 20), 2.0, categoryId = 1),
            line(LocalDate.of(2026, 10, 20), 8.0, categoryId = 2, productId = 2),
            line(LocalDate.of(2026, 10, 20), 4.0, categoryId = 2, productId = 3)
        )

        val categories = SpendingAnalytics.byCategory(lines)

        assertEquals(listOf(2L, 1L), categories.map { it.categoryId })
        assertEquals(12.0, categories[0].total, 0.001)
        assertEquals(2, categories[0].lineCount)
    }

    @Test
    fun `market karsilastirmasi ziyaret basina ortalama`() {
        val lines = listOf(
            line(LocalDate.of(2026, 10, 10), 10.0, historyId = 1, storeId = 1),
            line(LocalDate.of(2026, 10, 11), 20.0, historyId = 2, storeId = 1),
            line(LocalDate.of(2026, 10, 12), 12.0, historyId = 3, storeId = 2)
        )

        val stores = SpendingAnalytics.byStore(lines)

        assertEquals(listOf(1L, 2L), stores.map { it.storeId })
        assertEquals(15.0, stores[0].averageBasket, 0.001)
        assertEquals(12.0, stores[1].averageBasket, 0.001)
    }

    @Test
    fun `fiyat trendi son iki alisverisi karsilastirir`() {
        val lines = listOf(
            line(LocalDate.of(2026, 9, 1), 1.09, historyId = 1),
            line(LocalDate.of(2026, 10, 1), 0.99, historyId = 2),
            line(LocalDate.of(2026, 10, 20), 1.19, historyId = 3)
        )

        val trend = SpendingAnalytics.priceTrends(lines).single()

        assertEquals(1.19, trend.lastPrice, 0.001)
        assertEquals(0.99, trend.previousPrice!!, 0.001)
        assertEquals(3, trend.samples)
        assertEquals(0.2, trend.change!!, 0.001)
        assertTrue(trend.changePercent!! > 20.0)
    }

    @Test
    fun `tek seferlik ve fiyatsiz urunler trende girmez`() {
        val lines = listOf(
            line(LocalDate.of(2026, 10, 1), 1.09, historyId = 1, productId = 1),
            line(LocalDate.of(2026, 10, 2), 0.0, historyId = 2, productId = 2),
            line(LocalDate.of(2026, 10, 3), 0.0, historyId = 3, productId = 2)
        )

        assertTrue(SpendingAnalytics.priceTrends(lines).isEmpty())
    }

    @Test
    fun `ayni alisveristeki cift satir trendi bozmaz`() {
        val lines = listOf(
            line(LocalDate.of(2026, 10, 1), 1.09, historyId = 1),
            line(LocalDate.of(2026, 10, 5), 0.99, historyId = 2),
            line(LocalDate.of(2026, 10, 5), 0.99, historyId = 2)
        )

        val trend = SpendingAnalytics.priceTrends(lines).single()
        assertEquals(2, trend.samples)
        assertEquals(0.99, trend.lastPrice, 0.001)
    }

    @Test
    fun `bos gecmis sifir dondurur`() {
        assertEquals(0.0, SpendingAnalytics.total(emptyList()), 0.001)
        assertTrue(SpendingAnalytics.weekly(emptyList(), weeks = 3, nowMillis = now, zone = zone).all { it.total == 0.0 })
        assertTrue(SpendingAnalytics.priceTrends(emptyList()).isEmpty())
    }
}
