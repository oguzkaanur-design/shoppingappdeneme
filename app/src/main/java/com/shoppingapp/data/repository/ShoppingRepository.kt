package com.shoppingapp.data.repository

import com.shoppingapp.data.catalog.BrandCatalog
import com.shoppingapp.data.catalog.ProductAvatars
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.local.SeedState
import com.shoppingapp.data.model.*
import com.shoppingapp.feature.receipt.ReceiptLine
import com.shoppingapp.feature.share.ImportedLine
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
    private val shoppingDao: ShoppingDao,
    private val seedState: SeedState
) {
    val allListItems: Flow<List<ListItem>> = shoppingDao.getAllListItems()
    val allProducts: Flow<List<Product>> = shoppingDao.getAllProducts()
    val allStores: Flow<List<Store>> = shoppingDao.getAllStores()
    val allCategories: Flow<List<Category>> = shoppingDao.getAllCategories()
    val pantryItems: Flow<List<PantryItem>> = shoppingDao.getPantryItems()
    val purchaseHistory: Flow<List<PurchaseHistory>> = shoppingDao.getPurchaseHistory()
    val pendingPfand: Flow<List<PfandEntry>> = shoppingDao.getPendingPfand()
    val returnedPfand: Flow<List<PfandEntry>> = shoppingDao.getReturnedPfand()
    val moneyEntries: Flow<List<MoneyEntry>> = shoppingDao.getMoneyEntries()
    val pantryRows: Flow<List<PantryRow>> = shoppingDao.getPantryRows()
    val historyLines: Flow<List<PurchaseHistoryLine>> = shoppingDao.getHistoryLines()
    val frequentProducts: Flow<List<FrequentProductRow>> = shoppingDao.getFrequentProducts(12)

    /**
     * Reyon sırası kategori kimlikleriyle tutulur. Sıra markete göre gerçek
     * gezinme düzenini yansıtır: manav/giriş, süt ürünleri, atıştırmalık,
     * içecekler ve en arkadaki temizlik reyonu her markette aynı yerde değildir.
     */
    private val defaultStores = listOf(
        Store(id = 1, name = "Rewe", colorHex = "#E2001A", aisleOrderJson = "[1,2,4,3,5]"),
        Store(id = 2, name = "Edeka", colorHex = "#005CA9", aisleOrderJson = "[1,4,2,3,5]"),
        Store(id = 3, name = "Aldi Süd", colorHex = "#002B49", aisleOrderJson = "[1,5,2,4,3]"),
        Store(id = 4, name = "Lidl", colorHex = "#0050AA", aisleOrderJson = "[1,2,3,5,4]"),
        Store(id = 5, name = "Kaufland", colorHex = "#D00000", aisleOrderJson = "[1,2,3,4,5]"),
        Store(id = 6, name = "Penny", colorHex = "#E30613", aisleOrderJson = "[1,4,3,2,5]"),
        Store(id = 7, name = "Netto", colorHex = "#F2B705", aisleOrderJson = "[1,2,3,5,4]"),
        Store(id = 8, name = "dm", colorHex = "#0069B4", aisleOrderJson = "[5,4,2,1,3]"),
        Store(id = 9, name = "Rossmann", colorHex = "#D40511", aisleOrderJson = "[5,4,2,3,1]")
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

        // Örnek veri yalnızca ilk kurulumda. "Tüm verileri sil" sonrası tekrar
        // eklenmesin; aksi halde silme kalıcı olmaz.
        if (seedState.isSeeded) return

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

        // Mevcut kurulumlarda ürünler dolu olabilir; yine de bayrağı kapat ki
        // ileride veri silinirse demo liste geri gelmesin.
        seedState.isSeeded = true
    }

    suspend fun addProduct(product: Product): Long = shoppingDao.insertProduct(product)

    /** Listede avatarı değişen ürün; anahtar null ise otomatik tahmine dönülür. */
    suspend fun setProductAvatar(productId: Long, avatarKey: String?) =
        shoppingDao.setProductAvatar(productId, avatarKey)

    /** Ad → kategori: önce avatar kataloğu (daha geniş), sonra marka kataloğu. */
    private fun detectCategory(name: String): Long =
        ProductAvatars.categoryFor(name) ?: BrandCatalog.detectCategory(name) ?: 0L

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

    /**
     * Paylaşılan metinden çıkarılan satırları listeye ekler. Aynı adla kayıtlı ürün
     * yeniden oluşturulmaz; ürün yoksa adından kategori tahmin edilerek açılır.
     * @return listeye eklenen satır sayısı.
     */
    suspend fun importLines(lines: List<ImportedLine>): Int {
        var added = 0
        lines.forEach { line ->
            val name = line.name.trim()
            if (name.isBlank()) return@forEach
            val productId = shoppingDao.findProductByName(name)?.id
                ?: shoppingDao.insertProduct(
                    Product(
                        name = name,
                        barcode = "IMPORT_${System.currentTimeMillis()}_$added",
                        categoryId = detectCategory(name),
                        unit = line.unit,
                        priceEuro = 0.0
                    )
                )
            shoppingDao.insertListItem(
                ListItem(
                    productId = productId,
                    storeId = 0L,
                    quantity = line.quantity.coerceAtLeast(0.1),
                    isPurchased = false,
                    addedAt = System.currentTimeMillis() + added
                )
            )
            added++
        }
        return added
    }

    /** Barkod okutulan ürün için en son ödenen fiyatı getirir (yoksa null). */
    suspend fun lastPriceFor(productId: Long): LastPriceRow? =
        shoppingDao.getLastPriceForProduct(productId)

    suspend fun updateListItem(item: ListItem) = shoppingDao.updateListItem(item)

    suspend fun deleteListItem(item: ListItem) = shoppingDao.deleteListItem(item)

    // ---- Kiler (Pantry) ----

    /**
     * Kilerde yeni bir ürün takibi başlatır. Aynı adla kayıtlı bir ürün varsa
     * yeniden ürün açılmaz; stok o ürünün kaydına eklenir.
     */
    suspend fun addPantryItem(
        name: String,
        quantity: Double,
        minThreshold: Double,
        expiryDate: Long?,
        unit: String = "Adet",
        storeId: Long = 0L,
        categoryId: Long = 0L,
        priceEuro: Double = 0.0
    ): Long? {
        val cleanName = name.trim()
        if (cleanName.isBlank() || quantity <= 0.0) return null

        val productId = shoppingDao.findProductByName(cleanName)?.id
            ?: Product(
                name = cleanName,
                barcode = "PANTRY_${System.currentTimeMillis()}",
                categoryId = categoryId.takeIf { it > 0L }
                    ?: detectCategory(cleanName),
                unit = unit.ifBlank { "Adet" },
                defaultStoreId = storeId.takeIf { it > 0L },
                priceEuro = priceEuro.coerceAtLeast(0.0)
            ).let { shoppingDao.insertProduct(it) }

        return addPantryItemForProduct(
            productId = productId,
            quantity = quantity,
            minThreshold = minThreshold,
            expiryDate = expiryDate
        )
    }

    /** Var olan bir ürün için kilerde stok tutar; kayıt varsa adet eklenir. */
    suspend fun addPantryItemForProduct(
        productId: Long,
        quantity: Double,
        minThreshold: Double,
        expiryDate: Long? = null
    ): Long {
        val existing = shoppingDao.getPantryItemForProduct(productId)
        return if (existing != null) {
            shoppingDao.insertPantryItem(
                existing.copy(
                    currentQuantity = existing.currentQuantity + quantity.coerceAtLeast(0.0),
                    minThreshold = minThreshold.coerceAtLeast(0.0),
                    expiryDate = expiryDate ?: existing.expiryDate
                )
            )
        } else {
            shoppingDao.insertPantryItem(
                PantryItem(
                    productId = productId,
                    currentQuantity = quantity.coerceAtLeast(0.0),
                    minThreshold = minThreshold.coerceAtLeast(0.0),
                    expiryDate = expiryDate
                )
            )
        }
    }

    suspend fun setPantryQuantity(id: Long, quantity: Double) =
        shoppingDao.setPantryQuantity(id, quantity.coerceAtLeast(0.0))

    suspend fun setPantryThreshold(item: PantryItem, threshold: Double) =
        shoppingDao.insertPantryItem(item.copy(minThreshold = threshold.coerceAtLeast(0.0)))

    suspend fun setPantryExpiry(item: PantryItem, expiryDate: Long?) =
        shoppingDao.insertPantryItem(item.copy(expiryDate = expiryDate))

    suspend fun deletePantryItem(item: PantryItem) = shoppingDao.deletePantryItem(item)

    /** Kilerdeki eksik ürünü tek dokunuşla alışveriş listesine ekler. */
    suspend fun addPantryRowToList(row: PantryRow, quantity: Double = 1.0) {
        shoppingDao.insertListItem(
            ListItem(
                productId = row.productId,
                storeId = row.defaultStoreId ?: 0L,
                quantity = quantity.coerceAtLeast(1.0),
                isPurchased = false,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    // ---- Hızlı ekleme ----

    /** "Sık alınanlar" satırından listeye ekler. */
    suspend fun addFrequentToList(row: FrequentProductRow, quantity: Double = 1.0) {
        shoppingDao.insertListItem(
            ListItem(
                productId = row.productId,
                storeId = row.defaultStoreId ?: 0L,
                quantity = quantity.coerceAtLeast(1.0),
                isPurchased = false,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Son tamamlanan alışverişteki ürünleri aynı adetlerle listeye geri koyar.
     * @return listeye eklenen satır sayısı.
     */
    suspend fun repeatLastPurchase(): Int {
        val last = shoppingDao.getLastPurchaseOnce() ?: return 0
        var added = 0
        shoppingDao.getHistoryItemsOnce(last.id).forEach { line ->
            val product = shoppingDao.getProductById(line.productId) ?: return@forEach
            shoppingDao.insertListItem(
                ListItem(
                    productId = product.id,
                    storeId = product.defaultStoreId ?: last.storeId,
                    quantity = line.quantity.coerceAtLeast(1.0),
                    isPurchased = false,
                    addedAt = System.currentTimeMillis()
                )
            )
            added++
        }
        return added
    }

    /** Son alışveriş varsa "tekrarla" kısayolu gösterilir. */
    suspend fun hasPurchaseHistory(): Boolean = shoppingDao.getLastPurchaseOnce() != null

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
            // Kilerde takip edilen ürünün stoğu, satın alınan adet kadar artar.
            shoppingDao.increasePantryQuantity(line.productId, line.quantity)
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

    /**
     * Fiş fotoğrafından okunan satırları tek alışveriş olarak kaydeder. Fişteki ürün
     * adı daha önce kayıtlıysa o ürün kullanılır; yoksa adından kategori tahmin edilerek
     * yeni ürün açılır. Liste, normal "alışverişi tamamla" akışındaki gibi boşaltılır.
     */
    suspend fun completePurchaseFromReceipt(
        lines: List<ReceiptLine>,
        storeId: Long
    ): PurchaseReceipt {
        val purchaseLines = lines.mapIndexed { index, line ->
            val productId = shoppingDao.findProductByName(line.name)?.id
                ?: shoppingDao.insertProduct(
                    Product(
                        name = line.name,
                        barcode = "RECEIPT_${System.currentTimeMillis()}_$index",
                        categoryId = detectCategory(line.name),
                        unit = "Adet",
                        defaultStoreId = storeId.takeIf { it > 0L },
                        priceEuro = line.unitPrice
                    )
                )
            PurchaseLine(
                listItemId = 0L,
                productId = productId,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                pfandPerUnit = 0.0
            )
        }
        return completePurchase(purchaseLines, storeId)
    }

    // ---- Pfand ----

    suspend fun addPfandEntry(label: String, unitAmount: Double, quantity: Int) {
        if (quantity <= 0 || unitAmount < 0) return
        shoppingDao.insertPfandEntry(
            PfandEntry(label = label, unitAmount = unitAmount, quantity = quantity)
        )
    }

    suspend fun deletePfandEntry(entry: PfandEntry) = shoppingDao.deletePfandEntry(entry)

    /**
     * Kasadan alınan tutarı iade eder; yalnızca bu tutara denk gelen adetleri düşer.
     * Sonuç, arayüzün canlı hesaplamasıyla aynı planlayıcıyı kullanır.
     */
    suspend fun cashInPfand(amountCents: Int) = shoppingDao.cashInPfand(amountCents)

    /**
     * Bekleyen tüm Pfand'ı iade alındı yapar ve toplam tutarı nakit bakiyesine aktarır.
     * @return Cebim'e eklenen tutar.
     */
    suspend fun cashInAllPfand(): Double = shoppingDao.cashInAllPfand()

    suspend fun clearReturnedPfand() = shoppingDao.clearReturnedPfand()

    // ---- Cebim (nakit / banka) ----

    /**
     * Bakiyeye tek bir hareket ekler. [amount] işaretlidir: + para girdi, − harcandı.
     * Geçersiz hesap veya sıfır tutar sessizce yok sayılır.
     */
    suspend fun addMoneyEntry(account: String, amount: Double, note: String = ""): Long? {
        if (account != MoneyAccount.CASH && account != MoneyAccount.BANK) return null
        if (amount == 0.0 || amount.isNaN() || amount.isInfinite()) return null
        return shoppingDao.insertMoneyEntry(
            MoneyEntry(account = account, amount = amount, note = note.trim())
        )
    }

    suspend fun deleteMoneyEntry(entry: MoneyEntry) = shoppingDao.deleteMoneyEntry(entry)

    suspend fun resetMoneyData() = shoppingDao.clearMoneyEntries()
}
