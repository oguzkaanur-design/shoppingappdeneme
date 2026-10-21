package com.shoppingapp.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PfandQuantityTest {
    @Test
    fun parsesPositiveWholeBottleCounts() {
        assertEquals(35, PfandQuantity.parse("35"))
        assertEquals(1, PfandQuantity.parse("1"))
        assertEquals(9999, PfandQuantity.parse("9999"))
    }

    @Test
    fun rejectsEmptyZeroNonIntegerAndOutOfRangeCounts() {
        assertNull(PfandQuantity.parse(""))
        assertNull(PfandQuantity.parse("0"))
        assertNull(PfandQuantity.parse("-3"))
        assertNull(PfandQuantity.parse("2.5"))
        assertNull(PfandQuantity.parse("10000"))
        assertNull(PfandQuantity.parse("999999999999999999999"))
    }
}
