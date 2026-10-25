package com.shoppingapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Retry yalnızca GET + geçici hata durumunda çalışmalı. Bu testler gerçek bir
 * yerel HTTP sunucusu kullanır; gecikmeler enjekte edilerek test hızlı kalır.
 */
class RetryInterceptorTest {

    private lateinit var server: MockWebServer
    private val delays = mutableListOf<Long>()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor { millis -> delays += millis })
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `500 sonrasi basarili yanit tekrar denenir`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("gecici"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val response = client.newCall(get("/liste")).execute()

        response.use {
            assertEquals(200, it.code)
            assertEquals("ok", it.body?.string())
        }
        assertEquals(2, server.requestCount)
        assertEquals(1, delays.size)
        assertTrue("İlk bekleme 400-600 ms olmalı: ${delays[0]}", delays[0] in 400L..600L)
    }

    @Test
    fun `429 retry after basligina uyar`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "2")
        )
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(get("/limit")).execute().use { assertEquals(200, it.code) }

        assertEquals(2, server.requestCount)
        assertEquals(listOf(2_000L), delays)
    }

    @Test
    fun `post istegi asla tekrarlanmaz`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(server.url("/yaz"))
            .post("veri".toRequestBody())
            .build()

        client.newCall(request).execute().use { assertEquals(500, it.code) }

        assertEquals(1, server.requestCount)
        assertTrue("POST için bekleme olmamalı", delays.isEmpty())
    }

    @Test
    fun `deneme siniri asilinca son yanit doner`() {
        repeat(RetryPolicy.MAX_ATTEMPTS) {
            server.enqueue(MockResponse().setResponseCode(503))
        }

        client.newCall(get("/bakim")).execute().use { assertEquals(503, it.code) }

        assertEquals(RetryPolicy.MAX_ATTEMPTS, server.requestCount)
        assertEquals(RetryPolicy.MAX_ATTEMPTS - 1, delays.size)
    }

    private fun get(path: String): Request =
        Request.Builder().url(server.url(path)).get().build()
}
