package com.shoppingapp.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductIllustrationTest {
    @Test fun pastaUsesPastaIllustration() {
        assertEquals("🍝", ProductIllustration.forProductName("ja! Spaghetti 500 g"))
        assertEquals("🍝", ProductIllustration.forProductName("makarna"))
    }

    @Test fun eggsUseEggIllustration() {
        assertEquals("🥚", ProductIllustration.forProductName("Eier aus Freilandhaltung 10 Stück"))
        assertEquals("🥚", ProductIllustration.forProductName("yumurta"))
    }

    @Test fun unknownItemsUseNeutralIllustration() {
        assertEquals("🛒", ProductIllustration.forProductName("XYZ unknown"))
    }
}
