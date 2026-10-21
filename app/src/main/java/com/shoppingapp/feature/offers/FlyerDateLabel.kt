package com.shoppingapp.feature.offers

import com.shoppingapp.data.offers.Flyer
import com.shoppingapp.data.offers.FlyerPhase
import java.time.Instant
import java.time.ZoneId

/** Prospektüs geçerlilik aralığını kısa Türkçe etiketle gösterir (örn. "19–24 Eki"). */
object FlyerDateLabel {

    private val berlin: ZoneId = ZoneId.of("Europe/Berlin")
    private val months = arrayOf(
        "Oca", "Şub", "Mar", "Nis", "May", "Haz",
        "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
    )

    fun range(from: Instant, to: Instant): String {
        val start = from.atZone(berlin)
        val end = to.atZone(berlin)
        val startMonth = months[start.monthValue - 1]
        val endMonth = months[end.monthValue - 1]
        return if (start.month == end.month) {
            "${start.dayOfMonth}–${end.dayOfMonth} $startMonth"
        } else {
            "${start.dayOfMonth} $startMonth – ${end.dayOfMonth} $endMonth"
        }
    }

    fun range(flyer: Flyer): String = range(flyer.validFrom, flyer.validTo)

    /** Tek bir günü "24 Eki" biçiminde gösterir. */
    fun day(instant: Instant): String {
        val date = instant.atZone(berlin)
        return "${date.dayOfMonth} ${months[date.monthValue - 1]}"
    }

    fun phaseLabel(flyer: Flyer, now: Instant = Instant.now()): String = when (flyer.phase(now)) {
        FlyerPhase.CURRENT -> "Bu hafta"
        FlyerPhase.UPCOMING -> "Gelecek hafta"
        FlyerPhase.PAST -> "Geçen hafta"
    }
}
