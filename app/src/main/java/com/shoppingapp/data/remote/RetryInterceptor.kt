package com.shoppingapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Geçici ağ hatalarında ve 5xx/429 yanıtlarında isteği sınırlı sayıda tekrarlar.
 * Yalnızca GET istekleri tekrarlanır; sepet/sipariş gibi yan etkili çağrılar
 * (bu uygulamada yok) asla iki kez gönderilmez.
 */
class RetryInterceptor(
    private val sleeper: (Long) -> Unit = { millis ->
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastError: IOException? = null

        while (true) {
            try {
                val response = chain.proceed(request)
                if (!RetryPolicy.shouldRetryStatus(response.code) ||
                    !RetryPolicy.canRetry(request.method, attempt)
                ) {
                    return response
                }

                val retryAfter = response.header("Retry-After")
                    ?.trim()
                    ?.toLongOrNull()
                response.close()
                sleeper(RetryPolicy.delayMillis(attempt, retryAfter))
                attempt++
            } catch (error: IOException) {
                if (!RetryPolicy.shouldRetry(error) ||
                    !RetryPolicy.canRetry(request.method, attempt)
                ) {
                    throw lastError ?: error
                }
                lastError = error
                sleeper(RetryPolicy.delayMillis(attempt))
                attempt++
            }
        }
    }
}
