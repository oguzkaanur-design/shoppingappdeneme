package com.shoppingapp.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProduceCatalogTest {
    @Test
    fun catalogContainsCommonProduceWithGermanNames() {
        assertTrue(ProduceCatalog.items.size >= 50)
        assertTrue(ProduceCatalog.items.any { it.turkish == "Elma" && it.german == "Apfel" })
        assertTrue(ProduceCatalog.items.any { it.turkish == "Patates" && it.german == "Kartoffel" })
    }

    @Test
    fun searchMatchesTurkishAndGermanNames() {
        assertTrue(ProduceCatalog.search("elma").any { it.turkish == "Elma" })
        assertTrue(ProduceCatalog.search("Apfel").any { it.turkish == "Elma" })
        assertTrue(ProduceCatalog.search("Kartoffel").any { it.turkish == "Patates" })
        assertEquals("Lahana", ProduceCatalog.search("weisskohl").first { it.german == "Weißkohl" }.turkish)
    }

    @Test
    fun emptySearchReturnsFullCatalog() {
        assertEquals(ProduceCatalog.items.size, ProduceCatalog.search("  ").size)
    }
}
