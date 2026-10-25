package com.shoppingapp.data.pfand

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppingapp.data.local.ShoppingDatabase
import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.PfandEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kısmi Pfand iadesinin veritabanı davranışını doğrular: yalnızca ödenen tutara
 * denk gelen adet düşer, kalan kayıt korunur, para tek hareket olarak nakde yazılır.
 */
@RunWith(AndroidJUnit4::class)
class PfandCashInTest {

    private lateinit var db: ShoppingDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ShoppingDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun partialReturnDropsOnlyMatchingUnitsAndAddsCash() = runBlocking {
        val dao = db.shoppingDao()
        dao.insertPfandEntry(PfandEntry(label = "Plastik şişe", unitAmount = 0.25, quantity = 95))
        dao.insertPfandEntry(PfandEntry(label = "Kasa", unitAmount = 0.15, quantity = 10))

        val result = dao.cashInPfand(750)

        assertTrue(result.success)
        assertEquals(750, result.amountCents)
        assertEquals(30, result.returnedUnits)
        assertEquals(75, result.remainingUnits)

        val pending = dao.getPendingPfand().first()
        assertEquals(65, pending.first { it.label == "Plastik şişe" }.quantity)
        assertEquals(10, pending.first { it.label == "Kasa" }.quantity)

        val returned = dao.getReturnedPfand().first()
        assertEquals(1, returned.size)
        assertEquals(30, returned.single().quantity)
        assertEquals("Plastik şişe", returned.single().label)

        val money = dao.getMoneyEntries().first()
        assertEquals(1, money.size)
        assertEquals(MoneyAccount.CASH, money.single().account)
        assertEquals(7.50, money.single().amount, 0.001)
        assertEquals(MoneyAccount.PFAND_CASH_IN_NOTE, money.single().note)
    }

    @Test
    fun invalidAmountChangesNothing() = runBlocking {
        val dao = db.shoppingDao()
        dao.insertPfandEntry(PfandEntry(label = "Plastik şişe", unitAmount = 0.25, quantity = 4))

        val result = dao.cashInPfand(30)

        assertFalse(result.success)
        assertEquals(PfandPlanError.NO_EXACT_MATCH, result.error)
        assertEquals(4, dao.getPendingPfand().first().single().quantity)
        assertTrue(dao.getReturnedPfand().first().isEmpty())
        assertTrue(dao.getMoneyEntries().first().isEmpty())
    }

    @Test
    fun amountAbovePendingIsRejectedWithoutChanges() = runBlocking {
        val dao = db.shoppingDao()
        dao.insertPfandEntry(PfandEntry(label = "Kasa", unitAmount = 1.50, quantity = 2))

        val result = dao.cashInPfand(301)

        assertFalse(result.success)
        assertEquals(PfandPlanError.EXCEEDS_PENDING, result.error)
        assertEquals(2, dao.getPendingPfand().first().single().quantity)
        assertTrue(dao.getMoneyEntries().first().isEmpty())
    }

    @Test
    fun secondReturnUsesTheReducedQuantity() = runBlocking {
        val dao = db.shoppingDao()
        dao.insertPfandEntry(PfandEntry(label = "Plastik şişe", unitAmount = 0.25, quantity = 95))

        dao.cashInPfand(750)
        val second = dao.cashInPfand(750)

        assertTrue(second.success)
        val pending = dao.getPendingPfand().first().single()
        assertEquals(35, pending.quantity)
        assertEquals(15.00, dao.getMoneyEntries().first().sumOf { it.amount }, 0.001)
    }
}
