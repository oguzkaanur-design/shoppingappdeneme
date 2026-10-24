package com.shoppingapp.feature.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListImportTest {

    @Test
    fun `her satir bir urun olur`() {
        val items = ListImport.parse("Süt\nEkmek\nDomates")
        assertEquals(listOf("Süt", "Ekmek", "Domates"), items.map { it.name })
        assertTrue(items.all { it.quantity == 1.0 && it.unit == "Adet" })
    }

    @Test
    fun `madde isaretleri ve adet onegi okunur`() {
        val items = ListImport.parse("• 2 kg un\n- 3x Milch\n1,5 l Su\n2 adet yumurta")
        assertEquals(listOf("un", "Milch", "Su", "yumurta"), items.map { it.name })
        assertEquals(listOf(2.0, 3.0, 1.5, 2.0), items.map { it.quantity })
        assertEquals(listOf("kg", "Adet", "l", "Adet"), items.map { it.unit })
    }

    @Test
    fun `virgulle ayrilmis tek satir bolunur ama ondalik korunur`() {
        val items = ListImport.parse("süt, ekmek, 2 kg un, 1,5 l süt")
        assertEquals(listOf("süt", "ekmek", "un"), items.map { it.name })
        // Aynı ad iki kez geçerse birimli olan kazanır.
        val sut = items.first { it.name == "süt" }
        assertEquals(1.5, sut.quantity, 0.001)
        assertEquals("l", sut.unit)
    }

    @Test
    fun `baslik ve baglanti satirlari atlanir`() {
        val items = ListImport.parse("Einkaufsliste\nhttps://example.com\nSüt")
        assertEquals(listOf("Süt"), items.map { it.name })
    }

    @Test
    fun `bosluk ve gecersiz satirlar yok sayilir`() {
        assertTrue(ListImport.parse("   \n\n12345\n!").isEmpty())
    }

    @Test
    fun `buyuk kucuk harf farkiyla ayni urun tekrarlanmaz`() {
        val items = ListImport.parse("Süt\nSÜT")
        assertEquals(1, items.size)
        assertEquals(2.0, items.first().quantity, 0.001)
    }
}
