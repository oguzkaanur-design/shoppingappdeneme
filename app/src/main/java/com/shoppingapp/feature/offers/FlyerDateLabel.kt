package com.shoppingapp.feature.offers

import com.shoppingapp.data.offers.Flyer
import com.shoppingapp.data.offers.FlyerPhase
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Prospektüs geçerlilik aralığını kısa etiketle gösterir (örn. "19–24 Eki").
 * Ay kısaltmaları cihaz dilinden gelir; testler dili açıkça verir.
 */
object FlyerDateLabel {

    private val berlin: ZoneId = ZoneId.of("Europe/Berlin")

    private fun month(date: ZonedDateTime, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("MMM", locale))

    fun range(from: Instant, to: Instant, locale: Locale = Locale.getDefault()): String {
        val start = from.atZone(berlin)
        val end = to.atZone(berlin)
        val startMonth = month(start, locale)
        val endMonth = month(end, locale)
        return if (start.month == end.month) {
            "${start.dayOfMonth}–${end.dayOfMonth} $startMonth"
        } else {
            "${start.dayOfMonth} $startMonth – ${end.dayOfMonth} $endMonth"
        }
    }

    fun range(flyer: Flyer, locale: Locale = Locale.getDefault()): String =
        range(flyer.validFrom, flyer.validTo, locale)

    /** Tek bir günü "24 Eki" biçiminde gösterir. */
    fun day(instant: Instant, locale: Locale = Locale.getDefault()): String {
        val date = instant.atZone(berlin)
        return "${date.dayOfMonth} ${month(date, locale)}"
    }

    fun phase(flyer: Flyer, now: Instant = Instant.now()): FlyerPhase = flyer.phase(now)
}
