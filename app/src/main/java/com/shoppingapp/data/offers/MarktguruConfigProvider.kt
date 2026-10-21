package com.shoppingapp.data.offers

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
 * Teklif API'sinin istemci anahtarlarını katalog sitesinin kendi sayfasından okur ve
 * bellekte tutar. Anahtar yoksa uygulama yalnızca broşürü gösterir, ürün tıklama
 * özelliği sessizce devre dışı kalır.
 */
@Singleton
class MarktguruConfigProvider @Inject constructor(
    private val client: OkHttpClient
) {

    private val mutex = Mutex()
    @Volatile
    private var cached: MarktguruConfig? = null

    suspend fun config(): MarktguruConfig = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { fetch() }.also { cached = it }
    }

    private fun fetch(): MarktguruConfig {
        val request = Request.Builder()
            .url("https://www.marktguru.de/")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "de-DE,de;q=0.9")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Prospektüs yapılandırması alınamadı (${response.code}).")
            }
            val html = response.body?.string().orEmpty()
            return MarktguruConfigParser.parse(html)
                ?: throw IOException("Prospektüs yapılandırması okunamadı.")
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
