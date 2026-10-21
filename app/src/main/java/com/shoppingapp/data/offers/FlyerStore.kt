package com.shoppingapp.data.offers

/**
 * Prospektüs kaynağı: marketin uygulama içinde gösterilecek haftalık broşürleri.
 * [catalogUrl] broşür listesini yayınlayan sayfa, [pageUrl] hiçbir broşür
 * bulunamazsa kullanılacak resmî market sayfasıdır.
 */
data class FlyerStore(
    val id: String,
    val name: String,
    val shortName: String,
    val brandColor: Long,
    val catalogUrl: String,
    val pageUrl: String
)
