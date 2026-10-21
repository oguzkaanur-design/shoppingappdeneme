package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FlyerCatalogUrlTest {

    private val store = FlyerStores.byId("lidl")

    @Test
    fun addsADailyVersionParameter() {
        assertEquals(
            "https://www.marktguru.de/rp/lidl-prospekte?cb=20261021",
            FlyerCatalogUrl.forDay(store, LocalDate.of(2026, 10, 21))
        )
    }

    @Test
    fun changesTheParameterEveryDay() {
        val monday = FlyerCatalogUrl.forDay(store, LocalDate.of(2026, 10, 19))
        val tuesday = FlyerCatalogUrl.forDay(store, LocalDate.of(2026, 10, 20))

        assertEquals("https://www.marktguru.de/rp/lidl-prospekte?cb=20261019", monday)
        assertEquals("https://www.marktguru.de/rp/lidl-prospekte?cb=20261020", tuesday)
    }
}
