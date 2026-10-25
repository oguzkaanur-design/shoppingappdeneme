package com.shoppingapp.notifications

import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.feature.pantry.PantrySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Gönderilecek tek bir hatırlatıcı. Yalnızca karar verisi taşır; başlık ve metin
 * cihaz diline göre [NotificationHelper] içinde üretilir.
 */
data class ReminderMessage(
    val key: String,
    val id: Int,
    val count: Int,
    val names: List<String>,
    val route: String
)

/**
 * Hangi hatırlatıcının gönderileceğine karar veren saf mantık. Worker yalnızca
 * kararı bildirime çevirir; böylece kurallar (2 günlük fırsat penceresi, kritik
 * stok, cumartesi haftalık hatırlatma) JVM testleriyle doğrulanabilir.
 */
object ReminderPlanner {

    /** Son geçerlilik günü bu kadar kaldıysa fırsat hatırlatılır. */
    const val EXPIRY_WINDOW_MS = 2L * 24 * 60 * 60 * 1000

    const val KEY_OFFERS = "offers"
    const val KEY_PANTRY = "pantry"
    const val KEY_WEEKLY = "weekly"

    fun plan(
        offers: List<FlyerListItem>,
        pantry: List<PantryRow>,
        pendingCount: Int,
        nowMillis: Long = System.currentTimeMillis(),
        today: LocalDate = Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
    ): List<ReminderMessage> {
        val reminders = mutableListOf<ReminderMessage>()

        val expiring = offers.filter { it.validToEpochMs in nowMillis..(nowMillis + EXPIRY_WINDOW_MS) }
        if (expiring.isNotEmpty()) {
            reminders.add(
                ReminderMessage(
                    key = KEY_OFFERS,
                    id = NotificationHelper.ID_EXPIRING_OFFERS,
                    count = expiring.size,
                    names = expiring.take(3).map { it.productName },
                    route = "flyerList"
                )
            )
        }

        val lowStock = PantrySummary.restockNeeded(pantry, nowMillis)
        if (lowStock.isNotEmpty()) {
            reminders.add(
                ReminderMessage(
                    key = KEY_PANTRY,
                    id = NotificationHelper.ID_LOW_STOCK,
                    count = lowStock.size,
                    names = lowStock.take(4).map { it.name },
                    route = "pantry"
                )
            )
        }

        if (today.dayOfWeek == DayOfWeek.SATURDAY && pendingCount == 0) {
            reminders.add(
                ReminderMessage(
                    key = KEY_WEEKLY,
                    id = NotificationHelper.ID_WEEKLY_SHOPPING,
                    count = 0,
                    names = emptyList(),
                    route = "pantry"
                )
            )
        }

        return reminders
    }
}
