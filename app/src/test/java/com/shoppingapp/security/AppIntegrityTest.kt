package com.shoppingapp.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * İmza parmak izi karşılaştırması: Play Console "A5:96:E8:…" biçiminde, keytool ise
 * iki noktasız verir; ikisi de aynı sertifikayı göstermeli.
 */
class AppIntegrityTest {

    @Test
    fun `colon and case differences normalise to the same value`() {
        val keytoolStyle = "a596e8f9c285af6fd1bb4d0b8f49a3619d5e817be772d9d3b2444c8e871a08d2"
        val playConsoleStyle =
            "A5:96:E8:F9:C2:85:AF:6F:D1:BB:4D:0B:8F:49:A3:61:9D:5E:81:7B:E7:72:D9:D3:B2:44:4C:8E:87:1A:08:D2"
        assertEquals(
            AppIntegrity.normalize(keytoolStyle),
            AppIntegrity.normalize(playConsoleStyle)
        )
    }

    @Test
    fun `release certificate is trusted`() {
        assertTrue(AppIntegrity.trustedFingerprints.isNotEmpty())
        assertTrue(
            AppIntegrity.trustedFingerprints.map(AppIntegrity::normalize)
                .contains(AppIntegrity.RELEASE_CERT_SHA256)
        )
    }

    @Test
    fun `unknown certificate is not trusted`() {
        val trusted = AppIntegrity.trustedFingerprints.map(AppIntegrity::normalize)
        assertFalse(trusted.contains(AppIntegrity.normalize("00".repeat(32))))
    }
}
