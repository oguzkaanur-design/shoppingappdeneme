package com.shoppingapp.feature.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptTextLayoutTest {

    private fun line(text: String, left: Int, top: Int, right: Int, bottom: Int) =
        OcrLine(text = text, left = left, top = top, right = right, bottom = bottom)

    @Test
    fun `ayni yukseklikteki ad ve tutar tek satirda birlesir`() {
        val rows = ReceiptTextLayout.rows(
            listOf(
                line("MILSANI JOGHURT 3,5%", 40, 100, 500, 130),
                line("1,09 A", 620, 102, 720, 132),
                line("BANANEN 1,5 KG", 40, 160, 470, 190),
                line("2,49 B", 620, 161, 720, 191)
            )
        )
        assertEquals(
            listOf("MILSANI JOGHURT 3,5%  1,09 A", "BANANEN 1,5 KG  2,49 B"),
            rows
        )
    }

    @Test
    fun `farkli satirlar ayrilir`() {
        val rows = ReceiptTextLayout.rows(
            listOf(
                line("SUMME EUR", 40, 100, 300, 130),
                line("6,55", 620, 230, 700, 260)
            )
        )
        assertEquals(listOf("SUMME EUR", "6,55"), rows)
    }

    @Test
    fun `bos metin`() {
        assertEquals(emptyList<String>(), ReceiptTextLayout.rows(emptyList()))
    }
}
