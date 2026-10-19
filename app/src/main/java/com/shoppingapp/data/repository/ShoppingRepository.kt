package com.shoppingapp.data.repository

import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class PurchaseLine(
    val listItemId: Long,
    val productId: Long,
    val quantity: Double,
    val unitPrice: Double,
    val pfandPerUnit: Double
)

data class PurchaseReceipt(
    val total: Double,
    val pfand: Double,
    val itemCount: Int,
    val storeName: String
)

data class ListItemWithDetails(
    val listItem: ListItem,
    val product: Product?,
    val store: Store?,
    val category: Category?
)

@Singleton
class ShoppingRepository @Inject constructor(
    private val shoppingDao: ShoppingDao
) {
    val allListItems: Flow<List<ListItem>> = shoppingDao.getAllListItems()
    val allProducts: Flow<List<Product>> = shoppingDao.getAllProducts()
    val allStores: Flow<List<Store>> = shoppingDao.getAllStores()
    val allCategories: Flow<List<Category>> = shoppingDao.getAllCategories()
    val pantryItems: Flow<List<PantryItem>> = shoppingDao.getPantryItems()
    val purchaseHistory: Flow<List<PurchaseHistory>> = shoppingDao.getPurchaseHistory()
    val pendingPfand: Flow<List<PfandEntry>> = shoppingDao.getPendingPfand()
    val returnedPfand: Flow<List<PfandEntry>> = shoppingDao.getReturnedPfand()

    private val defaultStores = listOf(
        Store(id = 1, name = "Rewe", colorHex = "#E2001A", aisleOrderJson = "[]"),
        Store(id = 2, name = "Edeka", colorHex = "#005CA9", aisleOrderJson = "[]"),
        Store(id = 3, name = "Aldi Süd", colorHex = "#002B49", aisleOrderJson = "[]"),
        Store(id = 4, name = "Lidl", colorHex = "#0050AA", aisleOrderJson = "[]"),
        Store(id = 5, name = "Kaufland", colorHex = "#D00000", aisleOrderJson = "[]"),
        Store(id = 6, name = "Penny", colorHex = "#E30613", aisleOrderJson = "[]"),
        Store(id = 7, name = "Netto", colorHex = "#F2B705", aisleOrderJson = "[]"),
        Store(id = 8, name = "dm", colorHex = "#0069B4", aisleOrderJson = "[]"),
        Store(id = 9, name = "Rossmann", colorHex = "#D40511", aisleOrderJson = "[]")
    )

    private val defaultCategories = listOf(
        Category(id = 1, nameDe = "Obst & Gemüse", nameTr = "Meyve & Sebze", iconName = "eco", colorHex = "#4CAF50"),
        Category(id = 2, nameDe = "Milchprodukte", nameTr = "Süt Ürünleri", iconName = "local_drink", colorHex = "#2196F3"),
        Category(id = 3, nameDe = "Getränke", nameTr = "İçecekler", iconName = "local_bar", colorHex = "#FF9800"),
        Category(id = 4, nameDe = "Snacks & Süßes", nameTr = "Atıştırmalık", iconName = "cookie", colorHex = "#9C27B0"),
        Category(id = 5, nameDe = "Haushalt", nameTr = "Temizlik & Ev", iconName = "cleaning_services", colorHex = "#607D8B")
    )

    /** Örnek veriler marka adlarıyla yazıldı: gerçek market etiketleri gibi görünsün. */
    private val sampleProducts = listOf(
        Product(id = 1, name = "MILSANI Joghurt 3,5% Fett", barcode = "4001234567890", categoryId = 2, unit = "Adet", defaultStoreId = 3, pfandAmount = 0.0, priceEuro = 0.79),
        Product(id = 2, name = "ja! H-Milch 1,5%", barcode = "4002345678901", categoryId = 2, unit = "L", defaultStoreId = 1, pfandAmount = 0.0, priceEuro = 1.09),
        Product(id = 3, name = "GOLDÄHREN Toast 500 g", barcode = "4003456789012", categoryId = 1, unit = "Packung", defaultStoreId = 3, pfandAmount = 0.0, priceEuro = 0.99),
        Product(id = 4, name = "Saskia Mineralwasser 6×1,5 l Kasten", barcode = "4004567890123", categoryId = 3, unit = "Kasten", defaultStoreId = 4, pfandAmount = 1.50, priceEuro = 3.49),
        Product(id = 5, name = "MILPRIMA Joghurt mild 3,5%", barcode = "4005678901234", categoryId = 2, unit = "Adet", defaultStoreId = 6, pfandAmount = 0.0, priceEuro = 0.65)
    )

    /**
     * Market ve kategori listesi: her açılışta garanti altına alınır ve
     * yeni marketler (Penny, Netto, dm, Rossmann) eski kurulumlara da eklenir.
     * Store kayıtları sabit id ile upsert edilir; mevcut liste satırları bozulmaz.
     */
    suspend fun seedConfig() {
        defaultStores.forEach { shoppingDao.insertStore(it) }

        val categories = shoppingDao.getAllCategories().first()
        if (categories.isEmpty()) {
            defaultCategories.forEach { shoppingDao.insertCategory(it) }
        }
    }

    suspend fun initDefaultData() {
        seedConfig()

        val products = shoppingDao.getAllProducts().first()
        if (products.isEmpty()) {
            sampleProducts.forEach { shoppingDao.insertProduct(it) }

            val sampleItems = listOf(
                ListItem(id = 1, productId = 1, storeId = 3, quantity = 2.0, isPurchased = false, addedAt = System.currentTimeMillis()),
                ListItem(id = 2, productId = 5, storeId = 6, quantity = 2.0, isPurchased = false, addedAt = System.currentTimeMillis()),
                ListItem(id = 3, productId = 4, storeId = 4, quantity = 1.0, isPurchased = false, addedAt = System.currentTimeMillis()),
                ListItem(id = 4, productId = 2, storeId = 1, quantity = 1.0, isPurchased = true, addedAt = System.currentTimeMillis())
            )
            sampleItems.forEach { shoppingDao.insertListItem(it) }

            shoppingDao.insertPurchaseHistory(
                PurchaseHistory(
                    id = 1,
                    storeId = 1,
                    totalAmount = 18.50,
                    pfandTotal = 3.00,
                    purchaseDate = System.currentTimeMillis() - 86400000
                )
            )
        }

        // Drop dead third-party photo hosts from older installs; the UI then
        // falls back to the bundled category photography.
        shoppingDao.getAllProducts().first().forEach { product ->
            val url = product.imageUrl ?: return@forEach
            if (url.contains("loremflickr") || url.contains("pollinations")) {
                shoppingDao.insertProduct(product.copy(imageUrl = null))
            }
        }
    }

    suspend fun addProduct(product: Product): Long = shoppingDao.insertProduct(product)

    suspend fun addListItem(productId: Long, storeId: Long, quantity: Double): Long {
        return shoppingDao.insertListItem(
            ListItem(
                productId = productId,
                storeId = storeId,
                quantity = quantity,
                isPurchased = false,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Barkodla bulunan ürünü, kullanıcının seçtiği marketle alınacaklar listesine ekler.
     * @param storeId 0 → market belirtilmedi.
     */
    suspend fun addScannedToList(product: Product, storeId: Long) {
        val productId = if (product.id > 0L) {
            product.id
        } else {
            shoppingDao.insertProduct(product)
        }
        shoppingDao.insertListItem(
            ListItem(
                productId = productId,
                storeId = storeId,
                quantity = 1.0,
                isPurchased = false,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateListItem(item: ListItem) = shoppingDao.updateListItem(item)

    suspend fun deleteListItem(item: ListItem) = shoppingDao.deleteListItem(item)

    suspend fun clearPurchasedItems() = shoppingDao.clearPurchasedItems()

    /**
     * Alışveriş tarafını sıfırlar: liste, alışveriş geçmişi ve kayıtlı ürünler
     * silinir; market/kategori ayarları korunur.
     */
    suspend fun resetShoppingData() {
        shoppingDao.clearAllListItems()
        shoppingDao.clearPurchaseHistoryItems()
        shoppingDao.clearPurchaseHistory()
        shoppingDao.deleteAllProducts()
        seedConfig()
    }

    /** Pfand defterini (bekleyen + iade edilmiş) tamamen sıfırlar. */
    suspend fun resetPfandData() = shoppingDao.clearAllPfandEntries()

    /**
     * Writes the basket to the purchase history, remembers the entered unit prices
     * for next time, then empties the list.
     */
    suspend fun completePurchase(lines: List<PurchaseLine>, storeId: Long): PurchaseReceipt {
        val total = lines.sumOf { it.unitPrice * it.quantity }
        val pfand = lines.sumOf { it.pfandPerUnit * it.quantity }
        val historyId = shoppingDao.insertPurchaseHistory(
            PurchaseHistory(
                storeId = storeId,
                totalAmount = total,
                pfandTotal = pfand,
                purchaseDate = System.currentTimeMillis()
            )
        )

        lines.forEach { line ->
            shoppingDao.insertPurchaseHistoryItem(
                PurchaseHistoryItem(
                    historyId = historyId,
                    productId = line.productId,
                    priceEuro = line.unitPrice,
                    quantity = line.quantity
                )
            )
            if (line.unitPrice > 0.0) {
                shoppingDao.updateProductPrice(line.productId, line.unitPrice)
            }
        }

        shoppingDao.clearAllListItems()

        val storeName = shoppingDao.getAllStores().first()
            .firstOrNull { it.id == storeId }?.name
            ?: "Market"

        return PurchaseReceipt(
            total = total,
            pfand = pfand,
            itemCount = lines.sumOf { it.quantity }.toInt(),
            storeName = storeName
        )
    }

    // ---- Pfand ----

    suspend fun addPfandEntry(label: String, unitAmount: Double, quantity: Int) {
        if (quantity <= 0 || unitAmount < 0) return
        shoppingDao.insertPfandEntry(
            PfandEntry(label = label, unitAmount = unitAmount, quantity = quantity)
        )
    }

    suspend fun deletePfandEntry(entry: PfandEntry) = shoppingDao.deletePfandEntry(entry)

    suspend fun returnAllPfand() = shoppingDao.markAllPfandReturned()

    suspend fun clearReturnedPfand() = shoppingDao.clearReturnedPfand()
}
