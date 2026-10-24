package com.shoppingapp.feature.share

/** Paylaşılan metinden çıkarılan tek satır. */
data class ImportedLine(
    val name: String,
    val quantity: Double,
    val unit: String
)

/**
 * WhatsApp, mesaj veya not uygulamasından paylaşılan serbest metni alışveriş
 * satırlarına çevirir. Saf fonksiyon: ayrıştırma kuralları testlerle doğrulanır.
 *
 * Desteklenen biçimler:
 *  - her satır bir ürün ("Süt")
 *  - madde işaretleri ("- Süt", "• Ekmek", "1. Un")
 *  - adet/birim öneki ("3x Milch", "2 kg un", "1,5 l Su", "2 adet yumurta")
 *  - virgülle ayrılmış tek satır ("süt, ekmek, 2 kg un")
 */
object ListImport {

    private val bulletPrefix = Regex("^\\s*(?:[-–—•*·]+|\\d{1,2}[.)])\\s*")
    // Ondalık ayırıcı virgülden sonra boşluk gelmez ("1,5"); liste virgülünde gelir ("süt, ekmek").
    private val splitComma = Regex(",(?=\\s)")
    private val quantityPrefix = Regex(
        "^(\\d+(?:[.,]\\d+)?)\\s*(x|×|adet|stk|stück|kg|g|l|ml|paket|packung|dose|flasche|stange|bündel)?\\s+(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val headerWords = setOf(
        "alışveriş listesi", "alisveris listesi", "einkaufsliste", "shopping list", "liste", "list"
    )

    fun parse(raw: String, limit: Int = 100): List<ImportedLine> {
        if (raw.isBlank()) return emptyList()

        val lines = raw
            .replace("\r\n", "\n")
            .split('\n', ';')
            .flatMap { splitComma.split(it) }

        val parsed = LinkedHashMap<String, ImportedLine>()
        for (line in lines) {
            val item = parseLine(line) ?: continue
            val key = item.name.lowercase()
            val existing = parsed[key]
            parsed[key] = when {
                existing == null -> item
                existing.unit.equals(item.unit, ignoreCase = true) ->
                    existing.copy(quantity = existing.quantity + item.quantity)
                // Biri birimsiz (Adet), diğeri "1,5 l" gibi belirgin birimse belirgin olan kazanır.
                existing.unit == "Adet" -> item
                item.unit == "Adet" -> existing
                else -> existing
            }
            if (parsed.size >= limit) break
        }
        return parsed.values.toList()
    }

    private fun parseLine(line: String): ImportedLine? {
        var text = line.trim()
        text = bulletPrefix.replace(text, "").trim()
        if (text.length < 2 || text.length > 90) return null
        if (text.startsWith("http://") || text.startsWith("https://")) return null
        if (text.lowercase() in headerWords) return null
        if (text.none { it.isLetter() }) return null

        val match = quantityPrefix.find(text)
        val quantity: Double
        val unit: String
        var name = text

        if (match != null) {
            quantity = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0
            val rawUnit = match.groupValues[2].trim()
            name = match.groupValues[3].trim()
            unit = when {
                rawUnit.isEmpty() -> "Adet"
                rawUnit.equals("x", true) || rawUnit == "×" -> "Adet"
                rawUnit.equals("adet", true) || rawUnit.equals("stk", true) ||
                    rawUnit.equals("stück", true) -> "Adet"
                else -> rawUnit.lowercase()
            }
        } else {
            quantity = 1.0
            unit = "Adet"
        }

        name = name.trim().trim('.', ',', ':', '!', '?', '-').trim()
        if (name.length < 2) return null
        if (name.none { it.isLetter() }) return null

        return ImportedLine(
            name = name.replace(Regex("\\s+"), " "),
            quantity = quantity.coerceIn(0.1, 999.0),
            unit = unit
        )
    }
}
