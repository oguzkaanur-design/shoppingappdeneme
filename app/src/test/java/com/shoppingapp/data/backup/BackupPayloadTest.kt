package com.shoppingapp.data.backup

import com.google.gson.Gson
import com.shoppingapp.data.model.Category
import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.model.ListItem
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.data.model.PantryItem
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.data.model.Product
import com.shoppingapp.data.model.PurchaseHistory
import com.shoppingapp.data.model.PurchaseHistoryItem
import com.shoppingapp.data.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupPayloadTest {

    private val gson = Gson()

    private fun payload() = BackupPayload(
        createdAt = 1_700_000_000_000,
        products = listOf(
            Product(
                id = 7,
                name = "Süt",
                barcode = "CUSTOM_1",
                categoryId = 2,
                unit = "L",
                defaultStoreId = 1,
                pfandAmount = 0.25,
                priceEuro = 1.09
            )
        ),
        stores = listOf(Store(id = 1, name = "Rewe", colorHex = "#E2001A", aisleOrderJson = "[1,2,3]")),
        categories = listOf(Category(id = 2, nameDe = "Milch", nameTr = "Süt", iconName = "milk", colorHex = "#2196F3")),
        listItems = listOf(ListItem(id = 3, productId = 7, storeId = 1, quantity = 2.0, addedAt = 123)),
        pantryItems = listOf(PantryItem(id = 4, productId = 7, currentQuantity = 1.0, minThreshold = 2.0, expiryDate = 999)),
        purchaseHistory = listOf(PurchaseHistory(id = 5, storeId = 1, totalAmount = 2.18, pfandTotal = 0.5, purchaseDate = 555)),
        purchaseHistoryItems = listOf(PurchaseHistoryItem(id = 6, historyId = 5, productId = 7, priceEuro = 1.09, quantity = 2.0)),
        pfandEntries = listOf(PfandEntry(id = 8, label = "Kutu", unitAmount = 0.25, quantity = 2, isReturned = true)),
        moneyEntries = listOf(MoneyEntry(id = 9, account = "CASH", amount = 0.5, note = "Pfand iadesi")),
        flyerListItems = listOf(
            FlyerListItem(
                id = 10,
                offerId = 42,
                storeName = "REWE",
                storeColor = 0xFFD71920,
                productName = "Kaffee",
                unitPrice = 4.99,
                quantity = 1,
                validFromEpochMs = 1,
                validToEpochMs = 2
            )
        )
    )

    @Test
    fun `yedek JSON gidis donusu tum tablolari korur`() {
        val json = gson.toJson(payload())
        val restored = gson.fromJson(json, BackupPayload::class.java)

        assertNotNull(restored)
        assertEquals(1, restored.products.size)
        assertEquals("Süt", restored.products.first().name)
        assertEquals(1.09, restored.products.first().priceEuro, 0.001)
        assertEquals("[1,2,3]", restored.stores.first().aisleOrderJson)
        assertEquals(2.0, restored.listItems.first().quantity, 0.001)
        assertEquals(2.0, restored.pantryItems.first().minThreshold, 0.001)
        assertEquals(999L, restored.pantryItems.first().expiryDate)
        assertEquals(5L, restored.purchaseHistory.first().id)
        assertEquals(2.0, restored.purchaseHistoryItems.first().quantity, 0.001)
        assertEquals(0xFFD71920, restored.flyerListItems.first().storeColor)
        assertEquals("Pfand iadesi", restored.moneyEntries.first().note)
        assertEquals(true, restored.pfandEntries.first().isReturned)
    }

    @Test
    fun `eski veya eksik alanli yedek bos listelerle okunur`() {
        val restored = gson.fromJson("""{"schemaVersion":1}""", BackupPayload::class.java)

        assertNotNull(restored)
        assertEquals(1, restored.schemaVersion)
        assertEquals(0, restored.products.size)
        assertEquals(0, restored.flyerListItems.size)
    }

    @Test
    fun `ozet satir sayilarini dogru verir`() {
        val summary = BackupSummary(
            createdAt = 0,
            products = 1,
            listItems = 2,
            pantryItems = 3,
            purchases = 4,
            pfandEntries = 5,
            moneyEntries = 6,
            flyerItems = 7
        )

        assertEquals(
            "2 liste · 3 kiler · 4 alışveriş · 7 fırsat · 6 cebim hareketi",
            summary.headline
        )
    }
}
