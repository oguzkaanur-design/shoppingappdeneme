package com.shoppingapp.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandCatalogTest {
    @Test
    fun reweEggSearchOffersSeveralDistinctProducts() {
        val suggestions = BrandCatalog.suggest(1L, "yumurta", limit = 10)
        assertTrue(suggestions.size >= 4)
        assertTrue(suggestions.any { it.name.contains("ja!", ignoreCase = true) })
        assertTrue(suggestions.any { it.name.contains("Beste Wahl", ignoreCase = true) })
        assertEquals(suggestions.size, suggestions.map { it.name.lowercase() }.distinct().size)
    }

    @Test
    fun germanEggSearchUsesSameStoreCatalog() {
        assertEquals(
            BrandCatalog.suggest(1L, "yumurta", limit = 10).map { it.name },
            BrandCatalog.suggest(1L, "Eier", limit = 10).map { it.name }
        )
    }

    @Test
    fun eachMarketHasMultipleEggSuggestionsAndCorrectCategory() {
        for (storeId in listOf(1L, 2L, 3L, 4L, 6L)) {
            val suggestions = BrandCatalog.suggest(storeId, "Eier", limit = 10)
            assertTrue("store=$storeId", suggestions.size >= 3)
            assertTrue(suggestions.all { it.categoryId == 1L })
        }
        assertTrue(BrandCatalog.suggest(null, "yumurta").isEmpty())
        assertTrue(BrandCatalog.suggest(0L, "yumurta").isEmpty())
    }

    @Test
    fun pastaDoesNotMapToProduceCategory() {
        assertEquals(4L, BrandCatalog.detectCategory("makarna"))
        assertTrue(BrandCatalog.suggest(1L, "makarna", limit = 10).size >= 4)
    }
}
