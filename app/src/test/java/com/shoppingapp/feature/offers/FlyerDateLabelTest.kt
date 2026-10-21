package com.shoppingapp.feature.offers

import com.shoppingapp.data.offers.Flyer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FlyerDateLabelTest {

    private fun flyer(from: String, to: String) = Flyer(
        storeId = "penny",
        storeName = "PENNY",
        leafletId = 1L,
        pageCount = 10,
        validFrom = Instant.parse(from),
        validTo = Instant.parse(to)
    )

    @Test
    fun showsASingleMonthRangeInBerlinTime() {
        assertEquals(
            "19–24 Eki",
            FlyerDateLabel.range(
                Instant.parse("2026-10-18T22:00:00Z"),
                Instant.parse("2026-10-24T21:59:00Z")
            )
        )
    }

    @Test
    fun showsBothMonthsWhenTheWeekCrossesAMonth() {
        // Kış saati (CET, UTC+1): 23:00Z = ertesi gün 00:00 Berlin.
        assertEquals(
            "28 Ara – 3 Oca",
            FlyerDateLabel.range(
                Instant.parse("2026-12-27T23:00:00Z"),
                Instant.parse("2027-01-03T22:59:00Z")
            )
        )
    }

    @Test
    fun labelsCurrentUpcomingAndPastPhases() {
        val current = flyer("2026-10-18T22:00:00Z", "2026-10-24T21:59:00Z")
        val upcoming = flyer("2026-10-25T23:00:00Z", "2026-10-31T22:59:59Z")
        val past = flyer("2026-10-11T22:00:00Z", "2026-10-17T21:59:00Z")
        val now = Instant.parse("2026-10-21T09:00:00Z")

        assertEquals("Bu hafta", FlyerDateLabel.phaseLabel(current, now))
        assertEquals("Gelecek hafta", FlyerDateLabel.phaseLabel(upcoming, now))
        assertEquals("Geçen hafta", FlyerDateLabel.phaseLabel(past, now))
    }

    @Test
    fun showsASingleDayForOfferValidity() {
        assertEquals("24 Eki", FlyerDateLabel.day(Instant.parse("2026-10-24T21:59:00Z")))
        assertEquals("1 Kas", FlyerDateLabel.day(Instant.parse("2026-10-31T23:00:00Z")))
    }

    @Test
    fun usesTheFlyerOverloadForItsOwnRange() {
        assertEquals("19–24 Eki", FlyerDateLabel.range(flyer("2026-10-18T22:00:00Z", "2026-10-24T21:59:00Z")))
    }
}
