package com.shoppingapp.data.catalog

/**
 * Eski çağrı noktaları (kiler ekranı) için ince sarmalayıcı: simge artık
 * [ProductAvatars] kataloğundan gelir, böylece listedeki avatar ile kilerdeki
 * simge aynı üründe aynı görünür.
 */
object ProductIllustration {
    fun forProductName(name: String): String = ProductAvatars.emojiFor(name)
}
