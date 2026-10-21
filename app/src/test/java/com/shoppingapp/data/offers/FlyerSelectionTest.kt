package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FlyerSelectionTest {

    private val now: Instant = Instant.parse("2026-10-21T09:00:00Z")

    private fun flyer(id: Long, from: String, to: String) = Flyer(
        storeId = "lidl",
        storeName = "Lidl",
        leafletId = id,
        pageCount = 20,
        validFrom = Instant.parse(from),
        validTo = Instant.parse(to)
    )

    private val past = flyer(1, "2026-10-11T22:00:00Z", "2026-10-17T21:59:00Z")
    private val current = flyer(2, "2026-10-18T22:00:00Z", "2026-10-24T21:59:00Z")
    private val upcoming = flyer(3, "2026-10-25T23:00:00Z", "2026-10-31T22:59:59Z")

    @Test
    fun hidesArchivedFlyersAndKeepsTheCurrentWeek() {
        assertEquals(listOf(current, upcoming), FlyerSelection.relevant(listOf(current, upcoming, past), now))
    }

    @Test
    fun keepsTheMostRecentFlyerWhenNoWeekIsActive() {
        val olderPast = flyer(0, "2026-10-04T22:00:00Z", "2026-10-10T21:59:00Z")

        assertEquals(listOf(past), FlyerSelection.relevant(listOf(past, olderPast), now))
    }

    @Test
    fun returnsEmptyWhenThereAreNoFlyers() {
        assertEquals(emptyList<Flyer>(), FlyerSelection.relevant(emptyList(), now))
    }

    @Test
    fun picksTheMostCompleteEditionOfTheCurrentWeek() {
        val smallEdition = flyer(4, "2026-10-18T22:00:00Z", "2026-10-24T21:59:00Z").copy(pageCount = 2)
        val largeEdition = flyer(5, "2026-10-18T22:00:00Z", "2026-10-24T21:59:00Z").copy(pageCount = 69)

        assertEquals(
            listOf(largeEdition),
            FlyerSelection.relevant(listOf(smallEdition, largeEdition), now)
        )
    }

    @Test
    fun showsOnlyTheNearestUpcomingWeek() {
        val weekAfter = flyer(6, "2026-11-01T23:00:00Z", "2026-11-07T22:59:00Z").copy(pageCount = 80)

        assertEquals(
            listOf(upcoming),
            FlyerSelection.relevant(listOf(upcoming, weekAfter), now)
        )
    }
}
