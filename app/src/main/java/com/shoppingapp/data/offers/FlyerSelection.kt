package com.shoppingapp.data.offers

import java.time.Instant

/**
 * Katalog aynı hafta için birden fazla bölgesel broşür sürümü ve geçmiş haftaları
 * içerebilir. Ekranda bu haftanın en kapsamlı broşürü ile gelecek haftanın en
 * kapsamlı broşürü gösterilir; hiçbiri yoksa en son yayınlanan broşür gösterilir ki
 * ekran boş kalmasın.
 */
object FlyerSelection {

    fun relevant(flyers: List<Flyer>, now: Instant = Instant.now()): List<Flyer> {
        val current = flyers
            .filter { it.phase(now) == FlyerPhase.CURRENT }
            .maxByOrNull { it.pageCount }

        val nextWeekStart = flyers
            .filter { it.phase(now) == FlyerPhase.UPCOMING }
            .minOfOrNull { it.validFrom }
        val upcoming = nextWeekStart?.let { start ->
            flyers.filter { it.validFrom == start }.maxByOrNull { it.pageCount }
        }

        return listOfNotNull(current, upcoming).ifEmpty { flyers.take(1) }
    }
}
