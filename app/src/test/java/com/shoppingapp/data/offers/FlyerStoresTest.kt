package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlyerStoresTest {

    @Test
    fun coversTheFiveSupportedStoresWithUniqueIds() {
        assertEquals(5, FlyerStores.all.size)
        assertEquals(FlyerStores.all.size, FlyerStores.all.map { it.id }.distinct().size)
        assertEquals(
            listOf("rewe", "edeka", "aldi-sued", "lidl", "penny"),
            FlyerStores.all.map { it.id }
        )
    }

    @Test
    fun everyStoreHasHttpsCatalogAndFallbackPages() {
        FlyerStores.all.forEach { store ->
            assertTrue(store.catalogUrl.startsWith("https://"))
            assertTrue(store.pageUrl.startsWith("https://"))
            assertTrue(store.catalogUrl.contains(store.id))
        }
    }

    @Test
    fun lookupFallsBackToTheFirstStore() {
        assertEquals("lidl", FlyerStores.byId("lidl").id)
        assertEquals(FlyerStores.all.first(), FlyerStores.byId("unknown"))
    }
}
