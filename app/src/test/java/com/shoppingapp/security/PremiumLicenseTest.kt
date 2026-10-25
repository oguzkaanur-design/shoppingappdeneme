package com.shoppingapp.security

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lisans doğrulamasının güvenlik sözleşmesi: imzası üretilemeyen, süresi geçmiş
 * veya gövdesi değiştirilmiş hiçbir token kabul edilmez.
 */
class PremiumLicenseTest {

    private val keyPair: KeyPair = run {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        generator.generateKeyPair()
    }

    private val publicKeyB64: String =
        Base64.getEncoder().encodeToString(keyPair.public.encoded)

    private fun sign(payload: String, pair: KeyPair = keyPair): String {
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(pair.private)
            update(payload.toByteArray(Charsets.UTF_8))
            sign()
        }
        return "BASKIT1.${url(payload.toByteArray())}.${url(signature)}"
    }

    private fun url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun payload(
        version: Int = 1,
        subject: String = "ada@example.com",
        plan: String = "pro",
        issuedAt: Long = 1_000L,
        expiresAt: Long = 2_000L
    ) = """{"v":$version,"sub":"$subject","plan":"$plan","iat":$issuedAt,"exp":$expiresAt}"""

    @Test
    fun `valid token is accepted`() {
        val result = PremiumLicense.verify(sign(payload()), publicKeyB64, nowMs = 1_500L)
        val license = (result as PremiumLicense.Result.Valid).license
        assertEquals("ada@example.com", license.subject)
        assertEquals("pro", license.plan)
        assertEquals(2_000L, license.expiresAtMs)
    }

    @Test
    fun `expired token is rejected`() {
        val result = PremiumLicense.verify(sign(payload()), publicKeyB64, nowMs = 2_000L)
        assertEquals(PremiumLicense.Result.Invalid("expired"), result)
    }

    @Test
    fun `modified payload breaks the signature`() {
        val parts = sign(payload()).split('.')
        val forged = """{"v":1,"sub":"hacker@example.com","plan":"pro","iat":1000,"exp":999999999}"""
        val token = "${parts[0]}.${url(forged.toByteArray())}.${parts[2]}"
        assertEquals(PremiumLicense.Result.Invalid("signature"),
            PremiumLicense.verify(token, publicKeyB64, nowMs = 1_500L))
    }

    @Test
    fun `token signed by another key is rejected`() {
        val other = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
        val token = sign(payload(), other)
        assertEquals(PremiumLicense.Result.Invalid("signature"),
            PremiumLicense.verify(token, publicKeyB64, nowMs = 1_500L))
    }

    @Test
    fun `malformed tokens are rejected`() {
        assertEquals(PremiumLicense.Result.Invalid("malformed"),
            PremiumLicense.verify("BASKIT1.onlytwo", publicKeyB64))
        assertEquals(PremiumLicense.Result.Invalid("malformed"),
            PremiumLicense.verify("OTHER1.a.b", publicKeyB64))
        assertEquals(PremiumLicense.Result.Invalid("malformed"),
            PremiumLicense.verify("", publicKeyB64))
    }

    @Test
    fun `oversized token is rejected before parsing`() {
        val token = "BASKIT1." + "A".repeat(5_000)
        assertEquals(PremiumLicense.Result.Invalid("token_too_large"),
            PremiumLicense.verify(token, publicKeyB64))
    }

    @Test
    fun `wrong schema version is rejected`() {
        val token = sign(payload(version = 2))
        assertEquals(PremiumLicense.Result.Invalid("version"),
            PremiumLicense.verify(token, publicKeyB64, nowMs = 1_500L))
    }

    @Test
    fun `invalid validity window is rejected`() {
        val token = sign(payload(issuedAt = 5_000L, expiresAt = 5_000L))
        assertEquals(PremiumLicense.Result.Invalid("dates"),
            PremiumLicense.verify(token, publicKeyB64, nowMs = 5_000L))
    }

    /** Uygulamaya gömülü açık anahtar geçerli bir EC P-256 anahtarı olmalı. */
    @Test
    fun `embedded public key is a valid EC key`() {
        val padded = PremiumLicense.PUBLIC_KEY_B64.let {
            if (it.length % 4 == 0) it else it + "=".repeat(4 - it.length % 4)
        }
        val key = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(padded)))
        assertEquals("EC", key.algorithm)
        assertTrue(PremiumLicense.PUBLIC_KEY_B64.isNotBlank())
    }

    /** Üretim anahtarıyla imzalanmamış bir token gerçek doğrulamada geçmemeli. */
    @Test
    fun `test key does not validate against the embedded production key`() {
        val token = sign(payload())
        assertTrue(PremiumLicense.verify(token, nowMs = 1_500L) is PremiumLicense.Result.Invalid)
    }
}
