package com.shoppingapp.onboarding

import com.shoppingapp.money.Currency

/** Onboarding'de seçilen ülke; para birimini de belirler. */
data class Country(
    val code: String,
    val flag: String,
    val name: String,
    val currency: Currency
)

/**
 * Ülke listesi. Adlar ülkenin kendi dilinde yazılır ("Deutschland", "Türkiye",
 * "Nederland"); böylece seçim ekranı dil değişse de anlaşılır kalır.
 */
object CountryCatalog {

    val all: List<Country> = listOf(
        Country("DE", "🇩🇪", "Deutschland", Currency.EUR),
        Country("AT", "🇦🇹", "Österreich", Currency.EUR),
        Country("CH", "🇨🇭", "Schweiz", Currency.CHF),
        Country("NL", "🇳🇱", "Nederland", Currency.EUR),
        Country("BE", "🇧🇪", "België / Belgique", Currency.EUR),
        Country("LU", "🇱🇺", "Luxembourg", Currency.EUR),
        Country("FR", "🇫🇷", "France", Currency.EUR),
        Country("IT", "🇮🇹", "Italia", Currency.EUR),
        Country("ES", "🇪🇸", "España", Currency.EUR),
        Country("PT", "🇵🇹", "Portugal", Currency.EUR),
        Country("IE", "🇮🇪", "Éire / Ireland", Currency.EUR),
        Country("GR", "🇬🇷", "Ελλάδα", Currency.EUR),
        Country("TR", "🇹🇷", "Türkiye", Currency.TRY),
        Country("PL", "🇵🇱", "Polska", Currency.PLN),
        Country("CZ", "🇨🇿", "Česko", Currency.CZK),
        Country("HU", "🇭🇺", "Magyarország", Currency.HUF),
        Country("RO", "🇷🇴", "România", Currency.RON),
        Country("BG", "🇧🇬", "България", Currency.BGN),
        Country("RS", "🇷🇸", "Србија", Currency.RSD),
        Country("DK", "🇩🇰", "Danmark", Currency.DKK),
        Country("SE", "🇸🇪", "Sverige", Currency.SEK),
        Country("NO", "🇳🇴", "Norge", Currency.NOK),
        Country("GB", "🇬🇧", "United Kingdom", Currency.GBP),
        Country("US", "🇺🇸", "United States", Currency.USD),
        Country("UA", "🇺🇦", "Україна", Currency.UAH),
        Country("AE", "🇦🇪", "الإمارات", Currency.AED),
        Country("SA", "🇸🇦", "السعودية", Currency.SAR)
    )

    fun of(code: String?): Country? =
        all.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }

    /** Ülke kodu bilinmiyorsa avro bölgesi varsayılır; uygulama orada doğdu. */
    fun defaultCountry(): Country = of("DE")!!

    fun search(query: String): List<Country> {
        val needle = query.trim()
        if (needle.isEmpty()) return all
        return all.filter {
            it.name.contains(needle, ignoreCase = true) ||
                it.code.contains(needle, ignoreCase = true) ||
                it.currency.code.contains(needle, ignoreCase = true)
        }
    }

    /**
     * Sistem diline göre makul bir başlangıç: Türkçe cihazda Türkiye, Almanca
     * cihazda Almanya. Eşleşme yoksa avro bölgesi.
     */
    fun suggestFor(languageTag: String?): Country {
        val tag = languageTag?.lowercase().orEmpty()
        return when {
            tag.startsWith("tr") -> of("TR") ?: defaultCountry()
            tag.startsWith("de") -> defaultCountry()
            tag.startsWith("nl") -> of("NL") ?: defaultCountry()
            tag.startsWith("pl") -> of("PL") ?: defaultCountry()
            tag.startsWith("cs") -> of("CZ") ?: defaultCountry()
            tag.startsWith("hu") -> of("HU") ?: defaultCountry()
            tag.startsWith("ro") -> of("RO") ?: defaultCountry()
            tag.startsWith("bg") -> of("BG") ?: defaultCountry()
            tag.startsWith("sr") -> of("RS") ?: defaultCountry()
            tag.startsWith("uk") -> of("UA") ?: defaultCountry()
            tag.startsWith("en") -> of("GB") ?: defaultCountry()
            else -> defaultCountry()
        }
    }
}
