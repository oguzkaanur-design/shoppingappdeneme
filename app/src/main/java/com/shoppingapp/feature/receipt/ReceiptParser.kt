package com.shoppingapp.feature.receipt

/** Fişten okunan tek ürün satırı. */
data class ReceiptLine(
    val name: String,
    val quantity: Double,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class ParsedReceipt(
    val lines: List<ReceiptLine>,
    val total: Double?
)

/**
 * Market fişi OCR metnini ürün satırlarına çevirir.
 *
 * Fişlerde tutar satır sonundadır ("MILSANI JOGHURT 1,09 A"); vergi sınıfı harfi
 * (A/B) tutardan sonra gelir. Toplam/KDV/nakit satırları ürün sayılmaz.
 * Saf fonksiyon: gerçek fiş örnekleriyle test edilir.
 */
object ReceiptParser {

    private val moneyAtEnd = Regex("""(\d{1,4}[.,]\d{2})\s*(?:€|EUR)?\s*(?:[A-Z*])?\s*$""")
    private val quantityPrefix = Regex("""^(\d{1,3})\s*(?:x|×|stk\.?|stück)\s+""", RegexOption.IGNORE_CASE)
    private val quantitySuffix = Regex("""\s+(\d{1,3})\s*(?:x|×|stk\.?|stück)\s*$""", RegexOption.IGNORE_CASE)

    private val ignoredKeywords = listOf(
        "summe", "gesamt", "zu zahlen", "zahlbetrag", "total", "mwst", "ust", "netto",
        "brutto", "bar ", "barzahlung", "ec-karte", "kartenzahlung", "gegeben", "rückgeld",
        "wechselgeld", "rueckgeld", "rückgeld", "pfand", "leergut", "rabatt",
        "gutschein", "bon ", "filiale", "kasse", "datum", "uhrzeit", "bediener", "steuer",
        "zwischensumme", "gesamtbetrag"
    )

    fun parse(raw: String, limit: Int = 80): ParsedReceipt {
        if (raw.isBlank()) return ParsedReceipt(emptyList(), null)

        val lines = mutableListOf<ReceiptLine>()
        val seen = mutableSetOf<String>()
        var total: Double? = null

        raw.replace("\r\n", "\n").split('\n').forEach { rawLine ->
            val line = rawLine.replace(Regex("\\s+"), " ").trim()
            if (line.length < 4) return@forEach

            val match = moneyAtEnd.find(line) ?: return@forEach
            val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@forEach
            if (amount <= 0.0 || amount > 999.0) return@forEach

            var name = line.substring(0, match.range.first).trim()
            val lower = name.lowercase()

            if (ignoredKeywords.any { lower.contains(it) }) {
                if (total == null && amount > 0.0 &&
                    (lower.contains("summe") || lower.contains("gesamt") ||
                        lower.contains("zu zahlen") || lower.contains("total"))
                ) {
                    total = amount
                }
                return@forEach
            }

            var quantity = 1.0
            quantityPrefix.find(name)?.let { prefix ->
                quantity = prefix.groupValues[1].toDoubleOrNull() ?: 1.0
                name = name.substring(prefix.range.last + 1).trim()
            }
            quantitySuffix.find(name)?.let { suffix ->
                quantity = suffix.groupValues[1].toDoubleOrNull() ?: quantity
                name = name.substring(0, suffix.range.first).trim()
            }

            name = name.trim().trim('*', '-', ':', '.').trim()
            if (name.count { it.isLetter() } < 3) return@forEach
            if (lines.size >= limit) return@forEach

            val key = name.lowercase()
            if (!seen.add(key)) return@forEach

            val safeQuantity = quantity.coerceIn(1.0, 99.0)
            // "2 x MILCH 1,98" satırında tutar iki adedin toplamıdır; birim fiyat bölünerek bulunur.
            val unitPrice = if (safeQuantity > 1.0) amount / safeQuantity else amount
            lines += ReceiptLine(
                name = name.replace(Regex("\\s+"), " "),
                quantity = safeQuantity,
                unitPrice = (unitPrice * 100).toInt() / 100.0
            )
        }

        return ParsedReceipt(lines, total)
    }
}
