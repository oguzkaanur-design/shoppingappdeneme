package com.shoppingapp.sync

import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Drive'da bulunan yedek dosyası. */
data class DriveBackupFile(val id: String, val modifiedTime: String?)

/**
 * Google Drive'ın **uygulamaya özel gizli klasörü** (`appDataFolder`) üzerinden
 * yedek alışverişi. Kullanıcının diğer Drive dosyalarına erişilmez; kapsam
 * yalnızca `drive.appdata`'dır.
 *
 * Ağ katmanı bilinçli olarak ince tutuldu: gövde/sorgu kurulumu saf fonksiyonlarda,
 * böylece cihaz olmadan da test edilebilir.
 */
@Singleton
class DriveBackupClient @Inject constructor(
    private val http: OkHttpClient
) {

    /** Testlerde MockWebServer'a yönlendirmek için; üretimde sabit Google adresleri. */
    internal var apiBase: String = API
    internal var uploadBase: String = UPLOAD_API

    companion object {
        /** Sunucudaki tek dosya adı; çakışmayı önlemek için markaya bağlı. */
        const val FILE_NAME = "korb-backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
        private val JSON = "application/json; charset=UTF-8".toMediaType()
    }

    /** Uygulama klasöründeki yedeği bulur; yoksa `null`. */
    fun find(token: String): DriveBackupFile? {
        val query = java.net.URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        val url = "$apiBase/files?spaces=appDataFolder&orderBy=modifiedTime%20desc" +
            "&fields=files(id,name,modifiedTime)&pageSize=1&q=$query"
        val body = execute(authorized(url, token).build()) ?: return null
        val files = JsonParser.parseString(body).asJsonObject
            .getAsJsonArray("files") ?: return null
        val first = files.firstOrNull()?.asJsonObject ?: return null
        val id = first.get("id")?.asString ?: return null
        return DriveBackupFile(id = id, modifiedTime = first.get("modifiedTime")?.asString)
    }

    fun download(token: String, fileId: String): String =
        execute(authorized("$apiBase/files/$fileId?alt=media", token).build())
            ?: error("Drive yedeği indirilemedi")

    /** Dosya varsa içeriği günceller, yoksa uygulama klasöründe oluşturur. */
    fun upload(token: String, json: String, existingId: String?): DriveBackupFile {
        if (existingId != null) {
            val request = authorized("$uploadBase/files/$existingId?uploadType=media", token)
                .patch(json.toRequestBody(JSON))
                .build()
            execute(request)
            return DriveBackupFile(existingId, null)
        }
        val boundary = "korb-" + UUID.randomUUID().toString().replace("-", "")
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(json).append("\r\n")
            append("--").append(boundary).append("--")
        }
        val request = authorized("$uploadBase/files?uploadType=multipart", token)
            .post(body.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()
        val response = execute(request) ?: error("Drive'a yedek yüklenemedi")
        val id = JsonParser.parseString(response).asJsonObject.get("id")?.asString
            ?: error("Drive yanıtında dosya kimliği yok")
        return DriveBackupFile(id, null)
    }

    private fun authorized(url: String, token: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")

    private fun execute(request: Request): String? =
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Drive isteği başarısız (${response.code}): ${text.take(160)}")
            }
            text.ifBlank { null }
        }
}
