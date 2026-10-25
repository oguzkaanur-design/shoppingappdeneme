package com.shoppingapp.account

import android.content.Context

enum class AccountType {
    /** Hesap yok: veri yalnızca bu cihazda. */
    GUEST,

    /** Google ile giriş yapıldı; senkronizasyon Google Drive uygulama klasöründe. */
    GOOGLE,

    /** Cihazda oluşturulan yerel hesap (e-posta + şifre). */
    LOCAL
}

data class AccountState(
    val type: AccountType = AccountType.GUEST,
    val email: String? = null,
    val displayName: String? = null
) {
    val isSignedIn: Boolean get() = type != AccountType.GUEST
}

/**
 * Oturum durumu. Kimlik doğrulaması [LocalAccount] ve [GoogleSignIn] içinde
 * yapılır; burası yalnızca "kim giriş yaptı" bilgisini saklar. Şifre veya erişim
 * belirteci **asla** burada tutulmaz.
 */
object AccountStore {
    private const val PREFS = "korb_account"
    private const val KEY_TYPE = "type"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"

    fun current(context: Context): AccountState {
        val prefs = prefs(context)
        val type = runCatching {
            AccountType.valueOf(prefs.getString(KEY_TYPE, null) ?: AccountType.GUEST.name)
        }.getOrDefault(AccountType.GUEST)
        return AccountState(
            type = type,
            email = prefs.getString(KEY_EMAIL, null),
            displayName = prefs.getString(KEY_NAME, null)
        )
    }

    fun signIn(context: Context, type: AccountType, email: String?, displayName: String? = null) {
        prefs(context).edit()
            .putString(KEY_TYPE, type.name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, displayName)
            .apply()
    }

    /** Oturumu kapatır; yerel hesabın kaydı [#clearLocalCredentials] ile ayrıca silinir. */
    fun signOut(context: Context) {
        prefs(context).edit()
            .remove(KEY_TYPE)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Yerel hesabın tuz/hash kaydı; oturum bilgisinden ayrı tutulur. */
internal object LocalCredentials {
    private const val PREFS = "korb_local_account"
    private const val KEY_EMAIL = "email"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"

    fun save(context: Context, email: String, salt: String, hash: String) {
        prefs(context).edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash)
            .apply()
    }

    fun email(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    fun salt(context: Context): String? = prefs(context).getString(KEY_SALT, null)

    fun hash(context: Context): String? = prefs(context).getString(KEY_HASH, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
