package com.shoppingapp.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.shoppingapp.data.dao.FlyerListDao
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.local.ShoppingDatabase
import com.shoppingapp.data.model.Category
import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.model.ListItem
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.data.model.PantryItem
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.data.model.Product
import com.shoppingapp.data.model.PurchaseHistory
import com.shoppingapp.data.model.PurchaseHistoryItem
import com.shoppingapp.data.model.Store
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulamanın tüm verisinin tek bir JSON dosyasına yedeği.
 *
 * Veri tamamen cihazda tutulduğu için telefon değişimi veya uygulamanın silinmesi
 * her şeyi siler. Bu yüzden iki katman var: kullanıcının seçtiği yere (Storage
 * Access Framework) dışa aktarma ve her gün kendiliğinden alınan yerel yedek.
 *
 * Broşürden eklenen fırsat ürünlerinin **görselleri** yedeğe girmez; dosya yolu
 * taşınır ve görsel yoksa arayüz harf yer tutucusuna düşer.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ShoppingDatabase,
    private val shoppingDao: ShoppingDao,
    private val flyerListDao: FlyerListDao
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val backupDir: File
        get() = File(context.filesDir, BACKUP_DIR).apply { if (!exists()) mkdirs() }

    // ---- Dışa aktarma ----

    suspend fun snapshot(): BackupPayload = withContext(Dispatchers.IO) {
        BackupPayload(
            createdAt = System.currentTimeMillis(),
            products = shoppingDao.getProductsOnce(),
            stores = shoppingDao.getStoresOnce(),
            categories = shoppingDao.getCategoriesOnce(),
            listItems = shoppingDao.getAllListItemsOnce(),
            pantryItems = shoppingDao.getPantryItemsOnce(),
            purchaseHistory = shoppingDao.getPurchaseHistoryOnce(),
            purchaseHistoryItems = shoppingDao.getPurchaseHistoryItemsOnce(),
            pfandEntries = shoppingDao.getPfandEntriesOnce(),
            moneyEntries = shoppingDao.getMoneyEntriesOnce(),
            flyerListItems = flyerListDao.getAll()
        )
    }

    suspend fun exportTo(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = snapshot()
            val json = gson.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Seçilen dosya yazılamadı.")
            payload.summary()
        }
    }

    /** Paylaşım/önizleme için okunabilir JSON metni. */
    suspend fun exportJson(): String = gson.toJson(snapshot())

    // ---- İçe aktarma ----

    suspend fun importFrom(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: error("Seçilen dosya okunamadı.")
            restoreJson(json)
        }
    }

    suspend fun restoreLocal(file: BackupFile): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching { restoreJson(File(file.path).readText()) }
    }

    private suspend fun restoreJson(json: String): BackupSummary {
        val payload = try {
            gson.fromJson(json, BackupPayload::class.java)
        } catch (error: JsonSyntaxException) {
            throw IllegalArgumentException("Dosya geçerli bir Einkauf yedeği değil.", error)
        } ?: throw IllegalArgumentException("Dosya boş.")

        if (payload.products.isEmpty() && payload.stores.isEmpty()) {
            throw IllegalArgumentException("Yedekte ürün veya market verisi yok.")
        }

        // Geri yükleme tek transaction: yarıda kalan bir aktarım veriyi karışık bırakmaz.
        database.withTransaction {
            flyerListDao.clear()
            shoppingDao.clearMoneyEntries()
            shoppingDao.clearAllPfandEntries()
            shoppingDao.clearPurchaseHistoryItems()
            shoppingDao.clearPurchaseHistory()
            shoppingDao.clearPantryItems()
            shoppingDao.clearAllListItems()
            shoppingDao.deleteAllProducts()
            shoppingDao.clearStores()
            shoppingDao.clearCategories()

            shoppingDao.insertStores(payload.stores)
            shoppingDao.insertCategories(payload.categories)
            shoppingDao.insertProducts(payload.products)
            shoppingDao.insertListItems(payload.listItems)
            shoppingDao.insertPantryItems(payload.pantryItems)
            shoppingDao.insertPurchaseHistories(payload.purchaseHistory)
            shoppingDao.insertPurchaseHistoryItems(payload.purchaseHistoryItems)
            shoppingDao.insertPfandEntries(payload.pfandEntries)
            shoppingDao.insertMoneyEntries(payload.moneyEntries)
            if (payload.flyerListItems.isNotEmpty()) {
                payload.flyerListItems.forEach { flyerListDao.insert(it) }
            }
        }
        return payload.summary()
    }

    // ---- Günlük otomatik yedek ----

    /**
     * Günde bir kez yerel yedek alır. Son yedek [AUTO_BACKUP_INTERVAL_MS] içindeyse
     * hiçbir şey yapmaz; uygulama her açılışta çağırabilir.
     *
     * @return yeni dosya oluşturulduysa dosya bilgisi, aksi halde null.
     */
    suspend fun autoBackupIfNeeded(force: Boolean = false): BackupFile? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val last = localBackups().firstOrNull { it.isAuto }
        if (!force && last != null && now - last.createdAt < AUTO_BACKUP_INTERVAL_MS) return@withContext null

        val payload = snapshot()
        if (payload.products.isEmpty() && payload.stores.isEmpty()) return@withContext null

        val stamp = TIMESTAMP.format(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()))
        val target = File(backupDir, "auto-$stamp.json")
        val temp = File(backupDir, "auto-$stamp.tmp")
        temp.writeText(gson.toJson(payload))
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Yedek dosyası oluşturulamadı.")
        }
        pruneAutoBackups()
        BackupFile(
            path = target.absolutePath,
            name = target.name,
            createdAt = target.lastModified(),
            sizeBytes = target.length(),
            isAuto = true
        )
    }

    fun localBackups(): List<BackupFile> {
        val files = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?: return emptyList()
        return files
            .map { file ->
                BackupFile(
                    path = file.absolutePath,
                    name = file.name,
                    createdAt = file.lastModified(),
                    sizeBytes = file.length(),
                    isAuto = file.name.startsWith("auto-")
                )
            }
            .sortedByDescending { it.createdAt }
    }

    fun deleteLocal(file: BackupFile): Boolean = File(file.path).delete()

    private fun pruneAutoBackups() {
        backupDir.listFiles { file -> file.isFile && file.name.startsWith("auto-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_AUTO_BACKUPS)
            ?.forEach { it.delete() }
    }

    private fun BackupPayload.summary() = BackupSummary(
        createdAt = createdAt,
        products = products.size,
        listItems = listItems.size,
        pantryItems = pantryItems.size,
        purchases = purchaseHistory.size,
        pfandEntries = pfandEntries.size,
        moneyEntries = moneyEntries.size,
        flyerItems = flyerListItems.size
    )

    private companion object {
        const val BACKUP_DIR = "backups"
        const val MAX_AUTO_BACKUPS = 5
        const val AUTO_BACKUP_INTERVAL_MS = 20L * 60L * 60L * 1000L
        val TIMESTAMP: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.GERMANY)
    }
}

