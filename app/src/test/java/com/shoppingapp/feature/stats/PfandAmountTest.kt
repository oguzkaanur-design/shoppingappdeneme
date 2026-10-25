package com.shoppingapp.feature.stats

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PfandAmountTest {

    @Test
    fun parsesWholeAndDecimalEurosToCents() {
        assertEquals(1200, PfandAmount.parseCents("12"))
        assertEquals(1250, PfandAmount.parseCents("12,5"))
        assertEquals(1250, PfandAmount.parseCents("12.50"))
        assertEquals(50, PfandAmount.parseCents(".5"))
        assertEquals(25, PfandAmount.parseCents("0,25"))
        assertEquals(1_000_000, PfandAmount.parseCents("10000"))
    }

    @Test
    fun rejectsEmptyZeroMalformedAndOutOfRangeAmounts() {
        assertNull(PfandAmount.parseCents(""))
        assertNull(PfandAmount.parseCents("   "))
        assertNull(PfandAmount.parseCents("0"))
        assertNull(PfandAmount.parseCents("0,00"))
        assertNull(PfandAmount.parseCents("-1"))
        assertNull(PfandAmount.parseCents("1,2.3"))
        assertNull(PfandAmount.parseCents("abc"))
        assertNull(PfandAmount.parseCents("1,234"))
        assertNull(PfandAmount.parseCents("10000,01"))
        assertNull(PfandAmount.parseCents("9999999"))
    }

    @Test
    fun formatRoundTripsThroughSanitizeAndParse() {
        assertEquals("12.25", PfandAmount.format(1225))
        assertEquals("0.25", PfandAmount.format(25))
        assertEquals("0.05", PfandAmount.format(5))
        assertEquals("150.00", PfandAmount.format(15_000))
        assertEquals(1225, PfandAmount.parseCents(PfandAmount.sanitize(PfandAmount.format(1225))))
        assertEquals(5, PfandAmount.parseCents(PfandAmount.sanitize(PfandAmount.format(5))))
        assertEquals("0.00", PfandAmount.format(-10))
    }

    @Test
    fun separatorFollowsTheLocale() {
        assertEquals(',', PfandAmount.separatorFor(Locale.GERMANY))
        assertEquals(',', PfandAmount.separatorFor(Locale.forLanguageTag("tr")))
        assertEquals('.', PfandAmount.separatorFor(Locale.US))
    }

    @Test
    fun sanitizeUsesTheProvidedSeparator() {
        assertEquals("7,50", PfandAmount.sanitize("7.50", ','))
        assertEquals("7,50", PfandAmount.sanitize("7,50", ','))
        assertEquals("7,5", PfandAmount.sanitize("7,5", ','))
        assertEquals("12,34", PfandAmount.sanitize("12.345", ','))
        assertEquals("1,23", PfandAmount.sanitize("1,2,3", ','))
        assertEquals("7.50", PfandAmount.sanitize("7,50", '.'))
        assertEquals("", PfandAmount.sanitize("abc", ','))
    }

    @Test
    fun formatUsesTheProvidedSeparator() {
        assertEquals("12,25", PfandAmount.format(1225, ','))
        assertEquals("0,05", PfandAmount.format(5, ','))
        assertEquals("150,00", PfandAmount.format(15_000, ','))
        assertEquals(1225, PfandAmount.parseCents(PfandAmount.sanitize(PfandAmount.format(1225, ','), ',')))
        assertEquals(5, PfandAmount.parseCents(PfandAmount.sanitize(PfandAmount.format(5, ','), ',')))
    }

    @Test
    fun sanitizeKeepsDigitsAndTwoDecimals() {
        assertEquals("7.50", PfandAmount.sanitize("7,50"))
        assertEquals("7.5", PfandAmount.sanitize("7.5"))
        assertEquals("12.34", PfandAmount.sanitize("12,345"))
        assertEquals("1.23", PfandAmount.sanitize("1.2.3"))
        assertEquals("123456", PfandAmount.sanitize("1234567"))
        assertEquals("", PfandAmount.sanitize("abc"))
    }
}
