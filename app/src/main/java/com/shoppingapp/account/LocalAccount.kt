package com.shoppingapp.account

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Yerel hesap işlemlerinin hata kodları; arayüz bunları kendi diline çevirir. */
enum class LocalAccountError {
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    EMAIL_ALREADY_EXISTS,
    NOT_FOUND,
    WRONG_PASSWORD
}

/**
 * Cihazda kalan hesap: e-posta + şifre. Sunucu yok; şifrenin kendisi hiçbir
 * yerde saklanmaz, yalnızca PBKDF2-SHA256 ile türetilmiş özet tutulur.
 *
 * Amaç hesabı cihazda ayırmak ve cihaz değişiminde veriyi Google hesabıyla
 * taşıyabilmek; bu yüzden doğrulama tamamen yereldir.
 */
object LocalAccount {

    /** PBKDF2 tur sayısı: telefonda ~100 ms sürer, kaba kuvveti pahalı kılar. */
    const val ITERATIONS = 120_000
    const val MIN_PASSWORD_LENGTH = 8
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())

    fun create(
        context: Context,
        email: String,
        password: String,
        repeatedPassword: String
    ): Result<AccountState> {
        val normalized = email.trim().lowercase()
        if (!isValidEmail(normalized)) return Result.failure(LocalAccountException(LocalAccountError.EMAIL_INVALID))
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(LocalAccountException(LocalAccountError.PASSWORD_TOO_SHORT))
        }
        if (password != repeatedPassword) {
            return Result.failure(LocalAccountException(LocalAccountError.PASSWORD_MISMATCH))
        }
        val existing = LocalCredentials.email(context)
        if (existing != null && existing != normalized) {
            return Result.failure(LocalAccountException(LocalAccountError.EMAIL_ALREADY_EXISTS))
        }

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt, ITERATIONS)
        LocalCredentials.save(
            context = context,
            email = normalized,
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            hash = Base64.encodeToString(hash, Base64.NO_WRAP)
        )
        AccountStore.signIn(context, AccountType.LOCAL, normalized)
        return Result.success(AccountState(AccountType.LOCAL, normalized))
    }

    fun signIn(context: Context, email: String, password: String): Result<AccountState> {
        val normalized = email.trim().lowercase()
        val storedEmail = LocalCredentials.email(context)
            ?: return Result.failure(LocalAccountException(LocalAccountError.NOT_FOUND))
        val salt = LocalCredentials.salt(context)
        val expected = LocalCredentials.hash(context)
        if (salt == null || expected == null) {
            return Result.failure(LocalAccountException(LocalAccountError.NOT_FOUND))
        }
        if (storedEmail != normalized) {
            return Result.failure(LocalAccountException(LocalAccountError.WRONG_PASSWORD))
        }
        val actual = Base64.encodeToString(
            derive(password, Base64.decode(salt, Base64.NO_WRAP), ITERATIONS),
            Base64.NO_WRAP
        )
        if (!MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())) {
            return Result.failure(LocalAccountException(LocalAccountError.WRONG_PASSWORD))
        }
        AccountStore.signIn(context, AccountType.LOCAL, normalized)
        return Result.success(AccountState(AccountType.LOCAL, normalized))
    }

    fun exists(context: Context): Boolean = LocalCredentials.email(context) != null

    fun signOutAndForget(context: Context) {
        LocalCredentials.clear(context)
        AccountStore.signOut(context)
    }

    /** Testlerde deterministik sonuç için tur sayısı parametre. */
    internal fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

class LocalAccountException(val error: LocalAccountError) : Exception(error.name)
