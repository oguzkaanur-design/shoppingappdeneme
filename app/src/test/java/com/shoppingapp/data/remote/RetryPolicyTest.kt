package com.shoppingapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun `yalnizca get sinirli sayida tekrarlanir`() {
        assertTrue(RetryPolicy.canRetry("GET", 0))
        assertTrue(RetryPolicy.canRetry("get", 1))
        assertFalse(RetryPolicy.canRetry("GET", RetryPolicy.MAX_ATTEMPTS - 1))
        assertFalse(RetryPolicy.canRetry("POST", 0))
        assertFalse(RetryPolicy.canRetry("PUT", 0))
    }

    @Test
    fun `gecici durum kodlari tekrar denenir`() {
        listOf(408, 429, 500, 502, 503, 504).forEach {
            assertTrue("kod $it", RetryPolicy.shouldRetryStatus(it))
        }
        listOf(200, 201, 204, 301, 400, 401, 403, 404, 409, 422).forEach {
            assertFalse("kod $it", RetryPolicy.shouldRetryStatus(it))
        }
    }

    @Test
    fun `bekleme ustel olarak artar ve sinirda kesilir`() {
        assertEquals(400L, RetryPolicy.delayMillis(0, jitterMs = 0))
        assertEquals(800L, RetryPolicy.delayMillis(1, jitterMs = 0))
        assertEquals(1_600L, RetryPolicy.delayMillis(2, jitterMs = 0))
        assertEquals(4_000L, RetryPolicy.delayMillis(5, jitterMs = 0))
        assertEquals(4_000L, RetryPolicy.delayMillis(100, jitterMs = 0))
    }

    @Test
    fun `retry after saniye olarak uygulanir ve sinirlanir`() {
        assertEquals(2_000L, RetryPolicy.delayMillis(0, retryAfterSeconds = 2, jitterMs = 0))
        assertEquals(4_000L, RetryPolicy.delayMillis(0, retryAfterSeconds = 120, jitterMs = 0))
        assertEquals(0L, RetryPolicy.delayMillis(0, retryAfterSeconds = 0, jitterMs = 0))
    }
}
