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

    @Query("SELECT * FROM products WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findProductByName(name: String): Product?

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

    @Query("SELECT * FROM pantry_items")
    suspend fun getPantryItemsOnce(): List<PantryItem>

    // ---- Kiler (Pantry) ----

    @Query(
        "SELECT pi.id AS id, pi.productId AS productId, p.name AS name, p.unit AS unit, " +
            "p.categoryId AS categoryId, pi.currentQuantity AS currentQuantity, " +
            "pi.minThreshold AS minThreshold, pi.expiryDate AS expiryDate, " +
            "p.defaultStoreId AS defaultStoreId, p.priceEuro AS priceEuro " +
            "FROM pantry_items pi INNER JOIN products p ON p.id = pi.productId " +
            "ORDER BY p.name COLLATE NOCASE ASC"
    )
    fun getPantryRows(): Flow<List<PantryRow>>

    @Query(
        "SELECT pi.id AS id, pi.productId AS productId, p.name AS name, p.unit AS unit, " +
            "p.categoryId AS categoryId, pi.currentQuantity AS currentQuantity, " +
            "pi.minThreshold AS minThreshold, pi.expiryDate AS expiryDate, " +
            "p.defaultStoreId AS defaultStoreId, p.priceEuro AS priceEuro " +
            "FROM pantry_items pi INNER JOIN products p ON p.id = pi.productId " +
            "ORDER BY p.name COLLATE NOCASE ASC"
    )
    suspend fun getPantryRowsOnce(): List<PantryRow>

    @Query("SELECT * FROM pantry_items WHERE productId = :productId LIMIT 1")
    suspend fun getPantryItemForProduct(productId: Long): PantryItem?

    @Delete
    suspend fun deletePantryItem(item: PantryItem)

    @Query("UPDATE pantry_items SET currentQuantity = :quantity WHERE id = :id")
    suspend fun setPantryQuantity(id: Long, quantity: Double)

    /**
     * Alışveriş tamamlanınca kilerdeki stoğu artırır. Kayıt yoksa hiçbir şey yapmaz;
     * böylece geçmişteki her ürün kendiliğinden kilere eklenmez.
     */
    @Query("UPDATE pantry_items SET currentQuantity = currentQuantity + :amount WHERE productId = :productId")
    suspend fun increasePantryQuantity(productId: Long, amount: Double)

    @Query("DELETE FROM pantry_items")
    suspend fun clearPantryItems()

    // ---- İstatistik ve hızlı ekleme ----

    @Query(
        "SELECT phi.historyId AS historyId, phi.productId AS productId, p.name AS name, " +
            "p.unit AS unit, p.categoryId AS categoryId, p.pfandAmount AS pfandAmount, " +
            "phi.priceEuro AS priceEuro, phi.quantity AS quantity, " +
            "ph.purchaseDate AS purchaseDate, ph.storeId AS storeId " +
            "FROM purchase_history_items phi " +
            "INNER JOIN purchase_history ph ON ph.id = phi.historyId " +
            "INNER JOIN products p ON p.id = phi.productId " +
            "ORDER BY ph.purchaseDate ASC, phi.id ASC"
    )
    fun getHistoryLines(): Flow<List<PurchaseHistoryLine>>

    @Query(
        "SELECT p.id AS productId, p.name AS name, p.unit AS unit, p.categoryId AS categoryId, " +
            "p.defaultStoreId AS defaultStoreId, p.priceEuro AS priceEuro, " +
            "COUNT(phi.id) AS times, MAX(ph.purchaseDate) AS lastAt " +
            "FROM purchase_history_items phi " +
            "INNER JOIN purchase_history ph ON ph.id = phi.historyId " +
            "INNER JOIN products p ON p.id = phi.productId " +
            "GROUP BY p.id ORDER BY times DESC, lastAt DESC LIMIT :limit"
    )
    fun getFrequentProducts(limit: Int): Flow<List<FrequentProductRow>>

    @Query("SELECT * FROM purchase_history ORDER BY purchaseDate DESC LIMIT 1")
    suspend fun getLastPurchaseOnce(): PurchaseHistory?

    @Query("SELECT * FROM purchase_history_items WHERE historyId = :historyId ORDER BY id ASC")
    suspend fun getHistoryItemsOnce(historyId: Long): List<PurchaseHistoryItem>

    @Query("SELECT * FROM list_items WHERE isPurchased = 0")
    suspend fun getActiveListItemsOnce(): List<ListItem>

    @Query("SELECT * FROM list_items")
    suspend fun getAllListItemsOnce(): List<ListItem>

    @Query("SELECT COUNT(*) FROM list_items WHERE isPurchased = 0")
    suspend fun getPendingCountOnce(): Int

    @Query(
        "SELECT p.name AS name, li.quantity AS quantity, p.unit AS unit " +
            "FROM list_items li INNER JOIN products p ON p.id = li.productId " +
            "WHERE li.isPurchased = 0 ORDER BY li.addedAt ASC LIMIT :limit"
    )
    suspend fun getPendingPreview(limit: Int): List<PendingPreviewRow>

    // ---- Yedekleme (tek seferlik okuma / toplu yazma) ----

    @Query("SELECT * FROM products")
    suspend fun getProductsOnce(): List<Product>

    @Query("SELECT * FROM stores")
    suspend fun getStoresOnce(): List<Store>

    @Query("SELECT * FROM categories")
    suspend fun getCategoriesOnce(): List<Category>

    @Query("SELECT * FROM purchase_history")
    suspend fun getPurchaseHistoryOnce(): List<PurchaseHistory>

    @Query("SELECT * FROM purchase_history_items")
    suspend fun getPurchaseHistoryItemsOnce(): List<PurchaseHistoryItem>

    @Query("SELECT * FROM pfand_entries")
    suspend fun getPfandEntriesOnce(): List<PfandEntry>

    @Query("SELECT * FROM money_entries")
    suspend fun getMoneyEntriesOnce(): List<MoneyEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(items: List<Store>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItems(items: List<ListItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItems(items: List<PantryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistories(items: List<PurchaseHistory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistoryItems(items: List<PurchaseHistoryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPfandEntries(items: List<PfandEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyEntries(items: List<MoneyEntry>)

    @Query("DELETE FROM stores")
    suspend fun clearStores()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

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

    @Query("SELECT * FROM pfand_entries WHERE isReturned = 0")
    suspend fun getPendingPfandOnce(): List<PfandEntry>

    /**
     * İadeyi tek adımda tamamlar: bekleyen tüm Pfand kayıtlarını iade alındı yapar ve
     * toplamı nakit bakiyesine yazar.
     *
     * Tek transaction olması bilinçli: işaretleme ile para girişi birbirinden koparsa
     * kullanıcı ya parayı kaybeder ya da aynı Pfand'ı iki kez nakde çevirir.
     *
     * @return nakde eklenen tutar (bekleyen yoksa 0).
     */
    @Transaction
    suspend fun cashInAllPfand(): Double {
        val pending = getPendingPfandOnce()
        val total = pending.sumOf { it.unitAmount * it.quantity }
        markAllPfandReturned()
        if (total > 0.0) {
            insertMoneyEntry(
                MoneyEntry(
                    account = MoneyAccount.CASH,
                    amount = total,
                    note = MoneyAccount.PFAND_CASH_IN_NOTE
                )
            )
        }
        return total
    }
}
