package com.shoppingapp.feature.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.R
import com.shoppingapp.data.backup.BackupFile
import com.shoppingapp.data.backup.BackupManager
import com.shoppingapp.data.backup.BackupSummary
import com.shoppingapp.data.dao.FlyerListDao
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.notifications.NotificationHelper
import com.shoppingapp.notifications.ReminderPlanner
import com.shoppingapp.notifications.ReminderScheduler
import com.shoppingapp.notifications.ReminderSettings
import com.shoppingapp.security.CrashLog
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
                        onSuccess = { summary -> context.getString(R.string.backup_exported, summary.headline()) },
                        onFailure = { error ->
                            context.getString(R.string.backup_export_failed, error.message.orEmpty())
                        }
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
                        onSuccess = { summary -> context.getString(R.string.backup_imported, summary.headline()) },
                        onFailure = { error ->
                            context.getString(R.string.backup_import_failed, error.message.orEmpty())
                        }
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
                        onSuccess = { summary -> context.getString(R.string.backup_restored_msg, summary.headline()) },
                        onFailure = { error ->
                            context.getString(R.string.backup_import_failed, error.message.orEmpty())
                        }
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
                it.copy(
                    message = context.getString(
                        if (deleted) R.string.backup_deleted else R.string.backup_delete_failed
                    )
                )
            }
        }
    }

    /** Cihazdaki tüm veriyi (veritabanı, yerel yedekler, görseller, çökme kaydı) siler. */
    fun wipeAll() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = runCatching { backupManager.deleteEverything() }
            CrashLog.clear(context)
            _state.update {
                it.copy(
                    busy = false,
                    backups = emptyList(),
                    message = context.getString(
                        if (result.isSuccess) R.string.data_deleted else R.string.data_delete_failed
                    )
                )
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
                    message = context.getString(
                        if (file != null) R.string.backup_local_saved else R.string.backup_local_failed
                    )
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
                _state.update { it.copy(message = context.getString(R.string.reminder_none)) }
                return@launch
            }
            messages.forEach { message ->
                NotificationHelper.notify(
                    context = context,
                    id = message.id,
                    title = NotificationHelper.title(context, message),
                    text = NotificationHelper.body(context, message),
                    route = message.route
                )
            }
            _state.update {
                it.copy(message = context.getString(R.string.reminder_sent_count, messages.size))
            }
        }
    }

    fun setReminders(enabled: Boolean) {
        ReminderSettings.setEnabled(context, enabled)
        ReminderScheduler.sync(context, enabled)
        _state.update {
            it.copy(
                remindersEnabled = enabled,
                message = context.getString(
                    if (enabled) R.string.reminder_enabled else R.string.reminder_disabled
                )
            )
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    /** Yedek özetini cihaz dilinde tek satıra çevirir. */
    private fun BackupSummary.headline(): String = context.getString(
        R.string.backup_headline,
        listItems,
        pantryItems,
        purchases,
        flyerItems,
        moneyEntries
    )
}
