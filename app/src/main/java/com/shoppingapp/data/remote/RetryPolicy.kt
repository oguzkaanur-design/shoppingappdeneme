package com.shoppingapp.data.remote

import java.io.IOException
import kotlin.random.Random

/**
 * Yalnızca tekrar denemesi güvenli olan istekler için sınırlı üstel geri çekilme.
 *
 * Kural: GET + ağ hatası veya 408/429/5xx. En fazla 3 deneme (ilk istek + 2 tekrar),
 * bekleme 400 ms'den başlar, her turda ikiye katlanır ve 4 saniyede kesilir.
 * Sunucu `Retry-After` verdiyse o süre esas alınır (yine 4 saniyeyle sınırlı).
 *
 * Saf nesne: karar ve gecikme hesapları birim testlerle doğrulanır.
 */
object RetryPolicy {

    /** İlk istek dahil toplam deneme sayısı. */
    const val MAX_ATTEMPTS = 3
    const val BASE_DELAY_MS = 400L
    const val MAX_DELAY_MS = 4_000L

    fun canRetry(method: String, attempt: Int): Boolean =
        method.equals("GET", ignoreCase = true) && attempt < MAX_ATTEMPTS - 1

    fun shouldRetryStatus(code: Int): Boolean =
        code == 408 || code == 429 || code in 500..599

    /** OkHttp yalnızca IOException fırlatır; iptal edilen çağrılar buraya gelmez. */
    fun shouldRetry(exception: IOException): Boolean = true

    fun delayMillis(
        attempt: Int,
        retryAfterSeconds: Long? = null,
        jitterMs: Long = Random.nextLong(0, BASE_DELAY_MS / 2 + 1)
    ): Long {
        if (retryAfterSeconds != null) {
            return (retryAfterSeconds * 1_000L).coerceIn(0L, MAX_DELAY_MS)
        }
        val exponential = BASE_DELAY_MS shl attempt.coerceIn(0, 10)
        return (exponential + jitterMs).coerceAtMost(MAX_DELAY_MS)
    }
}
