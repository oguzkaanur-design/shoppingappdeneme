package com.shoppingapp.security

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Yerel uygulama kilidi. PIN asla düz metin saklanmaz; rastgele tuzlu
 * PBKDF2 özeti private SharedPreferences içinde tutulur.
 */
object AppLock {
    const val PIN_LENGTH = 4

    private const val PREFS = "einkauf_app_lock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"
    private const val KEY_BIOMETRIC = "biometric"
    private const val KEY_FAILED = "failed"
    private const val KEY_LOCKED_UNTIL = "locked_until"

    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_MS = 30_000L
    private const val ITERATIONS = 150_000

    data class VerifyResult(
        val success: Boolean,
        val lockoutSeconds: Int = 0
    )

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false) && hasPin(context)

    fun isBiometricEnabled(context: Context): Boolean =
        isEnabled(context) && prefs(context).getBoolean(KEY_BIOMETRIC, false)

    fun hasPin(context: Context): Boolean =
        prefs(context).contains(KEY_HASH) && prefs(context).contains(KEY_SALT)

    fun setPin(context: Context, pin: String) {
        require(pin.length == PIN_LENGTH && pin.all(Char::isDigit)) {
            "PIN must be $PIN_LENGTH digits"
        }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_FAILED, 0)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
    }

    fun verify(context: Context, pin: String): VerifyResult {
        val prefs = prefs(context)
        val until = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        val now = SystemClock.elapsedRealtime()
        if (until > now) {
            return VerifyResult(false, ((until - now + 999) / 1000).toInt())
        }
        if (until in 1L..now) {
            prefs.edit().remove(KEY_LOCKED_UNTIL).apply()
        }

        val salt = prefs.getString(KEY_SALT, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        }
        val expected = prefs.getString(KEY_HASH, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        }
        if (salt == null || expected == null) return VerifyResult(false)

        val matches = pin.length == PIN_LENGTH &&
            MessageDigest.isEqual(hash(pin, salt), expected)
        if (matches) {
            prefs.edit().putInt(KEY_FAILED, 0).remove(KEY_LOCKED_UNTIL).apply()
            return VerifyResult(true)
        }

        val failed = prefs.getInt(KEY_FAILED, 0) + 1
        if (failed >= MAX_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_FAILED, 0)
                .putLong(KEY_LOCKED_UNTIL, now + LOCKOUT_MS)
                .apply()
            return VerifyResult(false, (LOCKOUT_MS / 1000).toInt())
        }
        prefs.edit().putInt(KEY_FAILED, failed).apply()
        return VerifyResult(false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled && isEnabled(context)).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun canUseBiometric(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
