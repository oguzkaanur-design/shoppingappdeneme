package com.shoppingapp.money

import android.content.Context

/**
 * Seçili para birimi. Ülke seçimi (onboarding) ve ayarlar buraya yazar;
 * gösterim yerleri context taşımadan [current] ile okur.
 */
object CurrencyStore {
    private const val PREFS = "korb_money"
    private const val KEY_CURRENCY = "currency"

    @Volatile
    private var cached: Currency = Currency.EUR

    fun init(context: Context) {
        cached = read(context)
    }

    fun current(): Currency = cached

    fun current(context: Context): Currency = read(context)

    fun set(context: Context, currency: Currency) {
        cached = currency
        prefs(context).edit().putString(KEY_CURRENCY, currency.code).apply()
    }

    /** Alan etiketlerinde (`Fiyat (€)`) kullanılan kısa sembol. */
    fun symbol(): String = cached.symbol

    private fun read(context: Context): Currency =
        Currency.fromCode(prefs(context).getString(KEY_CURRENCY, null)).also { cached = it }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
