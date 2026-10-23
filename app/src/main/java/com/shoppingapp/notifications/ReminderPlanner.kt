package com.shoppingapp.notifications

import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.feature.pantry.PantrySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Gönderilecek tek bir hatırlatıcı. */
data class ReminderMessage(
    val key: String,
    val id: Int,
    val title: String,
    val text: String,
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
                    key = "offers",
                    id = NotificationHelper.ID_EXPIRING_OFFERS,
                    title = "${expiring.size} fırsatın süresi doluyor",
                    text = expiring.take(3).joinToString(", ") { it.productName } +
                        " — fırsat listesini kontrol et.",
                    route = "flyerList"
                )
            )
        }

        val lowStock = PantrySummary.restockNeeded(pantry, nowMillis)
        if (lowStock.isNotEmpty()) {
            reminders.add(
                ReminderMessage(
                    key = "pantry",
                    id = NotificationHelper.ID_LOW_STOCK,
                    title = "Kilerde ${lowStock.size} ürün azaldı",
                    text = lowStock.take(4).joinToString(", ") { it.name } +
                        " — tek dokunuşla alışveriş listesine ekleyebilirsin.",
                    route = "pantry"
                )
            )
        }

        if (today.dayOfWeek == DayOfWeek.SATURDAY && pendingCount == 0) {
            reminders.add(
                ReminderMessage(
                    key = "weekly",
                    id = NotificationHelper.ID_WEEKLY_SHOPPING,
                    title = "Haftalık alışveriş zamanı",
                    text = "Liste boş. Kilerdeki eksikleri tek dokunuşla ekleyip haftayı planlayabilirsin.",
                    route = "pantry"
                )
            )
        }

        return reminders
    }
}
