package com.shoppingapp.sync

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Drive taşıma katmanı gerçek HTTP üzerinden denenir: uygulama klasörü sorgusu,
 * indirme, ilk yükleme (multipart) ve güncelleme.
 */
class DriveBackupClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DriveBackupClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = DriveBackupClient(OkHttpClient()).apply {
            apiBase = server.url("/drive/v3").toString().trimEnd('/')
            uploadBase = server.url("/upload/drive/v3").toString().trimEnd('/')
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun findsTheExistingBackupFile() {
        server.enqueue(
            MockResponse().setBody(
                """{"files":[{"id":"abc","name":"korb-backup.json","modifiedTime":"2026-10-25T10:00:00Z"}]}"""
            )
        )

        val file = client.find("token-123")

        assertEquals("abc", file?.id)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/drive/v3/files?"))
        assertTrue(request.path!!.contains("appDataFolder"))
        assertTrue(request.path!!.contains("korb-backup.json"))
        assertEquals("Bearer token-123", request.getHeader("Authorization"))
    }

    @Test
    fun returnsNullWhenTheAppFolderIsEmpty() {
        server.enqueue(MockResponse().setBody("""{"files":[]}"""))
        assertNull(client.find("token"))
    }

    @Test
    fun downloadReturnsTheStoredJson() {
        server.enqueue(MockResponse().setBody("""{"schemaVersion":1,"createdAt":42}"""))

        val json = client.download("token", "abc")

        assertEquals("""{"schemaVersion":1,"createdAt":42}""", json)
        assertEquals("/drive/v3/files/abc?alt=media", server.takeRequest().path)
    }

    @Test
    fun firstUploadCreatesTheFileInTheAppFolder() {
        server.enqueue(MockResponse().setBody("""{"id":"new-id"}"""))

        val uploaded = client.upload("token", """{"createdAt":7}""", existingId = null)

        assertEquals("new-id", uploaded.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.startsWith("/upload/drive/v3/files?uploadType=multipart"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"name\":\"korb-backup.json\""))
        assertTrue(body.contains("\"parents\":[\"appDataFolder\"]"))
        assertTrue(body.contains("\"createdAt\":7"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/related; boundary="))
    }

    @Test
    fun laterUploadsPatchTheExistingFile() {
        server.enqueue(MockResponse().setBody("""{"id":"abc"}"""))

        client.upload("token", """{"createdAt":8}""", existingId = "abc")

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/upload/drive/v3/files/abc?uploadType=media", request.path)
        assertEquals("""{"createdAt":8}""", request.body.readUtf8())
    }

    @Test
    fun httpErrorsBecomeReadableFailures() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        val error = runCatching { client.download("expired", "abc") }.exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("401"))
    }
}
