package com.shoppingapp.onboarding

import com.shoppingapp.money.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCatalogTest {

    @Test
    fun countriesMapToTheirCurrency() {
        assertEquals(Currency.EUR, CountryCatalog.of("DE")!!.currency)
        assertEquals(Currency.EUR, CountryCatalog.of("NL")!!.currency)
        assertEquals(Currency.EUR, CountryCatalog.of("AT")!!.currency)
        assertEquals(Currency.TRY, CountryCatalog.of("TR")!!.currency)
        assertEquals(Currency.GBP, CountryCatalog.of("GB")!!.currency)
        assertEquals(Currency.USD, CountryCatalog.of("US")!!.currency)
        assertEquals(Currency.CHF, CountryCatalog.of("CH")!!.currency)
        assertEquals(Currency.PLN, CountryCatalog.of("PL")!!.currency)
    }

    @Test
    fun lookupIsCaseInsensitiveAndSafe() {
        assertEquals("TR", CountryCatalog.of("tr")!!.code)
        assertNull(CountryCatalog.of("ZZ"))
        assertNull(CountryCatalog.of(null))
    }

    @Test
    fun everyCountryHasAUniqueCodeAndOneOfTheCurrencies() {
        val codes = CountryCatalog.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(CountryCatalog.all.all { it.currency in Currency.entries })
        assertTrue(CountryCatalog.all.size >= 20)
    }

    @Test
    fun searchMatchesNameCodeAndCurrency() {
        assertEquals(listOf("TR"), CountryCatalog.search("türkiye").map { it.code })
        assertTrue(CountryCatalog.search("de").any { it.code == "DE" })
        assertTrue(CountryCatalog.search("de").any { it.code == "NL" })
        assertTrue(CountryCatalog.search("EUR").all { it.currency == Currency.EUR })
        assertEquals(CountryCatalog.all, CountryCatalog.search("  "))
    }

    @Test
    fun languageTagSuggestsACountry() {
        assertEquals("TR", CountryCatalog.suggestFor("tr").code)
        assertEquals("DE", CountryCatalog.suggestFor("de-DE").code)
        assertEquals("NL", CountryCatalog.suggestFor("nl").code)
        assertEquals("PL", CountryCatalog.suggestFor("pl-PL").code)
        assertEquals("DE", CountryCatalog.suggestFor(null).code)
        assertEquals("DE", CountryCatalog.suggestFor("xx").code)
    }
}