/** Yedek dosyasındaki tüm tablolar. Boş tablolar da yazılır ki geri yükleme tam olsun. */
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersion: String = "2.0",
    val createdAt: Long = System.currentTimeMillis(),
    val products: List<Product> = emptyList(),
    val stores: List<Store> = emptyList(),
    val categories: List<Category> = emptyList(),
    val listItems: List<ListItem> = emptyList(),
    val pantryItems: List<PantryItem> = emptyList(),
    val purchaseHistory: List<PurchaseHistory> = emptyList(),
    val purchaseHistoryItems: List<PurchaseHistoryItem> = emptyList(),
    val pfandEntries: List<PfandEntry> = emptyList(),
    val moneyEntries: List<MoneyEntry> = emptyList(),
    val flyerListItems: List<FlyerListItem> = emptyList()
)

data class BackupSummary(
    val createdAt: Long,
    val products: Int,
    val listItems: Int,
    val pantryItems: Int,
    val purchases: Int,
    val pfandEntries: Int,
    val moneyEntries: Int,
    val flyerItems: Int
) {
    val headline: String
        get() = "$listItems liste · $pantryItems kiler · $purchases alışveriş · " +
            "$flyerItems fırsat · $moneyEntries cebim hareketi"
}

data class BackupFile(
    val path: String,
    val name: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val isAuto: Boolean
)
