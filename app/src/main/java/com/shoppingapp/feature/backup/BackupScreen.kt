package com.shoppingapp.feature.backup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.R
import com.shoppingapp.account.AccountStore
import com.shoppingapp.data.backup.BackupFile
import com.shoppingapp.feature.language.LanguageCatalog
import com.shoppingapp.feature.language.LanguageStore
import com.shoppingapp.security.AppLock
import com.shoppingapp.security.AppLockSettingsDialog
import com.shoppingapp.security.CrashLog
import com.shoppingapp.security.LockDialogMode
import com.shoppingapp.security.PremiumCard
import com.shoppingapp.ui.theme.ThemeStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Yedekleme ve hatırlatıcı ayarları.
 *
 * Veri tamamen cihazda olduğu için bu ekran uygulamanın sigortası: kullanıcı
 * istediği yere JSON yedeği alabilir, geri yükleyebilir ve her gün kendiliğinden
 * alınan yerel yedeği görebilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAccount: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var pendingRestore by remember { mutableStateOf<BackupFile?>(null) }
    var pendingWipe by remember { mutableStateOf(false) }
    var lockDialog by remember { mutableStateOf<LockDialogMode?>(null) }
    var lockEnabled by remember { mutableStateOf(AppLock.isEnabled(context)) }
    var biometricEnabled by remember { mutableStateOf(AppLock.isBiometricEnabled(context)) }
    val biometricAvailable = remember { AppLock.canUseBiometric(context) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale.GERMANY) }
    val defaultFileName = remember {
        "korb-yedek-" + SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date()) + ".json"
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.setReminders(true) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_backup), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.s_tum_verin_cihazda),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(stringResource(R.string.s_liste_gecmis_kiler_cebim_ve_firsatlar_tek_bir_js),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.s_dosyaya_yedekle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.s_google_drive_indirilenler_veya_sd_kart_gibi_iste),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { exportLauncher.launch(defaultFileName) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.s_disa_aktar))
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.s_ice_aktar))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.s_hatirlaticilar), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.s_kilerde_azalan_urunler_suresi_dolan_firsatlar_ve),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.remindersEnabled,
                            onCheckedChange = { enabled ->
                                val needsPermission = enabled &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setReminders(enabled)
                                }
                            }
                        )
                    }

                    TextButton(
                        onClick = viewModel::testReminders,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.s_bildirimleri_simdi_dene))
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    stringResource(R.string.lock_settings_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.lock_settings_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = lockEnabled,
                                onCheckedChange = { enabled ->
                                    lockDialog = if (enabled) LockDialogMode.ENABLE else LockDialogMode.DISABLE
                                }
                            )
                        }

                        if (lockEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.lock_biometric),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = biometricEnabled && biometricAvailable,
                                    enabled = biometricAvailable,
                                    onCheckedChange = { enabled ->
                                        AppLock.setBiometricEnabled(context, enabled)
                                        biometricEnabled = AppLock.isBiometricEnabled(context)
                                    }
                                )
                            }
                            TextButton(
                                onClick = { lockDialog = LockDialogMode.CHANGE },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.lock_change_pin))
                            }
                        }
                    }
                }
            }

            item {
                val colorTheme = ThemeStore.colorTheme
                val uiStyle = ThemeStore.uiStyle
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToTheme),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                stringResource(R.string.theme_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(
                                    R.string.theme_selection_format,
                                    stringResource(colorTheme.labelRes),
                                    stringResource(uiStyle.labelRes)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToAccount),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_account),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                AccountStore.current(context).email
                                    ?: stringResource(R.string.account_signed_out),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { PremiumCard() }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToLanguage),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                stringResource(R.string.language_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                LanguageCatalog.displayName(LanguageStore.currentTag(context)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            stringResource(R.string.legal_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.legal_entry_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onNavigateToLegal, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.legal_title))
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.s_yerel_otomatik_yedekler),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.p_her_gun_bir_kez_son_x_yedek_saklanir, '5'),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = viewModel::createAutoBackup, enabled = !state.busy) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.s_simdi_al))
                    }
                }
            }

            if (state.backups.isEmpty()) {
                item {
                    Text(stringResource(R.string.s_henuz_yerel_yedek_yok_uygulama_her_acilista_ilk_),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(state.backups, key = { it.path }) { file ->
                    BackupFileRow(
                        file = file,
                        dateLabel = dateFormat.format(Date(file.createdAt)),
                        onRestore = { pendingRestore = file },
                        onDelete = { viewModel.deleteLocal(file) }
                    )
                }
            }

            item {
                Text(stringResource(R.string.s_not_prospektusten_eklenen_urun_gorselleri_yedege),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Tanılama: üçüncü taraf çökme servisi yok; kayıt cihazda tutulur ve
            // yalnızca kullanıcı isterse paylaşılır.
            item {
                OutlinedButton(
                    onClick = {
                        val log = CrashLog.read(context)
                        if (log == null) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.crash_none),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            shareCrashLog(context, log)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.crash_share))
                }
            }

            // Hesap yok; Google Play'in hesap silme şartı yerine cihazdaki tüm veriyi silme.
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            stringResource(R.string.data_delete_all),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.data_delete_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        OutlinedButton(
                            onClick = { pendingWipe = true },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.data_delete_confirm))
                        }
                    }
                }
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.backup_restore),
            text = stringResource(R.string.backup_restore_text),
            confirmLabel = stringResource(R.string.s_geri_yukle),
            onDismiss = { pendingImport = null },
            onConfirm = {
                viewModel.import(uri)
                pendingImport = null
            }
        )
    }

    if (pendingWipe) {
        ConfirmDialog(
            title = stringResource(R.string.data_delete_title),
            text = stringResource(R.string.data_delete_text),
            confirmLabel = stringResource(R.string.data_delete_confirm),
            onDismiss = { pendingWipe = false },
            onConfirm = {
                viewModel.wipeAll()
                pendingWipe = false
            }
        )
    }

    lockDialog?.let { mode ->
        AppLockSettingsDialog(
            mode = mode,
            onDismiss = { lockDialog = null },
            onComplete = { enabled, biometric ->
                lockEnabled = enabled
                biometricEnabled = biometric
                lockDialog = null
            }
        )
    }

    pendingRestore?.let { file ->
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_local),
            text = stringResource(
                R.string.backup_restore_local_text,
                dateFormat.format(Date(file.createdAt))
            ),
            confirmLabel = stringResource(R.string.s_geri_yukle),
            onDismiss = { pendingRestore = null },
            onConfirm = {
                viewModel.restoreLocal(file)
                pendingRestore = null
            }
        )
    }
}

@Composable
private fun BackupFileRow(
    file: BackupFile,
    dateLabel: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (file.isAuto) stringResource(R.string.backup_daily) else file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.p_x_x, dateLabel, sizeLabel(file.sizeBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRestore) { Text(stringResource(R.string.s_geri_yukle)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.s_yedegi_sil),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun shareCrashLog(context: android.content.Context, log: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_chooser))
        putExtra(Intent.EXTRA_TEXT, log)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.crash_chooser))
    )
}

private fun sizeLabel(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.GERMANY, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format(Locale.GERMANY, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
