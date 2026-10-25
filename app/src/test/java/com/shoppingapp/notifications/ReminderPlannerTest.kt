package com.shoppingapp.notifications

import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.model.PantryRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderPlannerTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 10, 24) // Cumartesi
    private val now = today.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
    private val day = 24L * 60 * 60 * 1000

    private fun offer(name: String, validTo: Long) = FlyerListItem(
        offerId = name.hashCode().toLong(),
        storeName = "REWE",
        storeColor = 0xFFD71920,
        productName = name,
        unitPrice = 1.99,
        validFromEpochMs = now - 3 * day,
        validToEpochMs = validTo
    )

    private fun pantry(name: String, quantity: Double, threshold: Double = 1.0) = PantryRow(
        id = name.hashCode().toLong(),
        productId = name.hashCode().toLong(),
        name = name,
        unit = "Adet",
        categoryId = 2,
        currentQuantity = quantity,
        minThreshold = threshold,
        expiryDate = null,
        defaultStoreId = 1,
        priceEuro = 0.0
    )

    @Test
    fun `suresi iki gun icinde dolan firsat hatirlatilir`() {
        val offers = listOf(offer("Kaffee", now + day), offer("Kuchen", now + 30 * day))
        val reminders = ReminderPlanner.plan(offers, emptyList(), 1, now, today)

        val offerReminder = reminders.single { it.key == "offers" }
        assertEquals(1, offerReminder.count)
        assertTrue(offerReminder.names.contains("Kaffee"))
        assertTrue(!offerReminder.names.contains("Kuchen"))
        assertEquals("flyerList", offerReminder.route)
        assertEquals(NotificationHelper.ID_EXPIRING_OFFERS, offerReminder.id)
    }

    @Test
    fun `suresi gecmis veya cok uzak firsat hatirlatilmaz`() {
        val offers = listOf(
            offer("Eski", now - day),
            offer("Yeni", now + 40 * day)
        )
        assertTrue(ReminderPlanner.plan(offers, emptyList(), 1, now, today).none { it.key == "offers" })
    }

    @Test
    fun `kritik kiler urunu hatirlatilir`() {
        val pantry = listOf(
            pantry("Süt", quantity = 0.0),
            pantry("Ekmek", quantity = 1.0),
            pantry("Zeytin", quantity = 5.0)
        )
        val reminder = ReminderPlanner.plan(emptyList(), pantry, 1, now, today)
            .single { it.key == "pantry" }

        assertEquals(2, reminder.count)
        assertTrue(reminder.names.contains("Süt"))
        assertTrue(reminder.names.contains("Ekmek"))
        assertTrue(!reminder.names.contains("Zeytin"))
        assertEquals("pantry", reminder.route)
    }

    @Test
    fun `yeterli stokta kiler hatirlatmasi yok`() {
        val pantry = listOf(pantry("Süt", quantity = 4.0))
        assertTrue(ReminderPlanner.plan(emptyList(), pantry, 1, now, today).none { it.key == "pantry" })
    }

    @Test
    fun `cumartesi bos listede haftalik hatirlatma`() {
        val reminder = ReminderPlanner.plan(emptyList(), emptyList(), 0, now, today)
            .single { it.key == "weekly" }

        assertEquals(NotificationHelper.ID_WEEKLY_SHOPPING, reminder.id)
        assertEquals("pantry", reminder.route)
    }

    @Test
    fun `dolu listede veya hafta ici haftalik hatirlatma yok`() {
        assertTrue(ReminderPlanner.plan(emptyList(), emptyList(), 3, now, today).none { it.key == "weekly" })

        val friday = LocalDate.of(2026, 10, 23)
        val fridayNow = friday.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
        assertTrue(
            ReminderPlanner.plan(emptyList(), emptyList(), 0, fridayNow, friday).none { it.key == "weekly" }
        )
    }

    @Test
    fun `uc kosul birlikte uc hatirlatici uretir`() {
        val reminders = ReminderPlanner.plan(
            offers = listOf(offer("Kaffee", now + day)),
            pantry = listOf(pantry("Süt", quantity = 0.0)),
            pendingCount = 0,
            nowMillis = now,
            today = today
        )

        assertEquals(listOf("offers", "pantry", "weekly"), reminders.map { it.key })
        assertEquals(reminders.size, reminders.map { it.id }.distinct().size)
    }
}
