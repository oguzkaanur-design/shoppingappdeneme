package com.shoppingapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.model.*

@Database(
    entities = [
        Product::class,
        Store::class,
        Category::class,
        ListItem::class,
        PantryItem::class,
        PurchaseHistory::class,
        PurchaseHistoryItem::class,
        PfandEntry::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ShoppingDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
}