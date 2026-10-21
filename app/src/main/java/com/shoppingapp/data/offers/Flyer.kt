package com.shoppingapp.data.offers

import java.time.Instant

enum class FlyerPhase {
    CURRENT,
    UPCOMING,
    PAST
}

/**
 * Tek bir haftalık prospektüs (broşür). [leafletId] prospektüsün sayfa görsellerini
 * adreslemek için kullanılır; sayfalar 0..[pageCount]-1 arasındadır.
 */
data class Flyer(
    val storeId: String,
    val storeName: String,
    val leafletId: Long,
    val pageCount: Int,
    val validFrom: Instant,
    val validTo: Instant,
    val offerCount: Int = 0
) {
    fun phase(now: Instant = Instant.now()): FlyerPhase = when {
        now.isBefore(validFrom) -> FlyerPhase.UPCOMING
        now.isAfter(validTo) -> FlyerPhase.PAST
        else -> FlyerPhase.CURRENT
    }
}
