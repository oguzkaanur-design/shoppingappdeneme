package com.shoppingapp.data.offers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Broşür sayfalarındaki tıklanabilir ürünleri getirir. Her sayfa için yalnızca o
 * sayfanın teklifleri istenir, böylece büyük broşürlerde (70+ sayfa) tek seferde
 * yüzlerce kayıt indirilmez.
 */
@Singleton
class FlyerOfferRepository @Inject constructor(
    private val client: OkHttpClient,
    private val configProvider: MarktguruConfigProvider
) {

    private val mutex = Mutex()
    private val hotspotsByLeaflet = mutableMapOf<Long, List<FlyerHotspot>>()
    private val offersByPage = mutableMapOf<Pair<Long, Int>, List<FlyerOffer>>()

    /** Aynı sayfa için birden fazla istek gitmesin diye sonuçlar bellekte tutulur. */
    suspend fun offersForPage(leafletId: Long, pageIndex: Int): List<FlyerOffer> {
        offersByPage[leafletId to pageIndex]?.let { return it }

        val config = configProvider.config()
        val offersJson = getJson(config, "offers?as=mobiledetailed&leafletId=$leafletId&pageIndex=$pageIndex")
        val offers = FlyerOfferParser.parsePageOffers(offersJson, pageIndex)
            .map { it.copy(leafletId = leafletId) }
        if (offers.isEmpty()) {
            offersByPage[leafletId to pageIndex] = emptyList()
            return emptyList()
        }

        val hotspots = hotspots(leafletId, config)
        val joined = FlyerOfferParser.attachHotspots(offers, hotspots)

        mutex.withLock { offersByPage[leafletId to pageIndex] = joined }
        Log.d(TAG, "leaflet $leafletId page $pageIndex: ${joined.size} offers, ${joined.count { it.hotspot != null }} hotspots")
        return joined
    }

    private suspend fun hotspots(leafletId: Long, config: MarktguruConfig): List<FlyerHotspot> {
        hotspotsByLeaflet[leafletId]?.let { return it }

        return mutex.withLock {
            hotspotsByLeaflet[leafletId] ?: run {
                val json = getJson(config, "leaflets/$leafletId?as=mobiledetailed")
                FlyerOfferParser.parseHotspots(json).also { hotspotsByLeaflet[leafletId] = it }
            }
        }
    }

    /**
     * Çağıran Main dispatcher'da olabilir (Compose LaunchedEffect); ağ isteği her
     * durumda IO'da çalışmalı, yoksa Android NetworkOnMainThreadException atar.
     */
    private suspend fun getJson(config: MarktguruConfig, path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://${config.apiHost}/api/v1/$path")
            .header("x-apikey", config.apiKey)
            .header("x-clientkey", config.clientKey)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Accept-Language", "de-DE,de;q=0.9")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Teklifler alınamadı (${response.code}).")
            }
            response.body?.string().orEmpty()
        }
    }

    private companion object {
        const val TAG = "FlyerOfferRepository"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
