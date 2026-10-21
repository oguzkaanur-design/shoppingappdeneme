package com.shoppingapp.feature.money

import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneySummaryTest {

    private fun entry(account: String, amount: Double) =
        MoneyEntry(account = account, amount = amount)

    @Test
    fun `nakit ve banka hareketleri kendi bakiyelerine yazilir`() {
        val balances = MoneySummary.of(
            listOf(
                entry(MoneyAccount.CASH, 50.0),
                entry(MoneyAccount.CASH, -12.25),
                entry(MoneyAccount.BANK, 300.0),
                entry(MoneyAccount.BANK, -100.0)
            ),
            pendingPfand = 0.0
        )

        assertEquals(37.75, balances.cash, 0.0001)
        assertEquals(200.0, balances.bank, 0.0001)
        assertEquals(37.75, balances.balanceOf(MoneyAccount.CASH), 0.0001)
        assertEquals(200.0, balances.balanceOf(MoneyAccount.BANK), 0.0001)
    }

    @Test
    fun `pfand beklemedeki tutar toplama otomatik eklenir`() {
        val balances = MoneySummary.of(
            listOf(
                entry(MoneyAccount.CASH, 20.0),
                entry(MoneyAccount.BANK, 80.0)
            ),
            pendingPfand = 4.50
        )

        assertEquals(4.50, balances.pfand, 0.0001)
        assertEquals(104.50, balances.total, 0.0001)
    }

    @Test
    fun `pfand tutari yoksa toplam nakit ve bankadir`() {
        val balances = MoneySummary.of(listOf(entry(MoneyAccount.BANK, 15.0)), pendingPfand = 0.0)

        assertEquals(15.0, balances.total, 0.0001)
    }

    @Test
    fun `negatif pfand degeri toplama yazilmaz`() {
        val balances = MoneySummary.of(emptyList(), pendingPfand = -3.0)

        assertEquals(0.0, balances.pfand, 0.0001)
        assertEquals(0.0, balances.total, 0.0001)
    }

    @Test
    fun `bakiye eksiltmede negatife dusebilir`() {
        val balances = MoneySummary.of(
            listOf(
                entry(MoneyAccount.CASH, 10.0),
                entry(MoneyAccount.CASH, -25.0)
            ),
            pendingPfand = 0.0
        )

        assertEquals(-15.0, balances.cash, 0.0001)
    }

    @Test
    fun `islem onizlemesi mevcut bakiyeyi degistirmez`() {
        assertEquals(35.0, MoneySummary.preview(20.0, 15.0), 0.0001)
    }

    @Test
    fun `tutar ayristirici virgul ve nokta kabul eder`() {
        assertEquals(12.5, MoneyInput.parse("12,50")!!, 0.0001)
        assertEquals(12.5, MoneyInput.parse("12.50")!!, 0.0001)
        assertEquals(7.0, MoneyInput.parse(" 7 € ")!!, 0.0001)
    }

    @Test
    fun `tutar ayristirici gecersiz ve sifir degerleri reddeder`() {
        assertNull(MoneyInput.parse(""))
        assertNull(MoneyInput.parse("abc"))
        assertNull(MoneyInput.parse("0"))
        assertNull(MoneyInput.parse("-5"))
    }

    @Test
    fun `tutar iki ondaliga yuvarlanir`() {
        assertEquals(3.33, MoneyInput.parse("3,3349")!!, 0.0001)
    }

    @Test
    fun `isaretli euro bicimi yon gosterir`() {
        assertEquals("+€12,50", signedEuro(12.5))
        assertEquals("−€12,50", signedEuro(-12.5))
        assertEquals("€0,00", signedEuro(0.0))
    }
}
