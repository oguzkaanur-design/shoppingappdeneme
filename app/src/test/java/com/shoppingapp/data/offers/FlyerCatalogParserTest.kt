package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FlyerCatalogParserTest {

    private val store = FlyerStore(
        id = "penny",
        name = "PENNY",
        shortName = "PENNY",
        brandColor = 0xFFE30613,
        catalogUrl = "https://www.marktguru.de/rp/penny-prospekte",
        pageUrl = "https://www.penny.de/angebote"
    )

    private val now: Instant = Instant.parse("2026-10-21T09:00:00Z")

    private val catalog = """
        {"config":{"page":1},"data":{"advertiser":{"name":"PENNY","uniqueName":"penny"},"leaflets":[
          {"id":298527,"mainLeafletId":6393372,"pageCount":44,"offerCount":317,
           "validFrom":"2026-10-18T22:00:00Z","validTo":"2026-10-24T21:59:00Z"},
          {"id":298617,"mainLeafletId":6411762,"pageCount":72,"offerCount":400,
           "validFrom":"2026-10-25T23:00:00Z","validTo":"2026-10-31T22:59:59Z"},
          {"id":296083,"mainLeafletId":6236223,"pageCount":48,
           "validFrom":"2026-09-27T22:00:00Z","validTo":"2026-10-02T21:59:00Z"},
          {"id":298527,"mainLeafletId":6393372,"pageCount":44,"offerCount":317,
           "validFrom":"2026-10-18T22:00:00Z","validTo":"2026-10-24T21:59:00Z"},
          {"id":11,"mainLeafletId":12,"pageCount":0,
           "validFrom":"2026-10-18T22:00:00Z","validTo":"2026-10-24T21:59:00Z"},
          {"id":13,"mainLeafletId":14,"pageCount":9}
        ]}}
    """.trimIndent()

    private fun page(vararg scripts: String): String = buildString {
        append("<html><head>")
        scripts.forEach { append("<script type=\"application/json\">").append(it).append("</script>") }
        append("</head><body>market</body></html>")
    }

    @Test
    fun readsLeafletsWithPagesAndValidity() {
        val flyers = FlyerCatalogParser.parse(page(catalog), store, now)

        assertEquals(3, flyers.size)
        val current = flyers.first()
        assertEquals(6393372L, current.leafletId)
        assertEquals(44, current.pageCount)
        assertEquals(317, current.offerCount)
        assertEquals("penny", current.storeId)
        assertEquals("PENNY", current.storeName)
        assertEquals(Instant.parse("2026-10-18T22:00:00Z"), current.validFrom)
        assertEquals(Instant.parse("2026-10-24T21:59:00Z"), current.validTo)
    }

    @Test
    fun ordersCurrentThenUpcomingThenPastAndDropsDuplicates() {
        val flyers = FlyerCatalogParser.parse(page(catalog), store, now)

        assertEquals(listOf(6393372L, 6411762L, 6236223L), flyers.map { it.leafletId })
        assertEquals(listOf(FlyerPhase.CURRENT, FlyerPhase.UPCOMING, FlyerPhase.PAST), flyers.map { it.phase(now) })
    }

    @Test
    fun ignoresLeafletsWithoutPagesOrValidity() {
        val flyers = FlyerCatalogParser.parse(page(catalog), store, now)

        assertTrue(flyers.none { it.leafletId == 12L })
        assertTrue(flyers.none { it.leafletId == 14L })
    }

    @Test
    fun findsLeafletsNestedDeeperInTheJson() {
        val nested = """
            {"content":[{"sections":[{"body":{"leaflets":[
              {"mainLeafletId":777,"pageCount":12,"validFrom":"2026-10-18T22:00:00Z","validTo":"2026-10-24T21:59:00Z"}
            ]}}]}]}
        """.trimIndent()

        val flyers = FlyerCatalogParser.parse(page(nested), store, now)

        assertEquals(1, flyers.size)
        assertEquals(777L, flyers.first().leafletId)
        assertEquals(12, flyers.first().pageCount)
    }

    @Test
    fun usesTheFirstJsonBlockThatContainsALeafletList() {
        val otherBlock = """{"config":{"leafletCount":0}}"""
        val flyers = FlyerCatalogParser.parse(page(otherBlock, catalog), store, now)

        assertEquals(3, flyers.size)
    }

    @Test
    fun returnsEmptyWhenPageHasNoJsonOrIsNotParseable() {
        assertTrue(FlyerCatalogParser.parse("<html><body>boş</body></html>", store, now).isEmpty())
        assertTrue(FlyerCatalogParser.parse(page("{not json"), store, now).isEmpty())
    }
}
