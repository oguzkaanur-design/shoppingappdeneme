package com.shoppingapp.data.offers

/**
 * Prospektüs sayfalarının görsel adresleri. Sayfalar WebP olarak yayınlanır:
 * 640 px kapak/önizleme, 1080 px okuma, 2000 px yakınlaştırma için yeterli.
 */
internal object FlyerPageUrls {
    private const val CDN = "https://cdn.marktguru.de/api/v1/leaflets"

    fun cover(leafletId: Long): String = page(leafletId, 0, "medium")

    fun page(leafletId: Long, index: Int, size: String = "xlarge"): String =
        "$CDN/$leafletId/images/pages/$index/$size.webp"
}
