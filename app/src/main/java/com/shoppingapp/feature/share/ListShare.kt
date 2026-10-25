package com.shoppingapp.feature.share

import java.util.Locale

/** Paylaşılacak tek satır. */
data class ShareLine(
    val name: String,
    val quantity: Double,
    val unit: String,
    val storeName: String? = null,
    val purchased: Boolean = false
)

/**
 * Paylaşım metninde kullanılan, cihaz diline göre çözülen etiketler.
 * Varsayılanı Türkçe; arayüz `context.getString` ile kendi dilini geçirir.
 */
data class ShareStrings(
    val empty: String = "Liste boş.",
    val noStore: String = "Market yok",
    val itemCount: (Int) -> String = { "$it ürün" }
)

/**
 * Alışveriş listesini WhatsApp/mesaj olarak göndermek için düz metin.
 * Saf fonksiyon: biçim testlerle doğrulanır.
 */
object ListShare {

    fun text(
        lines: List<ShareLine>,
        title: String = "Alışveriş listesi",
        includePurchased: Boolean = false,
        strings: ShareStrings = ShareStrings()
    ): String {
        val visible = lines.filter { includePurchased || !it.purchased }
        if (visible.isEmpty()) return "$title\n\n${strings.empty}"
        val grouped = visible.groupBy { it.storeName }

        return buildString {
            append(title)
            append('\n')
            append(strings.itemCount(visible.size))
            append("\n")
            grouped.forEach { (store, rows) ->
                append('\n')
                append(if (store.isNullOrBlank()) strings.noStore else store)
                append(":\n")
                rows.forEach { append("• ").append(line(it)).append('\n') }
            }
        }.trimEnd()
    }

    fun line(line: ShareLine): String {
        val quantity = if (line.quantity % 1.0 == 0.0) {
            line.quantity.toInt().toString()
        } else {
            String.format(Locale.GERMANY, "%.1f", line.quantity)
        }
        val unit = line.unit.trim()
        return if (unit.isBlank() || unit.equals("Adet", ignoreCase = true)) {
            "$quantity × ${line.name}"
        } else {
            "$quantity $unit ${line.name}"
        }
    }
}
