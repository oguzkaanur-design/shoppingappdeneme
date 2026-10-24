package com.shoppingapp.feature.receipt

import kotlin.math.abs

/** OCR'ın bulduğu tek satır ve sayfadaki konumu. */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * Fişler iki sütunlu okunur: ürün adı solda, tutar sağda. ML Kit metni blok blok
 * döndürdüğü için tutarlar ürünlerin altına düşer ve satır bazlı ayrıştırma bozulur.
 * Burada aynı yükseklikteki parçalar yeniden tek görsel satıra birleştirilir:
 * "MILSANI JOGHURT 3,5%   1,09 A".
 */
object ReceiptTextLayout {

    fun rows(lines: List<OcrLine>): List<String> {
        val sorted = lines
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy({ it.top }, { it.left }))
        if (sorted.isEmpty()) return emptyList()

        val rows = mutableListOf<String>()
        var current = mutableListOf<OcrLine>()
        var centerSum = 0.0
        var tallest = 0

        fun flush() {
            if (current.isEmpty()) return
            rows += current.sortedBy { it.left }.joinToString("  ") { it.text.trim() }
            current = mutableListOf()
            centerSum = 0.0
            tallest = 0
        }

        for (line in sorted) {
            val center = (line.top + line.bottom) / 2.0
            val height = (line.bottom - line.top).coerceAtLeast(1)
            val averageCenter = if (current.isEmpty()) center else centerSum / current.size
            // Aynı görsel satır: dikey merkezler satır yüksekliğinin yarısından yakın.
            val tolerance = maxOf(tallest, height) * 0.5
            if (current.isNotEmpty() && abs(center - averageCenter) > tolerance) {
                flush()
            }
            current += line
            centerSum += center
            tallest = maxOf(tallest, height)
        }
        flush()
        return rows
    }
}
