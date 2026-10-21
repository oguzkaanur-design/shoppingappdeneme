package com.shoppingapp.data.offers

import java.time.Instant

/**
 * Broşür sayfasındaki tıklanabilir ürün alanı. Koordinatlar sayfa görseline göre
 * 0..1 aralığında normalize edilmiştir (sol üst 0,0 – sağ alt 1,1).
 */
data class FlyerHotspot(
    val offerId: Long,
    val pageIndex: Int,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float
) {
    val width: Float get() = (toX - fromX).coerceAtLeast(0f)
    val height: Float get() = (toY - fromY).coerceAtLeast(0f)
    val centerX: Float get() = fromX + width / 2f
    val centerY: Float get() = fromY + height / 2f
}

/** Prospektüsteki tek bir fırsat: ürün adı, fiyatı, geçerliliği ve sayfadaki yeri. */
data class FlyerOffer(
    val id: Long,
    val leafletId: Long = 0L,
    val pageIndex: Int,
    val name: String,
    val description: String,
    val price: Double?,
    val oldPrice: Double?,
    val validFrom: Instant?,
    val validTo: Instant?,
    val hotspot: FlyerHotspot? = null
) {
    fun withHotspot(spot: FlyerHotspot?): FlyerOffer = copy(hotspot = spot)
}
