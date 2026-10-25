package com.shoppingapp.onboarding

import android.content.Context
import com.shoppingapp.money.CurrencyStore

/**
 * İlk açılış akışının durumu: dil → ülke/para birimi → hesap → tanıtım turu.
 * Akış bir kez bitirilir; sonradan ayarlardan dil, para birimi ve hesap
 * değiştirilebilir.
 */
object OnboardingStore {
    private const val PREFS = "korb_onboarding"
    private const val KEY_COMPLETED = "completed"
    private const val KEY_COUNTRY = "country"

    fun isCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun country(context: Context): Country? =
        CountryCatalog.of(prefs(context).getString(KEY_COUNTRY, null))

    /** Ülke seçimi para birimini de belirler; ikisi birlikte saklanır. */
    fun setCountry(context: Context, country: Country) {
        prefs(context).edit().putString(KEY_COUNTRY, country.code).apply()
        CurrencyStore.set(context, country.currency)
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
