package com.shoppingapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shoppingapp.di.DatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Güncellemelerde kullanıcı verisinin korunduğunu kanıtlayan geçiş testleri.
 *
 * Testler üretim yapılandırmasını ([DatabaseModule.buildDatabase]) kullanır; yani
 * `fallbackToDestructiveMigration()` geri eklenirse bu testler kırılır.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShoppingDatabase::class.java
    )

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val allTestDbs = listOf(DB_FROM_V5, DB_FROM_V3, DB_FROM_V2, DB_UNKNOWN_VERSION)

    @After
    fun tearDown() {
        allTestDbs.forEach(::deleteDatabase)
    }

    /**
     * v3'ten yükseltme: ürünler, liste ve Pfand kayıtları yerinde kalır; yeni `priceEuro`
     * kolonu varsayılan 0 ile gelir.
     */
    @Test
    fun v3tenV4eGecisVeriyiKorur() {
        deleteDatabase(DB_FROM_V3)

        helper.createDatabase(DB_FROM_V3, 3).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, nameDe, nameTr, iconName, colorHex) " +
                    "VALUES (1, 'Obst', 'Meyve', 'apple', '#FF0000')"
            )
            db.execSQL(
                "INSERT INTO stores (id, name, colorHex, aisleOrderJson) " +
                    "VALUES (1, 'Rewe', '#00FF00', '[]')"
            )
            db.execSQL(
                "INSERT INTO products (id, name, barcode, categoryId, imageUrl, unit, defaultStoreId, pfandAmount) " +
                    "VALUES (1, 'Milch', '4001234567890', 1, NULL, 'L', 1, 0.25)"
            )
            db.execSQL(
                "INSERT INTO list_items (id, productId, storeId, quantity, isPurchased, addedAt) " +
                    "VALUES (1, 1, 1, 2.0, 0, 1234)"
            )
            db.execSQL(
                "INSERT INTO pfand_entries (id, label, unitAmount, quantity, createdAt, isReturned) " +
                    "VALUES (1, 'Flaschen', 0.25, 4, 555, 0)"
            )
        }

        // Geçişler sonucunda şema v4 şemasıyla birebir eşleşmeli.
        helper.runMigrationsAndValidate(DB_FROM_V3, 4, true, AppMigrations.MIGRATION_3_4)

        val database = DatabaseModule.buildDatabase(context, DB_FROM_V3)
        try {
            val dao = database.shoppingDao()
            runBlocking {
                val product = dao.getProductById(1)
                assertNotNull("Ürün güncellemede kaybolmamalı", product)
                assertEquals("Milch", product!!.name)
                assertEquals("Barkod korunmalı", "4001234567890", product.barcode)
                assertEquals("Yeni kolon varsayılan 0 ile gelmeli", 0.0, product.priceEuro, 0.0001)

                assertEquals("Liste satırı korunmalı", 1, dao.getActiveListItemsFlow().first().size)
                assertEquals("Pfand kaydı korunmalı", 1, dao.getPendingPfand().first().size)
                assertEquals("Kategori korunmalı", 1, dao.getAllCategories().first().size)
                assertEquals("Market korunmalı", 1, dao.getAllStores().first().size)
            }
        } finally {
            database.close()
        }
    }

    /**
     * v5'ten yükseltme: cebim tablosu eklenir; liste, Pfand ve fırsat ürünleri yerinde kalır.
     */
    @Test
    fun v5tenV6eGecisVeriyiKorurVeCebimTablosunuEkler() {
        deleteDatabase(DB_FROM_V5)

        helper.createDatabase(DB_FROM_V5, 5).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, nameDe, nameTr, iconName, colorHex) " +
                    "VALUES (1, 'Obst', 'Meyve', 'apple', '#FF0000')"
            )
            db.execSQL(
                "INSERT INTO stores (id, name, colorHex, aisleOrderJson) " +
                    "VALUES (1, 'Rewe', '#00FF00', '[]')"
            )
            db.execSQL(
                "INSERT INTO products (id, name, barcode, categoryId, imageUrl, unit, defaultStoreId, pfandAmount, priceEuro) " +
                    "VALUES (1, 'Milch', '4001234567890', 1, NULL, 'L', 1, 0.25, 1.19)"
            )
            db.execSQL(
                "INSERT INTO pfand_entries (id, label, unitAmount, quantity, createdAt, isReturned) " +
                    "VALUES (1, 'Flaschen', 0.25, 4, 555, 0)"
            )
        }

        helper.runMigrationsAndValidate(DB_FROM_V5, 6, true, AppMigrations.MIGRATION_5_6)

        val database = DatabaseModule.buildDatabase(context, DB_FROM_V5)
        try {
            val dao = database.shoppingDao()
            runBlocking {
                assertEquals("Ürün korunmalı", "Milch", dao.getProductById(1)?.name)
                assertEquals("Pfand korunmalı", 1, dao.getPendingPfand().first().size)
                assertTrue("Cebim tablosu boş başlamalı", dao.getMoneyEntries().first().isEmpty())

                val id = dao.insertMoneyEntry(
                    com.shoppingapp.data.model.MoneyEntry(
                        account = com.shoppingapp.data.model.MoneyAccount.CASH,
                        amount = 12.5,
                        note = "harçlık",
                        createdAt = 999L
                    )
                )
                assertTrue("Cebim tablosuna yazılabilmeli", id > 0)
                assertEquals(1, dao.getMoneyEntries().first().size)
            }
        } finally {
            database.close()
        }
    }

    /**
     * v2'den yükseltme: en eski hâlâ geçişi olan sürüm. Pfand tablosu eklenir, ürünler kalır.
     */
    @Test
    fun v2denV4eGecisVeriyiKorurVePfandTablosunuEkler() {
        deleteDatabase(DB_FROM_V2)
        createV2Database(DB_FROM_V2)

        val database = DatabaseModule.buildDatabase(context, DB_FROM_V2)
        try {
            val dao = database.shoppingDao()
            runBlocking {
                val product = dao.getProductById(1)
                assertNotNull("Ürün v2'den yükseltmede kaybolmamalı", product)
                assertEquals("Brot", product!!.name)
                assertEquals("Yeni kolon varsayılan 0 ile gelmeli", 0.0, product.priceEuro, 0.0001)

                assertTrue("Pfand tablosu geçişle oluşmalı", dao.getPendingPfand().first().isEmpty())

                val inserted = dao.insertPfandEntry(
                    com.shoppingapp.data.model.PfandEntry(
                        label = "Dosen",
                        unitAmount = 0.25,
                        quantity = 3,
                        createdAt = 999L
                    )
                )
                assertTrue("Pfand tablosuna yazılabilmeli", inserted > 0)
                assertEquals(1, dao.getPendingPfand().first().size)
            }
        } finally {
            database.close()
        }
    }

    /**
     * Geçişi olmayan (çok eski) bir veritabanı açılırken uygulama **hata verir**;
     * sessizce silip yeniden oluşturmaz. İstenen davranışın asıl kilidi budur.
     */
    @Test
    fun gecisiOlmayanSurumVeriyiSilmedenHataVerir() {
        deleteDatabase(DB_UNKNOWN_VERSION)
        val file = context.getDatabasePath(DB_UNKNOWN_VERSION)
        file.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(file, null).use { raw ->
            raw.execSQL("CREATE TABLE IF NOT EXISTS `marker` (`id` INTEGER PRIMARY KEY, `value` TEXT NOT NULL)")
            raw.execSQL("INSERT INTO marker (id, value) VALUES (1, 'kullanıcı verisi')")
            raw.version = 1
        }

        val database = DatabaseModule.buildDatabase(context, DB_UNKNOWN_VERSION)
        val thrown = try {
            runBlocking { database.shoppingDao().getProductById(1) }
            null
        } catch (t: Throwable) {
            t
        } finally {
            runCatching { database.close() }
        }

        assertNotNull("Geçişi olmayan sürüm sessizce açılmamalı", thrown)
        assertTrue(
            "Veri kaybı yerine açık hata bekleniyordu, gelen: $thrown",
            thrown is IllegalStateException
        )

        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { check ->
            check.rawQuery("SELECT value FROM marker WHERE id = 1", null).use { cursor ->
                assertTrue("Kullanıcı verisi silinmiş olmamalı", cursor.moveToFirst())
                assertEquals("kullanıcı verisi", cursor.getString(0))
            }
        }
    }

    /** v2 şemasını ham SQL ile oluşturur (v4 eksi `products.priceEuro` ve `pfand_entries`). */
    private fun createV2Database(name: String) {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `barcode` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, " +
                    "`imageUrl` TEXT, `unit` TEXT NOT NULL, `defaultStoreId` INTEGER, `pfandAmount` REAL NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `stores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `aisleOrderJson` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`nameDe` TEXT NOT NULL, `nameTr` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `list_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`productId` INTEGER NOT NULL, `storeId` INTEGER NOT NULL, `quantity` REAL NOT NULL, " +
                    "`isPurchased` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pantry_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`productId` INTEGER NOT NULL, `currentQuantity` REAL NOT NULL, `minThreshold` REAL NOT NULL, " +
                    "`expiryDate` INTEGER)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `purchase_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`storeId` INTEGER NOT NULL, `totalAmount` REAL NOT NULL, `pfandTotal` REAL NOT NULL, " +
                    "`purchaseDate` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `purchase_history_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`historyId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `priceEuro` REAL NOT NULL, " +
                    "`quantity` REAL NOT NULL)"
            )

            db.execSQL(
                "INSERT INTO products (id, name, barcode, categoryId, imageUrl, unit, defaultStoreId, pfandAmount) " +
                    "VALUES (1, 'Brot', '4009999999999', 1, NULL, 'Stk', NULL, 0.0)"
            )
            db.version = 2
        }
    }

    private fun deleteDatabase(name: String) {
        val file: File = context.getDatabasePath(name)
        listOf(file, File("${file.path}-wal"), File("${file.path}-shm"), File("${file.path}-journal"))
            .forEach { it.delete() }
    }

    private companion object {
        const val DB_FROM_V5 = "migration_from_v5.db"
        const val DB_FROM_V3 = "migration_from_v3.db"
        const val DB_FROM_V2 = "migration_from_v2.db"
        const val DB_UNKNOWN_VERSION = "migration_unknown.db"
    }
}
