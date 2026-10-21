package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FlyerOfferParserTest {

    private val leafletJson = """
        {"id":6364267,"name":"REWE Erfurt","pageImages":{"count":30},"children":[
          {"id":"offers/25371677","coordinates":{"fromX":0.178,"fromY":0.253,"tox":0.671,"toy":0.443},
           "pageIndex":0,"scannable":false,"incentive":0.0},
          {"id":"offers/25347767","coordinates":{"fromX":0.8,"fromY":0.9,"tox":0.8,"toy":0.95},
           "pageIndex":0},
          {"id":"offers/25399867","coordinates":{"fromX":0.1,"fromY":0.1,"tox":0.2,"toy":0.2},
           "pageIndex":3}
        ]}
    """.trimIndent()

    private val offersJson = """
        {"totalResults":2,"results":[
          {"id":25371677,"description":"versch. Sorten, je 20 x 0,5-l-Fl.-Kasten zzgl. 3.10 Pfand",
           "price":10.99,"oldPrice":12.99,
           "validityDates":[{"from":"2026-10-18T22:00:00Z","to":"2026-10-24T21:59:00Z"}],
           "brand":{"name":"Beck's"},"product":{"name":"Pils"}},
          {"id":25347767,"description":"HINWEIS: MIT APP 0,10 EUR REWE BONUS",
           "price":0.99,"oldPrice":null,
           "validityDates":[{"from":"2026-10-18T22:00:00Z","to":"2026-10-24T21:59:00Z"}],
           "brand":null,"product":{"name":"~twxau~"}}
        ]}
    """.trimIndent()

    @Test
    fun parsesHotspotsAndDropsDegenerateAreas() {
        val spots = FlyerOfferParser.parseHotspots(leafletJson)

        assertEquals(2, spots.size)
        val first = spots.first()
        assertEquals(25371677L, first.offerId)
        assertEquals(0, first.pageIndex)
        assertEquals(0.178f, first.fromX, 0.0001f)
        assertEquals(0.671f, first.toX, 0.0001f)
        assertEquals(0.493f, first.width, 0.0001f)
        assertTrue(spots.none { it.offerId == 25347767L })
    }

    @Test
    fun readsLeafletName() {
        assertEquals("REWE Erfurt", FlyerOfferParser.parseLeafletName(leafletJson))
        assertNull(FlyerOfferParser.parseLeafletName("{}"))
    }

    @Test
    fun parsesOffersWithBrandProductPriceAndValidity() {
        val offers = FlyerOfferParser.parsePageOffers(offersJson, pageIndex = 0)

        assertEquals(2, offers.size)
        val beer = offers.first()
        assertEquals(25371677L, beer.id)
        assertEquals("Beck's Pils", beer.name)
        assertEquals(10.99, beer.price!!, 0.001)
        assertEquals(12.99, beer.oldPrice!!, 0.001)
        assertEquals(Instant.parse("2026-10-18T22:00:00Z"), beer.validFrom)
        assertEquals(Instant.parse("2026-10-24T21:59:00Z"), beer.validTo)
        assertEquals(0, beer.pageIndex)
    }

    @Test
    fun joinsOffersWithTheirPageAreas() {
        val spots = FlyerOfferParser.parseHotspots(leafletJson)
        val offers = FlyerOfferParser.parsePageOffers(offersJson, pageIndex = 0)

        val joined = FlyerOfferParser.attachHotspots(offers, spots)

        val beer = joined.first { it.id == 25371677L }
        assertEquals(0.253f, beer.hotspot!!.fromY, 0.0001f)
        assertNull(joined.first { it.id == 25347767L }.hotspot)
    }

    @Test
    fun fallsBackToDescriptionWhenNamesArePlaceholders() {
        val offers = FlyerOfferParser.parsePageOffers(offersJson, pageIndex = 0)
        val placeholder = offers.first { it.id == 25347767L }

        assertEquals("HINWEIS: MIT APP 0,10 EUR REWE BONUS", placeholder.name)
    }

    @Test
    fun buildsNameFromWhicheverPartExists() {
        assertEquals("ja! Brotaufstrich", FlyerOfferParser.displayName("ja!", "Brotaufstrich", ""))
        assertEquals("Brotaufstrich", FlyerOfferParser.displayName(null, "Brotaufstrich", ""))
        assertEquals("ja!", FlyerOfferParser.displayName("ja!", null, ""))
        assertEquals("Ürün", FlyerOfferParser.displayName(null, "~twxde~", ""))
    }

    @Test
    fun formatsPricesInGermanNotation() {
        assertEquals("10,99 €", FlyerPriceLabel.format(10.99))
        assertEquals("1,19 €", FlyerPriceLabel.format(1.19))
        assertNull(FlyerPriceLabel.format(null))
    }

    @Test
    fun returnsEmptyForBrokenJson() {
        assertTrue(FlyerOfferParser.parseHotspots("{ nope").isEmpty())
        assertTrue(FlyerOfferParser.parsePageOffers("[]", 0).isEmpty())
    }
}
