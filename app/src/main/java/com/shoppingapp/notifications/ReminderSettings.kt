package com.shoppingapp.notifications

import android.content.Context
import java.time.LocalDate

/**
 * Hatırlatıcı tercihleri ve "bugün bildirildi" işaretleri. Ayrı bir veritabanı
 * tablosu gerektirmemesi için SharedPreferences'ta tutulur.
 */
object ReminderSettings {

    private const val PREFS = "reminder_settings"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Aynı hatırlatıcı günde bir kez gösterilir; iş günde birkaç kez çalışsa bile
     * kullanıcı aynı bildirimi tekrar görmez.
     */
    fun shouldNotifyToday(context: Context, key: String, today: LocalDate = LocalDate.now()): Boolean {
        val last = prefs(context).getString("last_$key", null)
        return last != today.toString()
    }

    fun markNotifiedToday(context: Context, key: String, today: LocalDate = LocalDate.now()) {
        prefs(context).edit().putString("last_$key", today.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
