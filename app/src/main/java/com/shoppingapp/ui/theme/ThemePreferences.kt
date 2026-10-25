package com.shoppingapp.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.shoppingapp.R

/**
 * Görünüm iki bağımsız eksenden oluşur: renk paleti ve arayüz düzeni.
 * İkisi de cihazda saklanır; seçim anında tüm ekranlara uygulanır.
 */
enum class AppColorTheme(
    val key: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    /** null: sistemin açık/koyu modunu izle. */
    val forcedDark: Boolean?
) {
    SYSTEM("system", R.string.theme_color_system, R.string.theme_color_system_desc, null),
    WHITE("white", R.string.theme_color_white, R.string.theme_color_white_desc, false),
    BLACK("black", R.string.theme_color_black, R.string.theme_color_black_desc, true),
    NIGHT("night", R.string.theme_color_night, R.string.theme_color_night_desc, true),
    GREEN("green", R.string.theme_color_green, R.string.theme_color_green_desc, false);

    companion object {
        fun fromKey(key: String?): AppColorTheme =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class AppUiStyle(
    val key: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int
) {
    MODERN("modern", R.string.theme_ui_modern, R.string.theme_ui_modern_desc),
    CLASSIC("classic", R.string.theme_ui_classic, R.string.theme_ui_classic_desc),
    COMPACT("compact", R.string.theme_ui_compact, R.string.theme_ui_compact_desc),
    GALLERY("gallery", R.string.theme_ui_gallery, R.string.theme_ui_gallery_desc);

    companion object {
        fun fromKey(key: String?): AppUiStyle =
            entries.firstOrNull { it.key == key } ?: MODERN
    }
}

object ThemeStore {
    private const val PREFS = "einkauf_theme"
    private const val KEY_COLOR = "color_theme"
    private const val KEY_STYLE = "ui_style"

    private var loaded = false

    var colorTheme by mutableStateOf(AppColorTheme.SYSTEM)
        private set
    var uiStyle by mutableStateOf(AppUiStyle.MODERN)
        private set

    /** Uygulama/Activity açılışında bir kez çağrılır; kayıtlı seçimi yükler. */
    fun init(context: Context) {
        if (loaded) return
        val prefs = prefs(context)
        colorTheme = AppColorTheme.fromKey(prefs.getString(KEY_COLOR, null))
        uiStyle = AppUiStyle.fromKey(prefs.getString(KEY_STYLE, null))
        loaded = true
    }

    fun setColorTheme(context: Context, theme: AppColorTheme) {
        colorTheme = theme
        prefs(context).edit { putString(KEY_COLOR, theme.key) }
    }

    fun setUiStyle(context: Context, style: AppUiStyle) {
        uiStyle = style
        prefs(context).edit { putString(KEY_STYLE, style.key) }
    }

    fun reset(context: Context) {
        setColorTheme(context, AppColorTheme.SYSTEM)
        setUiStyle(context, AppUiStyle.MODERN)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
