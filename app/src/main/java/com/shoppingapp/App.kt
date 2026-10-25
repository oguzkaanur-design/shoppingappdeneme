package com.shoppingapp

import android.app.Application
import android.content.Context
import com.shoppingapp.di.BackgroundEntryPoint
import com.shoppingapp.feature.language.LanguageStore
import com.shoppingapp.notifications.NotificationHelper
import com.shoppingapp.notifications.ReminderScheduler
import com.shoppingapp.notifications.ReminderSettings
import com.shoppingapp.security.CrashLog
import com.shoppingapp.ui.theme.ThemeStore
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : Application() {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageStore.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()

        // Kayıtlı görünüm seçimi ilk çizimden önce yüklenir.
        ThemeStore.init(this)

        // Yakalanmamış hatalar cihazda tutulur; kullanıcı isterse tanılama kaydı olarak paylaşır.
        CrashLog.install(this)

        NotificationHelper.ensureChannel(this)
        ReminderScheduler.sync(this, ReminderSettings.isEnabled(this))

        // Veri tek kopya: uygulama her açıldığında günlük yerel yedeğin alınıp
        // alınmadığı kontrol edilir (günde birden fazla yazmaz).
        // Market/kategori ayarları önce garanti edilir; ilk kurulumda da yedek yazılsın.
        backgroundScope.launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@App,
                    BackgroundEntryPoint::class.java
                )
                runCatching { entryPoint.shoppingRepository().seedConfig() }
                entryPoint.backupManager().autoBackupIfNeeded()
            }
        }
    }
}
