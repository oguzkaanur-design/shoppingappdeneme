package com.shoppingapp.sync

import android.content.Context
import com.google.gson.JsonParser
import com.shoppingapp.data.backup.BackupFile
import com.shoppingapp.data.backup.BackupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Senkronizasyon sonucu. Arayüz bunu tek satırlık bir mesaja çevirir.
 */
enum class SyncOutcome {
    /** Cihazdaki veri daha yeniydi; buluta yazıldı. */
    UPLOADED,

    /** Buluttaki yedek daha yeniydi; cihaza geri yüklendi. */
    DOWNLOADED,

    /** Bulutta yedek yoktu; ilk yükleme yapıldı. */
    FIRST_UPLOAD
}

/**
 * Cihazdaki veri ile Drive'daki yedeği karşılaştırır.
 *
 * Çakışma kuralı bilinçli olarak basit: **daha yeni `createdAt` kazanır**. Alışveriş
 * listesi tek kullanıcılı bir veri olduğu için satır bazlı birleştirme yerine
 * bütünü yeni olan tarafa bırakmak, sessiz veri karışmasından daha güvenli.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val drive: DriveBackupClient
) {

    suspend fun syncNow(token: String): Result<SyncOutcome> = withContext(Dispatchers.IO) {
        runCatching {
            val remote = drive.find(token)
            val localJson = backupManager.exportJson()
            val localCreatedAt = createdAtOf(localJson)

            if (remote == null) {
                drive.upload(token, localJson, existingId = null)
                return@runCatching SyncOutcome.FIRST_UPLOAD
            }

            val remoteJson = drive.download(token, remote.id)
            val remoteCreatedAt = createdAtOf(remoteJson)

            if (remoteCreatedAt > localCreatedAt) {
                restore(remoteJson)
                SyncOutcome.DOWNLOADED
            } else {
                drive.upload(token, localJson, existingId = remote.id)
                SyncOutcome.UPLOADED
            }
        }
    }

    /** İndirilen JSON'u doğrulayıp geri yükler; bozuk veri cihazdakini silmez. */
    private suspend fun restore(json: String) {
        val file = File(context.cacheDir, "drive-restore-${System.currentTimeMillis()}.json")
        try {
            file.writeText(json)
            backupManager.restoreLocal(
                BackupFile(
                    path = file.absolutePath,
                    name = DriveBackupClient.FILE_NAME,
                    createdAt = System.currentTimeMillis(),
                    sizeBytes = file.length(),
                    isAuto = false
                )
            ).getOrThrow()
        } finally {
            file.delete()
        }
    }

    private fun createdAtOf(json: String): Long =
        runCatching {
            JsonParser.parseString(json).asJsonObject.get("createdAt")?.asLong ?: 0L
        }.getOrDefault(0L)
}
