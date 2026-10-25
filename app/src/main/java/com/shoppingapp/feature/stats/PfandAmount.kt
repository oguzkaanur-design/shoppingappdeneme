package com.shoppingapp.feature.stats

import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * İade diyaloğundaki tutar alanının kuralları. Tutar sent cinsinden tamsayıya çevrilir;
 * böylece 0,25 € gibi Pfand birimleri kayan nokta yuvarlaması olmadan karşılaştırılır.
 */
internal object PfandAmount {

    /** Tek seferde girilebilecek en yüksek iade: 10.000 €. */
    const val MAX_CENTS = 1_000_000

    /**
     * Alanın ondalık ayırıcısı: tr/de/nl gibi dillerde ",", en/zh gibi dillerde ".".
     * Klavyenin ondalık tuşu hangi işareti üretirse üretsin alan bu işareti gösterir.
     */
    fun separatorFor(locale: Locale): Char =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /**
     * Kullanıcının yazdığı metni yalnızca rakam, tek ayırıcı ve iki ondalıkla sınırlar.
     * Virgül ve nokta aynı kabul edilir; yazılan değer [separator] ile normalize edilir.
     */
    fun sanitize(raw: String, separator: Char = '.'): String {
        val normalized = raw
            .map { if (it == ',' || it == '.') separator else it }
            .filter { it.isDigit() || it == separator }
            .joinToString("")
        val firstSeparator = normalized.indexOf(separator)
        if (firstSeparator < 0) return normalized.take(6)

        val whole = normalized.substring(0, firstSeparator).take(6)
        val fraction = normalized.substring(firstSeparator + 1).filter { it.isDigit() }.take(2)
        return "$whole$separator$fraction"
    }

    /**
     * Sent tutarını alana yazılabilecek metne çevirir: 1225 → "12,25" (tr) / "12.25" (en).
     * "Tümünü iade et" kısayolu bekleyen toplamı bu biçimde doldurur;
     * [sanitize] ve [parseCents] aynı değeri geri okur.
     */
    fun format(cents: Int, separator: Char = '.'): String {
        val safe = cents.coerceAtLeast(0)
        return String.format(Locale.ROOT, "%d%c%02d", safe / 100, separator, safe % 100)
    }

    /**
     * "12", "12,5", "12.50" gibi girdileri sente çevirir. Boş, sıfır, birden fazla
     * ayırıcı veya desteklenmeyen biçim null döner.
     */
    fun parseCents(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val comma = trimmed.indexOf(',')
        val dot = trimmed.indexOf('.')
        if (comma >= 0 && dot >= 0) return null

        val separator = if (comma >= 0) comma else dot
        val wholePart = if (separator >= 0) trimmed.substring(0, separator) else trimmed
        val fractionPart = if (separator >= 0) trimmed.substring(separator + 1) else ""

        if (wholePart.isEmpty() && fractionPart.isEmpty()) return null
        if (wholePart.any { !it.isDigit() } || fractionPart.any { !it.isDigit() }) return null
        if (wholePart.length > 6 || fractionPart.length > 2) return null

        val whole = wholePart.ifEmpty { "0" }.toIntOrNull() ?: return null
        val fraction = when (fractionPart.length) {
            0 -> 0
            1 -> fractionPart.toInt() * 10
            else -> fractionPart.toInt()
        }
        if (whole > MAX_CENTS / 100) return null

        val cents = whole * 100 + fraction
        if (cents <= 0 || cents > MAX_CENTS) return null
        return cents
    }
}
