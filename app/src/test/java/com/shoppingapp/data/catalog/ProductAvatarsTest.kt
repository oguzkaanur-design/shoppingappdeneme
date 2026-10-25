package com.shoppingapp.data.catalog

import com.shoppingapp.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductAvatarsTest {

    @Test
    fun commonGroceriesGetTheirOwnSymbol() {
        assertEquals("🥛", ProductAvatars.emojiFor("Vollmilch 3,5% Fett"))
        assertEquals("🧀", ProductAvatars.emojiFor("Gouda Scheiben 400 g"))
        assertEquals("🧀", ProductAvatars.emojiFor("Kaşar peyniri"))
        assertEquals("🥚", ProductAvatars.emojiFor("Eier aus Freilandhaltung 10 Stück"))
        assertEquals("🍞", ProductAvatars.emojiFor("Vollkornbrot 500 g"))
        assertEquals("🍅", ProductAvatars.emojiFor("Domates 1 kg"))
        assertEquals("🍎", ProductAvatars.emojiFor("Elma"))
        assertEquals("🍌", ProductAvatars.emojiFor("Muz"))
        assertEquals("🍝", ProductAvatars.emojiFor("ja! Spaghetti 500 g"))
        assertEquals("🍫", ProductAvatars.emojiFor("Çikolata bitter"))
        assertEquals("🧽", ProductAvatars.emojiFor("Spülmittel 500 ml"))
        assertEquals("🧻", ProductAvatars.emojiFor("Toilettenpapier 10 Rollen"))
        assertEquals("🔧", ProductAvatars.emojiFor("Tornavida seti"))
        assertEquals("🔋", ProductAvatars.emojiFor("Batterien AA 4 Stück"))
        assertEquals("🐾", ProductAvatars.emojiFor("Katzenfutter 400 g"))
    }

    @Test
    fun turkishAndGermanSpellingsShareOneAvatar() {
        assertEquals(
            ProductAvatars.detect("Süt")?.key,
            ProductAvatars.detect("Milch")?.key
        )
        assertEquals(
            ProductAvatars.detect("Yumurta")?.key,
            ProductAvatars.detect("Eier")?.key
        )
        assertEquals(
            ProductAvatars.detect("Bulaşık deterjanı")?.key,
            ProductAvatars.detect("Spülmittel")?.key
        )
        assertEquals(
            ProductAvatars.detect("ÜZÜM")?.key,
            ProductAvatars.detect("Weintrauben")?.key
        )
    }

    @Test
    fun shortWordsDoNotMatchInsideOtherWords() {
        // "ei" (yumurta) "Reiniger" içinde geçiyor diye yumurta simgesi çıkmamalı.
        assertEquals("🧽", ProductAvatars.emojiFor("Reiniger Zitrone"))
        assertEquals("🍦", ProductAvatars.emojiFor("Eis am Stiel"))
        assertEquals("🍚", ProductAvatars.emojiFor("Reis 1 kg"))
    }

    @Test
    fun twoWordPhrasesBeatSingleWords() {
        assertEquals("🧃", ProductAvatars.emojiFor("Elma suyu 1 l"))
        assertEquals("💧", ProductAvatars.emojiFor("Maden suyu 6×0,5 l"))
        assertEquals("🍼", ProductAvatars.emojiFor("Bebek bezi 4 numara"))
        assertEquals("🐾", ProductAvatars.emojiFor("Kedi maması 2 kg"))
    }

    @Test
    fun turkishSuffixesStillMatch() {
        assertEquals("🍞", ProductAvatars.emojiFor("Tam buğday ekmeği"))
        assertEquals("🥛", ProductAvatars.emojiFor("Sütü kaynat"))
        assertEquals("🥚", ProductAvatars.emojiFor("Yumurtalar 10'lu"))
        assertEquals("🧀", ProductAvatars.emojiFor("Peynirler"))
    }

    @Test
    fun userChoiceOverridesDetection() {
        val product = Product(
            name = "Domates",
            barcode = "CUSTOM_1",
            categoryId = 1L,
            unit = "Adet",
            avatarKey = "cheese"
        )
        assertEquals("🧀", ProductAvatars.emojiFor(product))

        // "Otomatik"e dönüldüğünde (anahtar null) simge yine addan gelir.
        assertEquals("🍅", ProductAvatars.emojiFor(product.copy(avatarKey = null)))
    }

    @Test
    fun unknownNamesFallBackToCategoryThenCart() {
        val unknown = Product(name = "XYZ unknown", barcode = "CUSTOM_2", categoryId = 0L, unit = "Adet")
        assertEquals(ProductAvatars.FALLBACK_EMOJI, ProductAvatars.emojiFor(unknown))
        assertEquals("🥤", ProductAvatars.emojiFor(unknown.copy(categoryId = 3L)))
        assertNull(ProductAvatars.detect("XYZ unknown"))
    }

    @Test
    fun detectionAlsoFeedsTheCategory() {
        assertEquals(1L, ProductAvatars.categoryFor("Domates"))
        assertEquals(2L, ProductAvatars.categoryFor("Peynir"))
        assertEquals(3L, ProductAvatars.categoryFor("Mineralwasser"))
        // Makarna meyve-sebze değil, atıştırmalık/temel gıda sayılır.
        assertEquals(4L, ProductAvatars.categoryFor("Makarna"))
        assertEquals(5L, ProductAvatars.categoryFor("Deterjan"))
        assertNull(ProductAvatars.categoryFor("???"))
    }

    @Test
    fun everyAvatarIsSelectableAndWellFormed() {
        val keys = ProductAvatars.all.map { it.key }
        assertEquals("Avatar anahtarları benzersiz olmalı", keys.size, keys.toSet().size)
        ProductAvatars.all.forEach { avatar ->
            assertTrue("Simge boş: ${avatar.key}", avatar.emoji.isNotBlank())
            assertTrue("Ad boş: ${avatar.key}", avatar.name.isNotBlank())
            assertTrue("Kategori tanımsız: ${avatar.key}", avatar.categoryId in 1L..5L)
            assertTrue("Anahtar boş: ${avatar.key}", avatar.keywords.isNotEmpty())
            assertNotNull(
                "Anahtarla geri bulunamıyor: ${avatar.key}",
                ProductAvatars.byKey(avatar.key)
            )
        }
    }
}
