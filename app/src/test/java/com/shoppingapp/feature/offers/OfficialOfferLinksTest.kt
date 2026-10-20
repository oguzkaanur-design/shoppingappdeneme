package com.shoppingapp.feature.offers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialOfferLinksTest {
    @Test
    fun eachMarketHasAnOfficialHttpsOfferLink() {
        assertEquals(5, OfficialOfferLinks.stores.size)
        assertTrue(OfficialOfferLinks.stores.all { it.url.startsWith("https://") })
        assertTrue(OfficialOfferLinks.stores.any { it.name == "REWE" && it.url.contains("rewe.de") })
        assertTrue(OfficialOfferLinks.stores.any { it.name == "EDEKA" && it.url.contains("edeka.de") })
        assertTrue(OfficialOfferLinks.stores.any { it.name == "ALDI SÜD" && it.url.contains("aldi-sued.de") })
        assertTrue(OfficialOfferLinks.stores.any { it.name == "Lidl" && it.url.contains("lidl.de") })
        assertTrue(OfficialOfferLinks.stores.any { it.name == "PENNY" && it.url.contains("penny.de") })
    }
}
