package com.shoppingapp.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.model.ListItem
import com.shoppingapp.data.model.Product
import com.shoppingapp.data.repository.ShoppingRepository
import com.shoppingapp.di.BackgroundEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Yedek geri yükleme yıkıcı bir işlemdir: önce tüm tablolar temizlenir.
 * Bozuk, eksik veya desteklenmeyen bir dosya bu temizliği **başlatmamalı**;
 * hata sonrası mevcut alışveriş listesi ve ürünler aynen durmalıdır.
 *
 * Test, üretim [BackupManager] ve Room veritabanını kullanır; ayrı bir kopya
 * oluşturmak yerine gerçek geri yükleme yolunu doğrular.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {

    private lateinit var manager: BackupManager
    private lateinit var dao: ShoppingDao
    private lateinit var repository: ShoppingRepository
    private val tempFiles = mutableListOf<File>()

    private var markerItemId = 0L
    private var createdProduct = false

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            BackgroundEntryPoint::class.java
        )
        manager = entryPoint.backupManager()
        dao = entryPoint.shoppingDao()
        repository = entryPoint.shoppingRepository()

        runBlocking(Dispatchers.IO) {
            repository.seedConfig()
            repository.initDefaultData()

            val product = dao.getAllProducts().first().firstOrNull()
                ?: Product(
                    name = "Yedek test ürünü",
                    barcode = "BACKUP_TEST_${System.currentTimeMillis()}",
                    categoryId = 1,
                    unit = "Adet",
                    defaultStoreId = 1,
                    priceEuro = 1.0
                ).let { created ->
                    createdProduct = true
                    created.copy(id = dao.insertProduct(created))
                }

            markerItemId = dao.insertListItem(
                ListItem(
                    productId = product.id,
                    storeId = 1,
                    quantity = 1.0,
                    isPurchased = false,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    @After
    fun tearDown() {
        runBlocking(Dispatchers.IO) {
            dao.getAllListItemsOnce()
                .filter { it.id == markerItemId }
                .forEach { dao.deleteListItem(it) }
            // Yalnızca tablo boşken oluşturduğumuz ürünü geri al.
            if (createdProduct) dao.deleteAllProducts()
        }
        tempFiles.forEach { it.delete() }
    }

    @Test
    fun bozukYedeklerMevcutVeriyiSilmez() {
        val beforeItems = runBlocking(Dispatchers.IO) { dao.getAllListItemsOnce() }
        val beforeProducts = runBlocking(Dispatchers.IO) { dao.getProductsOnce() }
        val beforeStores = runBlocking(Dispatchers.IO) { dao.getStoresOnce() }

        val invalidPayloads = listOf(
            "boş dosya" to "",
            "bozuk JSON" to "{\"schemaVersion\":1,",
            "desteklenmeyen şema" to
                """{"schemaVersion":99,"products":[{"id":1}],"stores":[]}""",
            "eksik ürün adı" to
                """{"schemaVersion":1,"products":[{"id":1,"barcode":"1","categoryId":1,"unit":"L","pfandAmount":0.0}],"stores":[{"id":1,"name":"PENNY","colorHex":"#E2001A","aisleOrderJson":"[]"}]}""",
            "yanlış alan tipi" to
                """{"schemaVersion":1,"products":[{"id":1,"name":"Süt","barcode":"1","categoryId":1,"unit":"L","pfandAmount":0.0,"priceEuro":"pahalı"}],"stores":[{"id":1,"name":"PENNY","colorHex":"#E2001A","aisleOrderJson":"[]"}]}""",
            "8 MB üstü dosya" to "x".repeat(BackupValidator.MAX_BYTES + 1)
        )

        invalidPayloads.forEach { (label, content) ->
            val result = runBlocking(Dispatchers.IO) {
                manager.restoreLocal(fileFor(content).toBackupFile())
            }

            assertTrue("$label reddedilmeliydi", result.isFailure)
            assertEquals(
                "$label sonrası liste değişmemeli",
                beforeItems.size,
                runBlocking(Dispatchers.IO) { dao.getAllListItemsOnce().size }
            )
            assertEquals(
                "$label sonrası ürünler değişmemeli",
                beforeProducts.size,
                runBlocking(Dispatchers.IO) { dao.getProductsOnce().size }
            )
            assertEquals(
                "$label sonrası marketler değişmemeli",
                beforeStores.size,
                runBlocking(Dispatchers.IO) { dao.getStoresOnce().size }
            )
            assertNotNull(
                "$label sonrası işaretli liste satırı kaybolmamalı",
                runBlocking(Dispatchers.IO) {
                    dao.getAllListItemsOnce().firstOrNull { it.id == markerItemId }
                }
            )
        }
    }

    @Test
    fun gecerliYedekGeriYuklenirVeFazladanKayitlariTemizler() {
        val snapshot = runBlocking(Dispatchers.IO) { manager.snapshot() }
        val json = runBlocking(Dispatchers.IO) { manager.exportJson() }
        val extraItemId = runBlocking(Dispatchers.IO) {
            dao.insertListItem(
                ListItem(
                    productId = snapshot.products.first().id,
                    storeId = 1,
                    quantity = 7.0,
                    isPurchased = false,
                    addedAt = System.currentTimeMillis() + 1
                )
            )
        }

        val result = runBlocking(Dispatchers.IO) {
            manager.restoreLocal(fileFor(json).toBackupFile())
        }

        assertTrue("Geçerli yedek geri yüklenmeliydi: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(snapshot.listItems.size, result.getOrThrow().listItems)
        assertEquals(snapshot.products.size, result.getOrThrow().products)

        runBlocking(Dispatchers.IO) {
            val restoredIds = dao.getAllListItemsOnce().map { it.id }
            assertFalse("Geri yükleme sonrası fazladan satır kalmamalı", extraItemId in restoredIds)
            assertEquals(snapshot.listItems.size, restoredIds.size)
            assertEquals(snapshot.products.size, dao.getProductsOnce().size)
            assertEquals(snapshot.stores.size, dao.getStoresOnce().size)
        }
    }

    private fun fileFor(content: String): File =
        File(context.cacheDir, "backup-test-${System.nanoTime()}.json").also { file ->
            file.writeText(content)
            tempFiles += file
        }

    private fun File.toBackupFile() = BackupFile(
        path = absolutePath,
        name = name,
        createdAt = lastModified(),
        sizeBytes = length(),
        isAuto = false
    )
}
