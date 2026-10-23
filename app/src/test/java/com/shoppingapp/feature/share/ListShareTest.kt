package com.shoppingapp.feature.share

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListShareTest {

    @Test
    fun `liste markete gore gruplanir`() {
        val text = ListShare.text(
            listOf(
                ShareLine("Süt", 2.0, "L", "Rewe"),
                ShareLine("Ekmek", 1.0, "Adet", "Rewe"),
                ShareLine("Elma", 1.5, "kg", "Aldi Süd")
            ),
            title = "Alışveriş listesi"
        )

        assertTrue(text.startsWith("Alışveriş listesi"))
        assertTrue(text.contains("3 ürün"))
        assertTrue(text.contains("Rewe:"))
        assertTrue(text.contains("Aldi Süd:"))
        assertTrue(text.contains("• 2 L Süt"))
        assertTrue(text.contains("• 1 × Ekmek"))
        assertTrue(text.contains("• 1,5 kg Elma"))
    }

    @Test
    fun `alinanlar varsayilan olarak paylasilmaz`() {
        val lines = listOf(
            ShareLine("Süt", 1.0, "L", "Rewe"),
            ShareLine("Ekmek", 1.0, "Adet", "Rewe", purchased = true)
        )

        val text = ListShare.text(lines)
        assertTrue(text.contains("1 ürün"))
        assertTrue(text.contains("Süt"))
        assertFalse(text.contains("Ekmek"))
    }

    @Test
    fun `alinanlar istenirse eklenir`() {
        val text = ListShare.text(
            listOf(ShareLine("Ekmek", 1.0, "Adet", purchased = true)),
            includePurchased = true
        )
        assertTrue(text.contains("Ekmek"))
    }

    @Test
    fun `bos liste anlasilir metin dondurur`() {
        val text = ListShare.text(emptyList())
        assertTrue(text.contains("Liste boş."))
    }

    @Test
    fun `market yoksa ayri baslik`() {
        val text = ListShare.text(listOf(ShareLine("Süt", 1.0, "L", storeName = null)))
        assertTrue(text.contains("Market yok:"))
    }
}
