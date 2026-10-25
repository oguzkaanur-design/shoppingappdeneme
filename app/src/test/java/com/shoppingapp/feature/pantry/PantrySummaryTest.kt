package com.shoppingapp.feature.pantry

import com.shoppingapp.data.model.PantryRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PantrySummaryTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val now: Long = LocalDate.of(2026, 10, 24)
        .atStartOfDay(zone).toInstant().toEpochMilli()

    private fun row(
        id: Long = 1,
        quantity: Double = 2.0,
        threshold: Double = 1.0,
        expiry: LocalDate? = null,
        name: String = "Süt"
    ) = PantryRow(
        id = id,
        productId = id,
        name = name,
        unit = "L",
        categoryId = 2,
        currentQuantity = quantity,
        minThreshold = threshold,
        expiryDate = expiry?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
        defaultStoreId = 1,
        priceEuro = 1.09
    )

    @Test
    fun `stok seviyeleri`() {
        assertEquals(PantryLevel.EMPTY, PantrySummary.evaluate(row(quantity = 0.0), now, zone).level)
        assertEquals(PantryLevel.LOW, PantrySummary.evaluate(row(quantity = 1.0), now, zone).level)
        assertEquals(PantryLevel.OK, PantrySummary.evaluate(row(quantity = 3.0), now, zone).level)
    }

    @Test
    fun `esik sifirsa sadece biten urun kritik sayilir`() {
        assertTrue(PantrySummary.evaluate(row(quantity = 0.0, threshold = 0.0), now, zone).needsRestock)
        assertFalse(PantrySummary.evaluate(row(quantity = 0.1, threshold = 0.0), now, zone).needsRestock)
    }

    @Test
    fun `son kullanma tarihi gun farkiyla siniflanir`() {
        val expired = PantrySummary.evaluate(row(expiry = LocalDate.of(2026, 10, 20)), now, zone)
        val soon = PantrySummary.evaluate(row(expiry = LocalDate.of(2026, 10, 26)), now, zone)
        val later = PantrySummary.evaluate(row(expiry = LocalDate.of(2026, 11, 10)), now, zone)

        assertEquals(ExpiryLevel.EXPIRED, expired.expiryLevel)
        assertEquals(-4L, expired.expiresInDays)
        assertEquals(ExpiryLevel.SOON, soon.expiryLevel)
        assertEquals(ExpiryLevel.LATER, later.expiryLevel)
    }

    @Test
    fun `tarihsiz urun SKT bilgisi tasimaz`() {
        val status = PantrySummary.evaluate(row(expiry = null), now, zone)
        assertEquals(ExpiryLevel.NONE, status.expiryLevel)
        assertEquals(null, status.expiresInDays)
    }

    @Test
    fun `kritik urunler once siralanir`() {
        val rows = listOf(
            row(id = 1, name = "Zeytin", quantity = 5.0),
            row(id = 2, name = "Süt", quantity = 0.0),
            row(id = 3, name = "Ekmek", quantity = 1.0),
            row(id = 4, name = "Yoğurt", quantity = 4.0, expiry = LocalDate.of(2026, 10, 25))
        )

        val sorted = PantrySummary.sort(rows, now, zone)
        assertEquals(listOf(2L, 3L, 4L, 1L), sorted.map { it.id })
    }

    @Test
    fun `listeye eklenecekler yalnizca biten ve kritik olanlar`() {
        val rows = listOf(
            row(id = 1, quantity = 0.0),
            row(id = 2, quantity = 1.0),
            row(id = 3, quantity = 8.0)
        )

        assertEquals(listOf(1L, 2L), PantrySummary.restockNeeded(rows, now, zone).map { it.id })
    }

    @Test
    fun `skt yaklasanlar eskiye gore siralanir`() {
        val rows = listOf(
            row(id = 1, name = "A", expiry = LocalDate.of(2026, 10, 26)),
            row(id = 2, name = "B", expiry = LocalDate.of(2026, 10, 20)),
            row(id = 3, name = "C", expiry = LocalDate.of(2027, 1, 1))
        )

        assertEquals(listOf(2L, 1L), PantrySummary.expiringSoon(rows, now, zone).map { it.id })
    }
}
