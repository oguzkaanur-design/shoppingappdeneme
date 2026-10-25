package com.shoppingapp.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hata ayıklama derlemesi hiçbir zaman premium sayılmamalı: testler debug APK
 * üzerinde çalıştığı için burada beklenen durum TAMPERED'dır. Bu garanti,
 * yayın dışı bir kurulumun ücretli özellikleri bedava açmasını engeller.
 */
@RunWith(AndroidJUnit4::class)
class PremiumGateTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun debugBuildIsNeverPremium() {
        assertFalse(AppIntegrity.isTrusted(context))
        assertEquals(PremiumGate.State.TAMPERED, PremiumGate.state(context))
        assertFalse(PremiumGate.isPremium(context))
    }

    @Test
    fun activationRejectsForgedToken() {
        assertFalse(PremiumGate.activate(context, "BASKIT1.e30.e30"))
        assertEquals(PremiumGate.State.TAMPERED, PremiumGate.state(context))
        assertNull(PremiumGate.activeLicense(context))
    }
}
