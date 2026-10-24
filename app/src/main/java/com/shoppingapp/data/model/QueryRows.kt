package com.shoppingapp.data.model

/**
 * Alışveriş geçmişi satırı: istatistikler (haftalık harcama, kategori dağılımı,
 * fiyat trendi) bu satırlardan hesaplanır. Fiyat ve adet alışveriş anında
 * kaydedildiği için ürün fiyatı sonradan değişse bile geçmiş bozulmaz.
 */
data class PurchaseHistoryLine(
    val historyId: Long,
    val productId: Long,
    val name: String,
    val unit: String,
    val categoryId: Long,
    val pfandAmount: Double,
    val priceEuro: Double,
    val quantity: Double,
    val purchaseDate: Long,
    val storeId: Long
)

/** Sık alınan ürün: geçmişteki alım sayısı ve son alım tarihi. */
data class FrequentProductRow(
    val productId: Long,
    val name: String,
    val unit: String,
    val categoryId: Long,
    val defaultStoreId: Long?,
    val priceEuro: Double,
    val times: Int,
    val lastAt: Long
)

/** Ana ekran widget'ında gösterilen kısa liste satırı. */
data class PendingPreviewRow(
    val name: String,
    val quantity: Double,
    val unit: String
)

/**
 * Bir ürün için en son ödenen birim fiyat. Barkod okutulduğunda "geçen sefer ne
 * ödemiştim?" sorusunu cevaplar; fiyat değişimini markete gitmeden gösterir.
 */
data class LastPriceRow(
    val priceEuro: Double,
    val purchaseDate: Long,
    val storeName: String?
)

/** Kiler satırı: stok kaydı ile ürün bilgisinin birleşimi. */
data class PantryRow(
    val id: Long,
    val productId: Long,
    val name: String,
    val unit: String,
    val categoryId: Long,
    val currentQuantity: Double,
    val minThreshold: Double,
    val expiryDate: Long?,
    val defaultStoreId: Long?,
    val priceEuro: Double
)
