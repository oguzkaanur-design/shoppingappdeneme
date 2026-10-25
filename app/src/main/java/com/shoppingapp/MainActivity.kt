package com.shoppingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoppingapp.R
import com.shoppingapp.feature.language.LanguageStore
import com.shoppingapp.notifications.NotificationHelper
import com.shoppingapp.security.AppIntegrity
import com.shoppingapp.security.AppLock
import com.shoppingapp.security.AppLockScreen
import com.shoppingapp.security.AppLockStateViewModel
import com.shoppingapp.ui.AppNavigation
import com.shoppingapp.ui.theme.ShoppingAppTheme
import com.shoppingapp.ui.theme.ThemeStore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private companion object {
        const val LOCK_TIMEOUT_MS = 3 * 60 * 1000L
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageStore.wrap(newBase))
    }

    /** Bildirim veya widget'tan gelen ekran isteği; tüketilince null olur. */
    private var requestedRoute by mutableStateOf<String?>(null)

    /** WhatsApp/mesaj uygulamasından paylaşılan liste metni; içe aktarılınca temizlenir. */
    private var sharedText by mutableStateOf<String?>(null)

    private val lockState: AppLockStateViewModel by viewModels()
    private var backgroundedAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 36'da edge-to-edge zorunlu; sistem çubukları saydam kalır ve
        // içerik arkalarına çizilir. Scaffold'lar insetleri tüketerek üst üste binmeyi önler.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ThemeStore.init(this)
        requestedRoute = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE)
        sharedText = intent?.sharedShoppingText()
        // ViewModel ilk oluşturulduğunda kilidi uygular; döndürme/dil değişiminde
        // açık oturum korunur, süreç yeniden başlarsa kilit geri gelir.
        lockState.initialize(AppLock.isEnabled(this))

        // Yayın imzası tutmuyorsa APK yeniden paketlenmiştir; veriye erişim açılmaz.
        val tampered = !BuildConfig.DEBUG && !AppIntegrity.isTrusted(this)

        setContent {
            ShoppingAppTheme {
                // Yüzey tüm pencereyi kaplar; saydam durum çubuğunun arkasında
                // tema arka planı görünür, ikon kontrastı Theme'de ayarlanır.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        tampered -> TamperedBuildScreen()
                        lockState.locked -> AppLockScreen(
                            errorMessage = lockState.error,
                            biometricEnabled = AppLock.isBiometricEnabled(this@MainActivity) &&
                                AppLock.canUseBiometric(this@MainActivity),
                            onSubmit = { pin -> submitPin(pin) },
                            onBiometric = { showBiometricPrompt() }
                        )
                        else -> AppNavigation(
                            routeRequest = requestedRoute,
                            onRouteHandled = { requestedRoute = null },
                            sharedText = sharedText,
                            onSharedTextHandled = { sharedText = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(NotificationHelper.EXTRA_ROUTE)
        if (!route.isNullOrBlank()) requestedRoute = route
        intent.sharedShoppingText()?.let { sharedText = it }
    }

    override fun onStart() {
        super.onStart()
        val leftAt = backgroundedAt
        backgroundedAt = null
        if (leftAt != null && AppLock.isEnabled(this) &&
            SystemClock.elapsedRealtime() - leftAt >= LOCK_TIMEOUT_MS
        ) {
            lockState.lock()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && AppLock.isEnabled(this)) {
            backgroundedAt = SystemClock.elapsedRealtime()
        }
    }

    private fun submitPin(pin: String) {
        val result = AppLock.verify(this, pin)
        if (result.success) {
            lockState.unlock()
            backgroundedAt = null
        } else {
            lockState.showError(
                if (result.lockoutSeconds > 0) {
                    getString(R.string.lock_locked_out, result.lockoutSeconds)
                } else {
                    getString(R.string.lock_wrong_pin)
                }
            )
        }
    }

    private fun showBiometricPrompt() {
        if (!AppLock.canUseBiometric(this)) return
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockState.unlock()
                    backgroundedAt = null
                }

                override fun onAuthenticationFailed() {
                    lockState.showError(getString(R.string.lock_wrong_pin))
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_screen_title))
            .setSubtitle(getString(R.string.lock_biometric_unlock))
            .setNegativeButtonText(getString(R.string.action_cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}

/** Paylaşım menüsünden gelen düz metin; liste içe aktarma sayfasını açar. */
private fun Intent.sharedShoppingText(): String? {
    if (action != Intent.ACTION_SEND || type != "text/plain") return null
    val text = getStringExtra(Intent.EXTRA_TEXT)
        ?.trim()
        .orEmpty()
        .take(com.shoppingapp.feature.share.ListImport.MAX_INPUT_CHARS)
    return text.takeIf { it.isNotBlank() }
}

@androidx.compose.runtime.Composable
private fun TamperedBuildScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.s_bu_surum_dogrulanamadi),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(text = stringResource(R.string.s_uygulamanin_imzasi_orijinal_yayinla_eslesmiyor_d),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
