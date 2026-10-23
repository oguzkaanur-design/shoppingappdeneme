package com.shoppingapp.data.aisle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AisleOrderTest {

    private data class Item(val id: Long, val categoryId: Long?)

    @Test
    fun `parse okur ve gecersiz degerleri atlar`() {
        assertEquals(listOf(1L, 5L, 2L), AisleOrder.parse("[1, \"5\", 2]"))
        assertEquals(emptyList<Long>(), AisleOrder.parse("[]"))
        assertEquals(emptyList<Long>(), AisleOrder.parse("bozuk json"))
        assertEquals(emptyList<Long>(), AisleOrder.parse(null))
        assertEquals(listOf(1L, 2L), AisleOrder.parse("[1, 1, 2, 0, -3]"))
    }

    @Test
    fun `siralamada olmayan kategoriler sona eklenir`() {
        val order = listOf(3L, 1L)
        val available = listOf(1L, 2L, 3L, 4L)

        assertEquals(listOf(3L, 1L, 2L, 4L), AisleOrder.forCategories(order, available))
    }

    @Test
    fun `urunler reyon sirasina gore gruplanir`() {
        val items = listOf(
            Item(1, 2),
            Item(2, 1),
            Item(3, 3),
            Item(4, 2)
        )

        val groups = AisleOrder.group(items, order = listOf(3L, 1L, 2L)) { it.categoryId }

        assertEquals(listOf(3L, 1L, 2L), groups.map { it.categoryId })
        assertEquals(listOf(1, 2, 3), groups.map { it.aisleNumber })
        assertEquals(listOf(1L, 4L), groups[2].items.map { it.id })
    }

    @Test
    fun `kategorisi olmayan urunler son grupta kaybolmaz`() {
        val items = listOf(Item(1, null), Item(2, 1), Item(3, 99))

        val groups = AisleOrder.group(items, order = listOf(1L)) { it.categoryId }

        // Sırada olmayan kategori kendi grubunu alır, kategori kimliği olmayan en sonda.
        assertEquals(listOf(1L, 99L, null), groups.map { it.categoryId })
        assertEquals(3, groups.sumOf { it.items.size })
    }

    @Test
    fun `bos liste bos grup dondurur`() {
        assertTrue(AisleOrder.group(emptyList<Item>(), listOf(1L)) { it.categoryId }.isEmpty())
    }
}
