package com.shoppingapp.feature.language

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Uygulama dili seçimini saklar ve uygular.
 *
 * API 33+ dil, sistemin "uygulama dili" ayarına da yazılır. Daha eski
 * sürümlerde Activity/Application bağlamı seçilen locale ile sarılır.
 */
object LanguageStore {
    private const val PREFS = "einkauf_language"
    private const val KEY_TAG = "tag"

    fun storedTag(context: Context): String? =
        prefs(context).getString(KEY_TAG, null)

    /** Sistem ayarı varsa onu, yoksa uygulama içi seçimi döndürür. */
    fun currentTag(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) return locales[0]?.toLanguageTag()
        }
        return storedTag(context)
    }

    fun setTag(activity: Activity, tag: String?) {
        prefs(activity).edit().apply {
            if (tag == null) remove(KEY_TAG) else putString(KEY_TAG, tag)
        }.apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = activity.getSystemService(LocaleManager::class.java)
            manager.applicationLocales = tag
                ?.let { LocaleList.forLanguageTags(it) }
                ?: LocaleList.getEmptyLocaleList()
            // Dil değişikliği yapılandırma değişimi üretir; Activity sistem
            // tarafından yeniden oluşturulur. Eski sürümlerde bunu biz yaparız.
        } else {
            activity.recreate()
        }
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = storedTag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
