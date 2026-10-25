package com.shoppingapp.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Uygulamanın para birimleri. Tutarlar veritabanında olduğu gibi (kayan noktalı
 * sayı) saklanır; para birimi yalnızca **gösterim** biçimini belirler. Ülke
 * seçimi bu birimlerden birini otomatik seçer, kullanıcı ayarlardan değiştirebilir.
 */
enum class Currency(
    val code: String,
    val symbol: String,
    val decimals: Int = 2,
    val decimalSeparator: Char = ',',
    val groupingSeparator: Char = '.',
    /** Sembol sayının önüne mi yazılır (İngiliz/Amerikan biçimi)? */
    val symbolFirst: Boolean = false
) {
    EUR("EUR", "€"),
    TRY("TRY", "₺"),
    USD("USD", "$", decimalSeparator = '.', groupingSeparator = ',', symbolFirst = true),
    GBP("GBP", "£", decimalSeparator = '.', groupingSeparator = ',', symbolFirst = true),
    CHF("CHF", "CHF"),
    PLN("PLN", "zł"),
    CZK("CZK", "Kč"),
    HUF("HUF", "Ft", decimals = 0),
    RON("RON", "lei"),
    BGN("BGN", "лв"),
    RSD("RSD", "din"),
    SEK("SEK", "kr"),
    DKK("DKK", "kr"),
    NOK("NOK", "kr"),
    UAH("UAH", "₴"),
    AED("AED", "AED"),
    SAR("SAR", "SAR");

    companion object {
        /** Bilinmeyen/boş kodda avroya döner: uygulamanın eski verisi avroyla girildi. */
        fun fromCode(code: String?): Currency =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: EUR
    }
}

/**
 * Tutarları seçilen para birimine göre biçimlendirir.
 *
 * `1.234,56 €` (avro, lira, zloti…) veya `$1,234.56` (dolar, sterlin) üretir.
 * Yuvarlama [BigDecimal] ile yapılır; `0.1 + 0.2` tipi kayan nokta artıkları
 * ekrana yansımaz.
 */
object MoneyFormat {

    fun amount(value: Double, currency: Currency = CurrencyStore.current()): String {
        val safe = if (value.isFinite()) value else 0.0
        val rounded = BigDecimal.valueOf(safe)
            .setScale(currency.decimals, RoundingMode.HALF_UP)
        val plain = rounded.abs().toPlainString()
        val body = buildString {
            if (currency.symbolFirst) append(currency.symbol)
            append(grouped(plain, currency))
            if (!currency.symbolFirst) append(' ').append(currency.symbol)
        }
        return if (safe < 0.0) "−$body" else body
    }

    /** İşaretli gösterim: `+12,40 €`, `−3,00 €`, `0,00 €`. */
    fun signed(value: Double, currency: Currency = CurrencyStore.current()): String = when {
        value > 0.0 -> "+" + amount(value, currency)
        else -> amount(value, currency)
    }

    private fun grouped(plain: String, currency: Currency): String {
        val dot = plain.indexOf('.')
        val whole = if (dot >= 0) plain.substring(0, dot) else plain
        val fraction = if (dot >= 0) plain.substring(dot + 1) else ""
        val groupedWhole = whole.reversed()
            .chunked(3)
            .joinToString(currency.groupingSeparator.toString())
            .reversed()
        return if (fraction.isEmpty()) groupedWhole
        else groupedWhole + currency.decimalSeparator + fraction
    }

    /**
     * Kullanıcının yazdığı tutarı okur: `"12,50"`, `"12.50"`, `" ₺ 7 "` hepsi geçerli.
     * Geçersiz, sıfır veya negatif girdilerde `null` döner.
     */
    fun parse(raw: String, currency: Currency = CurrencyStore.current()): Double? {
        var text = raw.trim()
        Currency.entries.forEach { text = text.replace(it.symbol, "") }
        text = text.replace("\u00A0", "").replace(" ", "")
        if (text.isEmpty()) return null
        // Tutarlar yalnızca pozitif olabilir; eksi işareti sessizce yutulmamalı.
        if (text.contains('-') || text.contains('\u2212')) return null

        // Hem "1.234,56" hem "1,234.56" biçimi: en sağdaki ayırıcı ondalıktır.
        val lastComma = text.lastIndexOf(',')
        val lastDot = text.lastIndexOf('.')
        val decimalIndex = maxOf(lastComma, lastDot)
        val normalized = if (decimalIndex < 0) {
            text
        } else {
            val whole = text.substring(0, decimalIndex).filter { it.isDigit() }
            val fraction = text.substring(decimalIndex + 1).filter { it.isDigit() }
            if (whole.isEmpty() && fraction.isEmpty()) return null
            "$whole.$fraction"
        }
        if (normalized.any { !it.isDigit() && it != '.' }) return null

        val value = normalized.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value <= 0.0) return null
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}
