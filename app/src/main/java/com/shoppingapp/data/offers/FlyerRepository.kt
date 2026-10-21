package com.shoppingapp.data.offers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Market prospektüs listesini getirir. Mağaza siteleri uygulama içi WebView'de
 * boş kaldığı için broşür listesi, broşürleri sayfa görseli olarak yayınlayan
 * herkese açık prospektüs kataloğundan okunur; sayfa görselleri doğrudan CDN'den
 * yüklenir. Böylece uygulama içinde gerçek broşür sayfaları gösterilir.
 *
 * Katalog aynı adrese yapılan isteklerde farklı bölgesel sürümler döndürebiliyor
 * (bazen yalnızca birkaç sayfalık bir el ilanı). Tam boy bir broşür gelene kadar
 * birkaç deneme yapılır ve gelen broşürler birleştirilir.
 */
@Singleton
class FlyerRepository @Inject constructor(
    private val client: OkHttpClient
) {

    /** @throws IOException ağ veya ayrıştırma hatasında, kullanıcıya gösterilebilir mesajla. */
    suspend fun flyers(store: FlyerStore): List<Flyer> = withContext(Dispatchers.IO) {
        var collected = fetch(store)
        var attempts = 1

        while (attempts < MAX_ATTEMPTS && !hasFullWeekFlyer(collected)) {
            val extra = runCatching { fetch(store) }.getOrNull() ?: break
            collected = FlyerCatalogParser.order(collected + extra, Instant.now())
            attempts++
        }

        collected
    }

    private fun fetch(store: FlyerStore): List<Flyer> {
        val request = Request.Builder()
            .url(FlyerCatalogUrl.forDay(store))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "de-DE,de;q=0.9,tr;q=0.8")
            // Ara sunucu önbelleği geçen haftanın broşürlerini döndürmesin.
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Prospektüs sunucusu ${response.code} yanıtı verdi.")
            }
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) {
                throw IOException("Prospektüs sayfası boş döndü.")
            }

            val flyers = FlyerCatalogParser.parse(html, store, Instant.now())
            Log.d(
                TAG,
                "${store.id}: ${response.code} ${html.length} chars, " +
                    "leaflets=${flyers.joinToString { "${it.leafletId}/${it.pageCount}p/${it.phase()}" }}"
            )
            return flyers.ifEmpty {
                throw IOException("${store.name} için güncel prospektüs bulunamadı.")
            }
        }
    }

    private fun hasFullWeekFlyer(flyers: List<Flyer>): Boolean =
        flyers.any { it.phase() == FlyerPhase.CURRENT && it.pageCount >= FULL_FLYER_PAGES }

    private companion object {
        const val TAG = "FlyerRepository"
        const val MAX_ATTEMPTS = 3
        const val FULL_FLYER_PAGES = 12
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
