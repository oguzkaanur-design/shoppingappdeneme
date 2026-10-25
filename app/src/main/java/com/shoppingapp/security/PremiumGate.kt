package com.shoppingapp.security

import android.content.Context
import androidx.core.content.edit

/**
 * Premium yetkisinin tek karar noktası.
 *
 * Kural: premium yalnızca (1) uygulama yayın imzasıyla çalışıyorsa, (2) hata ayıklama
 * / emülatör / hook ortamında değilse ve (3) geçerli, süresi dolmamış bir imzalı lisans
 * varsa açılır. Yerelde `true` dönen bir bayrak yoktur; lisans olmadan hiçbir özellik
 * premium sayılmaz.
 *
 * Yeni premium özellik eklerken `PremiumGate.isPremium(context)` kontrolünü kullanın
 * ve özelliği hem UI'da hem iş katmanında kapatın. Ödeme eklendiğinde lisansı Play
 * Billing + sunucu doğrulamasıyla üretin; bu sınıf yalnızca istemci tarafındaki kapıdır.
 */
object PremiumGate {

    private const val PREFS = "baskit_premium"
    private const val KEY_TOKEN = "license_token"

    enum class State { ACTIVE, LOCKED, TAMPERED, EXPIRED }

    fun state(context: Context): State {
        if (!AppIntegrity.isTrusted(context) || TamperSignals.isCompromised(context)) {
            return State.TAMPERED
        }
        val token = prefs(context).getString(KEY_TOKEN, null) ?: return State.LOCKED
        return when (val result = PremiumLicense.verify(token)) {
            is PremiumLicense.Result.Valid -> State.ACTIVE
            is PremiumLicense.Result.Invalid ->
                if (result.reason == "expired") State.EXPIRED else State.LOCKED
        }
    }

    fun isPremium(context: Context): Boolean = state(context) == State.ACTIVE

    /** İmza geçerliyse saklar; geçersiz token hiçbir zaman yazılmaz. */
    fun activate(context: Context, token: String): Boolean {
        val valid = PremiumLicense.verify(token) is PremiumLicense.Result.Valid
        if (valid) prefs(context).edit { putString(KEY_TOKEN, token.trim()) }
        return valid
    }

    fun deactivate(context: Context) {
        prefs(context).edit { remove(KEY_TOKEN) }
    }

    fun activeLicense(context: Context): PremiumLicense.License? {
        if (state(context) != State.ACTIVE) return null
        val token = prefs(context).getString(KEY_TOKEN, null) ?: return null
        return (PremiumLicense.verify(token) as? PremiumLicense.Result.Valid)?.license
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
