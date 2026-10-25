package com.shoppingapp.feature.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {

    private val sample = """
        REWE Markt München
        FILIALE 1234   KASSE 3
        MILSANI JOGHURT 3,5%        1,09 A
        2 x JA! H-MILCH 1,5%        1,98 A
        BANANEN 1,5 KG              2,49 B
        SUMME EUR                   5,56
        GEGEBEN BAR                 10,00
        RÜCKGELD                    4,44
    """.trimIndent()

    @Test
    fun `urun satirlari ve toplam okunur`() {
        val receipt = ReceiptParser.parse(sample)
        assertEquals(3, receipt.lines.size)
        assertEquals("MILSANI JOGHURT 3,5%", receipt.lines[0].name)
        assertEquals(1.09, receipt.lines[0].unitPrice, 0.001)
        assertEquals(2.0, receipt.lines[1].quantity, 0.001)
        // "2 x ... 1,98" satırında 1,98 iki adedin toplamıdır.
        assertEquals(0.99, receipt.lines[1].unitPrice, 0.001)
        assertEquals(5.56, receipt.lines.sumOf { it.lineTotal }, 0.01)
        assertEquals("BANANEN 1,5 KG", receipt.lines[2].name)
        assertEquals(5.56, receipt.total ?: 0.0, 0.001)
    }

    @Test
    fun `odeme ve kdv satirlari urun sayilmaz`() {
        val receipt = ReceiptParser.parse(sample)
        assertTrue(receipt.lines.none { it.name.contains("SUMME", true) })
        assertTrue(receipt.lines.none { it.name.contains("RÜCKGELD", true) })
        assertTrue(receipt.lines.none { it.name.contains("MWST", true) })
    }

    @Test
    fun `pfand satiri ayri urun olarak eklenmez`() {
        val receipt = ReceiptParser.parse("PFAND 0,25 A\nWASSER 0,99 A")
        assertEquals(listOf("WASSER"), receipt.lines.map { it.name })
    }

    @Test
    fun `tutari olmayan basliklar atlanir`() {
        val receipt = ReceiptParser.parse("REWE Markt\nKASSE 3\nBrot 1,99")
        assertEquals(1, receipt.lines.size)
        assertNull(receipt.total)
    }

    @Test
    fun `ayni urun iki kez yazilmaz`() {
        val receipt = ReceiptParser.parse("Brot 1,99\nbrot 2,49")
        assertEquals(1, receipt.lines.size)
    }

    @Test
    fun `bos metin`() {
        val receipt = ReceiptParser.parse("   ")
        assertTrue(receipt.lines.isEmpty())
        assertNull(receipt.total)
    }
}
