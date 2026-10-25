package com.shoppingapp.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyFormatTest {

    @Test
    fun formatsEuroWithGermanSeparators() {
        assertEquals("12,40 €", MoneyFormat.amount(12.4, Currency.EUR))
        assertEquals("1.234,56 €", MoneyFormat.amount(1234.56, Currency.EUR))
        assertEquals("0,05 €", MoneyFormat.amount(0.05, Currency.EUR))
    }

    @Test
    fun formatsLiraAndZlotyWithTheirSymbols() {
        assertEquals("34,90 ₺", MoneyFormat.amount(34.9, Currency.TRY))
        assertEquals("1.000,00 ₺", MoneyFormat.amount(1000.0, Currency.TRY))
        assertEquals("12,50 zł", MoneyFormat.amount(12.5, Currency.PLN))
    }

    @Test
    fun formatsDollarAndPoundWithPrefixAndDot() {
        assertEquals("$12.40", MoneyFormat.amount(12.4, Currency.USD))
        assertEquals("$1,234.56", MoneyFormat.amount(1234.56, Currency.USD))
        assertEquals("£9.99", MoneyFormat.amount(9.99, Currency.GBP))
    }

    @Test
    fun forintHasNoFractionDigits() {
        assertEquals("1.500 Ft", MoneyFormat.amount(1500.4, Currency.HUF))
    }

    @Test
    fun negativeAmountsUseTheMinusSign() {
        assertEquals("−5,00 €", MoneyFormat.amount(-5.0, Currency.EUR))
        assertEquals("−5,00 €", MoneyFormat.signed(-5.0, Currency.EUR))
        assertEquals("+5,00 €", MoneyFormat.signed(5.0, Currency.EUR))
    }

    @Test
    fun roundsHalfUpLikeABankWould() {
        assertEquals("0,13 €", MoneyFormat.amount(0.125, Currency.EUR))
        assertEquals("2,68 €", MoneyFormat.amount(2.675, Currency.EUR))
    }

    @Test
    fun parsesAmountsWithSymbolsAndSeparators() {
        assertEquals(12.5, MoneyFormat.parse("12,50", Currency.EUR))
        assertEquals(12.5, MoneyFormat.parse("12.50", Currency.EUR))
        assertEquals(12.5, MoneyFormat.parse(" 12,50 € ", Currency.EUR))
        assertEquals(34.9, MoneyFormat.parse("₺34,90", Currency.TRY))
        assertEquals(1234.56, MoneyFormat.parse("1.234,56", Currency.EUR))
        assertEquals(1234.56, MoneyFormat.parse("$1,234.56", Currency.USD))
        assertEquals(7.0, MoneyFormat.parse("7", Currency.EUR))
    }

    @Test
    fun rejectsEmptyZeroNegativeAndGarbage() {
        assertNull(MoneyFormat.parse("", Currency.EUR))
        assertNull(MoneyFormat.parse("   ", Currency.EUR))
        assertNull(MoneyFormat.parse("0", Currency.EUR))
        assertNull(MoneyFormat.parse("-3,00", Currency.EUR))
        assertNull(MoneyFormat.parse("abc", Currency.EUR))
        assertNull(MoneyFormat.parse(",", Currency.EUR))
    }

    @Test
    fun formattedTextRoundTripsThroughParse() {
        listOf(Currency.EUR, Currency.TRY, Currency.USD, Currency.PLN).forEach { currency ->
            listOf(0.05, 7.0, 12.5, 1234.56).forEach { value ->
                assertEquals(
                    "$currency $value",
                    value,
                    MoneyFormat.parse(MoneyFormat.amount(value, currency), currency)!!,
                    0.0001
                )
            }
        }
    }

    @Test
    fun unknownCodeFallsBackToEuro() {
        assertEquals(Currency.EUR, Currency.fromCode(null))
        assertEquals(Currency.EUR, Currency.fromCode("XYZ"))
        assertEquals(Currency.TRY, Currency.fromCode("try"))
    }
}
