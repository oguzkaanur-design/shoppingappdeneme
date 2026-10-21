package com.shoppingapp.data.offers

import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Katalog adresine günlük bir sürüm parametresi ekler. Aksi hâlde ara sunucular
 * önceki haftanın broşür listesini önbellekten döndürebiliyor ve ekranda geçen
 * haftanın broşürü görünüyor.
 */
internal object FlyerCatalogUrl {

    fun forDay(store: FlyerStore, day: LocalDate = LocalDate.now(ZoneOffset.UTC)): String {
        val stamp = day.toString().replace("-", "")
        return "${store.catalogUrl}?cb=$stamp"
    }
}
