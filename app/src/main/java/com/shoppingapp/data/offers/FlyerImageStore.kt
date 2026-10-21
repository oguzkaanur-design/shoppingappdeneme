package com.shoppingapp.data.offers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Ürün fotoğrafı ayrı bir görsel servisinde yayınlanmadığı için ürünün broşür
 * sayfasındaki alanı kırpılıp küçük bir JPEG olarak saklanır. Böylece listede
 * ürünün gerçek görseli görünür ve internet olmadan da açılır.
 */
@Singleton
class FlyerImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {

    suspend fun saveCrop(leafletId: Long, hotspot: FlyerHotspot): String? = withContext(Dispatchers.IO) {
        val bytes = downloadPage(leafletId, hotspot.pageIndex) ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val pixels = FlyerCrop.rectFor(hotspot, bounds.outWidth, bounds.outHeight)
        val bitmap = crop(bytes, Rect(pixels.left, pixels.top, pixels.right, pixels.bottom))
            ?: return@withContext null

        val directory = File(context.filesDir, "flyer-crops").apply { mkdirs() }
        val file = File(directory, "flyer-${UUID.randomUUID()}.jpg")
        return@withContext runCatching {
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            bitmap.recycle()
            file.absolutePath
        }.getOrElse {
            bitmap.recycle()
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun crop(bytes: ByteArray, rect: Rect): Bitmap? = runCatching {
        val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
            ?: return null
        try {
            decoder.decodeRegion(rect, BitmapFactory.Options())
        } finally {
            decoder.recycle()
        }
    }.getOrNull()

    private fun downloadPage(leafletId: Long, pageIndex: Int): ByteArray? {
        val request = Request.Builder()
            .url(FlyerPageUrls.page(leafletId, pageIndex, size = "large"))
            .header("User-Agent", USER_AGENT)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Sayfa görseli alınamadı.")
                response.body?.bytes()
            }
        }.getOrNull()
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}

internal data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Normalize edilmiş hotspot'u piksel dikdörtgenine çevirir; saf ve test edilebilir. */
internal object FlyerCrop {

    fun rectFor(spot: FlyerHotspot, imageWidth: Int, imageHeight: Int): PixelRect {
        val left = (spot.fromX * imageWidth).roundToInt().coerceIn(0, imageWidth - 1)
        val top = (spot.fromY * imageHeight).roundToInt().coerceIn(0, imageHeight - 1)
        val right = (spot.toX * imageWidth).roundToInt().coerceIn(left + 1, imageWidth)
        val bottom = (spot.toY * imageHeight).roundToInt().coerceIn(top + 1, imageHeight)
        return PixelRect(left, top, right, bottom)
    }
}
