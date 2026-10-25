package com.shoppingapp.feature.language

import android.content.res.Resources
import java.util.Locale

data class AppLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val flag: String
)

object LanguageCatalog {
    val supported = listOf(
        AppLanguage("tr", "Türkçe", "Turkish", "🇹🇷"),
        AppLanguage("en", "English", "English", "🇺🇸"),
        AppLanguage("en-GB", "English (UK)", "English (UK)", "🇬🇧"),
        AppLanguage("de", "Deutsch", "German", "🇩🇪"),
        AppLanguage("bg", "Български", "Bulgarian", "🇧🇬"),
        AppLanguage("sr", "Српски", "Serbian", "🇷🇸"),
        AppLanguage("zh-CN", "简体中文", "Chinese (Simplified)", "🇨🇳"),
        AppLanguage("uk", "Українська", "Ukrainian", "🇺🇦"),
        AppLanguage("ru", "Русский", "Russian", "🇷🇺"),
        AppLanguage("pl", "Polski", "Polish", "🇵🇱"),
        AppLanguage("nl", "Nederlands", "Dutch", "🇳🇱")
    )

    fun byTag(tag: String?): AppLanguage? {
        if (tag.isNullOrBlank()) return null
        return supported.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
            ?: supported.firstOrNull { it.tag.substringBefore('-').equals(tag.substringBefore('-'), ignoreCase = true) }
    }

    fun filter(query: String): List<AppLanguage> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return supported
        return supported.filter { language ->
            "${language.nativeName} ${language.englishName} ${language.tag}"
                .lowercase(Locale.ROOT)
                .contains(needle)
        }
    }

    fun displayName(tag: String?): String {
        byTag(tag)?.let { return it.nativeName }
        return systemLanguageName()
    }

    fun systemLanguageName(): String {
        val locale = Resources.getSystem().configuration.locales[0]
        return locale.getDisplayName(locale)
    }
}
