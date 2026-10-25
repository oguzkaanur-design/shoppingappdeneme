package com.shoppingapp.data.offers

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Son başarılı broşür listesinin cihazdaki kopyası.
 *
 * Broşür kataloğu dış bir servisten (marktguru) geliyor; servis yavaşladığında,
 * engellendiğinde veya internet yokken ekranın boş kalması yerine son bilinen
 * liste gösterilir. Görseller Coil önbelleğinden gelir; önbellekte yoksa kart
 * yer tutucuyla çizilir.
 */
@Singleton
class FlyerCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val gson = Gson()

    private val cacheDir: File
        get() = File(context.filesDir, "flyer-cache").apply { if (!exists()) mkdirs() }

    fun read(storeId: String): List<Flyer>? {
        val file = File(cacheDir, "$storeId.json")
        if (!file.exists()) return null
        val cached = runCatching {
            gson.fromJson(file.readText(), CachedCatalog::class.java)
        }.getOrNull() ?: return null
        if (cached.flyers.isEmpty()) return null
        return cached.flyers.map { it.toFlyer() }
    }

    fun write(storeId: String, flyers: List<Flyer>) {
        if (flyers.isEmpty()) return
        val cached = CachedCatalog(
            savedAt = System.currentTimeMillis(),
            flyers = flyers.map { it.toCached() }
        )
        runCatching {
            File(cacheDir, "$storeId.json").writeText(gson.toJson(cached))
        }
    }

    fun savedAt(storeId: String): Long? {
        val file = File(cacheDir, "$storeId.json")
        if (!file.exists()) return null
        return runCatching {
            gson.fromJson(file.readText(), CachedCatalog::class.java)?.savedAt
        }.getOrNull()
    }

    private data class CachedCatalog(
        val savedAt: Long = 0,
        val flyers: List<CachedFlyer> = emptyList()
    )

    private data class CachedFlyer(
        val storeId: String = "",
        val storeName: String = "",
        val leafletId: Long = 0,
        val pageCount: Int = 0,
        val validFrom: Long = 0,
        val validTo: Long = 0,
        val offerCount: Int = 0
    ) {
        fun toFlyer() = Flyer(
            storeId = storeId,
            storeName = storeName,
            leafletId = leafletId,
            pageCount = pageCount,
            validFrom = Instant.ofEpochMilli(validFrom),
            validTo = Instant.ofEpochMilli(validTo),
            offerCount = offerCount
        )
    }

    private fun Flyer.toCached() = CachedFlyer(
        storeId = storeId,
        storeName = storeName,
        leafletId = leafletId,
        pageCount = pageCount,
        validFrom = validFrom.toEpochMilli(),
        validTo = validTo.toEpochMilli(),
        offerCount = offerCount
    )
}
