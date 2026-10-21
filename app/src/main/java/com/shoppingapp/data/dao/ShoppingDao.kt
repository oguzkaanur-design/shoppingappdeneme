package com.shoppingapp.data.dao

import androidx.room.*
import com.shoppingapp.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: Store): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItem(item: ListItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistory(history: PurchaseHistory): Long

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM list_items")
    fun getAllListItems(): Flow<List<ListItem>>

    @Query("SELECT * FROM list_items WHERE isPurchased = 0")
    fun getActiveListItemsFlow(): Flow<List<ListItem>>

    @Query("SELECT * FROM stores")
    fun getAllStores(): Flow<List<Store>>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM pantry_items")
    fun getPantryItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM purchase_history")
    fun getPurchaseHistory(): Flow<List<PurchaseHistory>>

    @Update
    suspend fun updateListItem(item: ListItem)

    @Delete
    suspend fun deleteListItem(item: ListItem)

    @Query("DELETE FROM list_items WHERE isPurchased = 1")
    suspend fun clearPurchasedItems()

    @Query("DELETE FROM list_items")
    suspend fun clearAllListItems()

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("DELETE FROM purchase_history_items")
    suspend fun clearPurchaseHistoryItems()

    @Query("DELETE FROM purchase_history")
    suspend fun clearPurchaseHistory()

    @Query("DELETE FROM pfand_entries")
    suspend fun clearAllPfandEntries()

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getStoreById(id: Long): Store?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistoryItem(item: PurchaseHistoryItem): Long

    @Query("UPDATE products SET priceEuro = :price WHERE id = :productId")
    suspend fun updateProductPrice(productId: Long, price: Double)

    @Query("SELECT * FROM products WHERE imageUrl IS NULL")
    suspend fun getProductsWithoutImage(): List<Product>

    // ---- Pfand ----

    @Query("SELECT * FROM pfand_entries WHERE isReturned = 0 ORDER BY createdAt DESC")
    fun getPendingPfand(): Flow<List<PfandEntry>>

    @Query("SELECT * FROM pfand_entries WHERE isReturned = 1 ORDER BY createdAt DESC")
    fun getReturnedPfand(): Flow<List<PfandEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPfandEntry(entry: PfandEntry): Long

    @Delete
    suspend fun deletePfandEntry(entry: PfandEntry)

    @Query("UPDATE pfand_entries SET isReturned = 1 WHERE isReturned = 0")
    suspend fun markAllPfandReturned()

    @Query("DELETE FROM pfand_entries WHERE isReturned = 1")
    suspend fun clearReturnedPfand()

    // ---- Cebim (nakit / banka) ----

    @Query("SELECT * FROM money_entries ORDER BY createdAt DESC, id DESC")
    fun getMoneyEntries(): Flow<List<MoneyEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyEntry(entry: MoneyEntry): Long

    @Delete
    suspend fun deleteMoneyEntry(entry: MoneyEntry)

    @Query("DELETE FROM money_entries")
    suspend fun clearMoneyEntries()
}
