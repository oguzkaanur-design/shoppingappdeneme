package com.shoppingapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shoppingapp.data.dao.FlyerListDao
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
        PfandEntry::class,
        FlyerListItem::class,
        MoneyEntry::class
    ],
    version = 7,
    // Şemalar app/schemas altına yazılır; geçişler bu dosyalara karşı test edilir.
    exportSchema = true
)
abstract class ShoppingDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
    abstract fun flyerListDao(): FlyerListDao
}