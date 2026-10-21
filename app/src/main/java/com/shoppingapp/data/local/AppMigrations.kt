package com.shoppingapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room şema geçişlerinin tek kaynağı.
 *
 * Kural: [ShoppingDatabase] sürümünü artırdığında, o adım için buraya bir [Migration] ekle ve
 * [ALL] listesine koy. Eksik bir geçiş artık veriyi sessizce silmez; uygulama açılırken
 * `IllegalStateException` ile durur, böylece hata kullanıcı verisini yok etmek yerine
 * geliştirme sırasında görünür olur.
 */
object AppMigrations {

    /** v3 yalnızca Pfand defterini ekler; mevcut alışveriş verisi korunur. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `pfand_entries` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`label` TEXT NOT NULL, " +
                    "`unitAmount` REAL NOT NULL, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`isReturned` INTEGER NOT NULL)"
            )
        }
    }

    /** v4, checkout toplamının kullandığı birim fiyatı ekler. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `products` ADD COLUMN `priceEuro` REAL NOT NULL DEFAULT 0"
            )
        }
    }

    /** v5, prospektüsten eklenen fırsat ürünleri için ayrı tablo ekler; mevcut veri korunur. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `flyer_list_items` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`offerId` INTEGER NOT NULL, " +
                    "`storeName` TEXT NOT NULL, " +
                    "`storeColor` INTEGER NOT NULL, " +
                    "`productName` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`unitPrice` REAL NOT NULL, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "`imagePath` TEXT, " +
                    "`validFromEpochMs` INTEGER NOT NULL, " +
                    "`validToEpochMs` INTEGER NOT NULL, " +
                    "`addedAt` INTEGER NOT NULL)"
            )
        }
    }

    /** `Room.databaseBuilder(...).addMigrations(*AppMigrations.ALL)` için sıralı liste. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
