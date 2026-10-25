package com.shoppingapp.security

import com.google.gson.JsonParser
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Çevrimdışı premium lisansı.
 *
 * Lisans, satıcının çevrimdışı tuttuğu EC P-256 özel anahtarıyla imzalanır. Uygulamada
 * yalnızca açık anahtar bulunur. APK klonlansa, R8 kaldırılsa veya SharedPreferences
 * kurcalansa bile geçerli bir imza üretilemediği için premium açılamaz.
 *
 * Biçim: `BASKIT1.<base64url(payload)>.<base64url(ECDSA-SHA256 imzası)>`
 * Yük (JSON): `{"v":1,"sub":"...","plan":"pro","iat":<ms>,"exp":<ms>}`
 *
 * Sınır: kararlı bir saldırgan imza kontrolünü yamalayabilir. Uygulama içi her kapı
 * gibi bu da mutlak değildir; ücretli abonelik Play Billing + sunucu doğrulamasıyla
 * verilmelidir. Bu yapı, sunucu olmadan kopya APK'ların lisans çalmasını engeller.
 */
object PremiumLicense {

    private const val PREFIX = "BASKIT1"
    private const val MAX_TOKEN_CHARS = 4096

    /**
     * `license/baskit-license-public.b64` içeriği (X.509 SubjectPublicKeyInfo, DER,
     * base64). Özel anahtar depoya girmez; `license/` klasöründe çevrimdışı saklanır.
     */
    const val PUBLIC_KEY_B64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7XAIBTZW4oPrA8nOsrW2hyAAK0dI60n3avQJQseIBbVGpYy8uen5kc/s+M0EFfdKee3wsXltCupkOt09kXYukg=="

    data class License(
        val subject: String,
        val plan: String,
        val issuedAtMs: Long,
        val expiresAtMs: Long
    ) {
        fun isActive(nowMs: Long): Boolean = nowMs < expiresAtMs
    }

    sealed interface Result {
        data class Valid(val license: License) : Result
        data class Invalid(val reason: String) : Result
    }

    /** Üretimde açık anahtar; testler kendi anahtar çiftini geçebilir. */
    fun verify(
        token: String,
        publicKeyB64: String = PUBLIC_KEY_B64,
        nowMs: Long = System.currentTimeMillis()
    ): Result {
        if (token.length > MAX_TOKEN_CHARS) return Result.Invalid("token_too_large")
        val parts = token.trim().split('.')
        if (parts.size != 3 || parts[0] != PREFIX) return Result.Invalid("malformed")

        val payload = decodeUrl(parts[1]) ?: return Result.Invalid("payload")
        val signature = decodeUrl(parts[2]) ?: return Result.Invalid("signature")
        if (!signatureMatches(payload, signature, publicKeyB64)) return Result.Invalid("signature")

        val json = runCatching {
            JsonParser.parseString(String(payload, Charsets.UTF_8)).asJsonObject
        }.getOrNull() ?: return Result.Invalid("json")

        val version = runCatching { json.get("v").asInt }.getOrNull()
        if (version != 1) return Result.Invalid("version")
        val subject = runCatching { json.get("sub").asString }.getOrNull()
            ?.trim().orEmpty()
        val plan = runCatching { json.get("plan").asString }.getOrNull()
            ?.trim().orEmpty()
        val issuedAt = runCatching { json.get("iat").asLong }.getOrNull()
        val expiresAt = runCatching { json.get("exp").asLong }.getOrNull()
        if (subject.isEmpty() || subject.length > 190) return Result.Invalid("subject")
        if (plan.isEmpty() || plan.length > 32) return Result.Invalid("plan")
        if (issuedAt == null || expiresAt == null) return Result.Invalid("dates")
        if (expiresAt <= issuedAt) return Result.Invalid("dates")

        val license = License(subject, plan, issuedAt, expiresAt)
        if (!license.isActive(nowMs)) return Result.Invalid("expired")
        return Result.Valid(license)
    }

    private fun signatureMatches(payload: ByteArray, signature: ByteArray, publicKeyB64: String): Boolean =
        runCatching {
            val padded = publicKeyB64.trim().let {
                if (it.length % 4 == 0) it else it + "=".repeat(4 - it.length % 4)
            }
            val keyBytes = Base64.getDecoder().decode(padded)
            val publicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(keyBytes))
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(payload)
                verify(signature)
            }
        }.getOrDefault(false)

    private fun decodeUrl(value: String): ByteArray? =
        runCatching { Base64.getUrlDecoder().decode(value) }.getOrNull()
}
