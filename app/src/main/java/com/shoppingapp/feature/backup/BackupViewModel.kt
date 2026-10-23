package com.shoppingapp.feature.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.backup.BackupFile
import com.shoppingapp.data.backup.BackupManager
import com.shoppingapp.data.dao.FlyerListDao
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.notifications.NotificationHelper
import com.shoppingapp.notifications.ReminderPlanner
import com.shoppingapp.notifications.ReminderScheduler
import com.shoppingapp.notifications.ReminderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val backups: List<BackupFile> = emptyList(),
    val remindersEnabled: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null
) {
    val lastAuto: BackupFile? get() = backups.firstOrNull { it.isAuto }
    val totalSizeBytes: Long get() = backups.sumOf { it.sizeBytes }
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val shoppingDao: ShoppingDao,
    private val flyerListDao: FlyerListDao
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(remindersEnabled = ReminderSettings.isEnabled(context)) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val backups = withContext(Dispatchers.IO) { backupManager.localBackups() }
            _state.update { it.copy(backups = backups) }
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = backupManager.exportTo(uri)
            _state.update {
                it.copy(
                    busy = false,
                    message = result.fold(
                        onSuccess = { summary -> "Yedek dışa aktarıldı · ${summary.headline}" },
                        onFailure = { error -> "Dışa aktarılamadı: ${error.message}" }
                    )
                )
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = backupManager.importFrom(uri)
            val backups = withContext(Dispatchers.IO) { backupManager.localBackups() }
            _state.update {
                it.copy(
                    busy = false,
                    backups = backups,
                    message = result.fold(
                        onSuccess = { summary -> "Geri yüklendi · ${summary.headline}" },
                        onFailure = { error -> "Geri yüklenemedi: ${error.message}" }
                    )
                )
            }
        }
    }

    fun restoreLocal(file: BackupFile) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = backupManager.restoreLocal(file)
            _state.update {
                it.copy(
                    busy = false,
                    message = result.fold(
                        onSuccess = { summary -> "Yedek geri yüklendi · ${summary.headline}" },
                        onFailure = { error -> "Geri yüklenemedi: ${error.message}" }
                    )
                )
            }
        }
    }

    fun deleteLocal(file: BackupFile) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { backupManager.deleteLocal(file) }
            refresh()
            _state.update {
                it.copy(message = if (deleted) "Yedek silindi" else "Yedek silinemedi")
            }
        }
    }

    fun createAutoBackup() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val file = runCatching { backupManager.autoBackupIfNeeded(force = true) }.getOrNull()
            val backups = withContext(Dispatchers.IO) { backupManager.localBackups() }
            _state.update {
                it.copy(
                    busy = false,
                    backups = backups,
                    message = if (file != null) "Yerel yedek alındı" else "Yedek alınamadı"
                )
            }
        }
    }

    /**
     * Bildirim yolunu kullanıcıya doğrulatır: günlük işi beklemeden, aynı planlayıcı
     * ve aynı bildirim kanalıyla şu an geçerli hatırlatıcıları gönderir.
     */
    fun testReminders() {
        viewModelScope.launch {
            val messages = runCatching {
                ReminderPlanner.plan(
                    offers = flyerListDao.getAll(),
                    pantry = shoppingDao.getPantryRowsOnce(),
                    pendingCount = shoppingDao.getPendingCountOnce()
                )
            }.getOrDefault(emptyList())

            if (messages.isEmpty()) {
                _state.update { it.copy(message = "Şu an hatırlatılacak bir şey yok") }
                return@launch
            }
            messages.forEach { message ->
                NotificationHelper.notify(
                    context = context,
                    id = message.id,
                    title = message.title,
                    text = message.text,
                    route = message.route
                )
            }
            _state.update { it.copy(message = "${messages.size} hatırlatıcı gönderildi") }
        }
    }

    fun setReminders(enabled: Boolean) {
        ReminderSettings.setEnabled(context, enabled)
        ReminderScheduler.sync(context, enabled)
        _state.update {
            it.copy(
                remindersEnabled = enabled,
                message = if (enabled) "Hatırlatıcılar açık · her gün 18:00" else "Hatırlatıcılar kapalı"
            )
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }
}
