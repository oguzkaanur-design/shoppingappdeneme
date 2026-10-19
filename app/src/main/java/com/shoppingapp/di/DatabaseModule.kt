package com.shoppingapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.local.ShoppingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** v3 only adds the Pfand ledger; existing shopping data stays untouched. */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
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

    /** v4 adds the unit price used by the checkout total. */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `products` ADD COLUMN `priceEuro` REAL NOT NULL DEFAULT 0"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShoppingDatabase {
        return Room.databaseBuilder(
            context,
            ShoppingDatabase::class.java,
            "shopping_db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideShoppingDao(database: ShoppingDatabase): ShoppingDao {
        return database.shoppingDao()
    }
}
