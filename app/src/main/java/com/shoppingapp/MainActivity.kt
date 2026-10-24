package com.shoppingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoppingapp.notifications.NotificationHelper
import com.shoppingapp.security.AppIntegrity
import com.shoppingapp.ui.AppNavigation
import com.shoppingapp.ui.theme.ShoppingAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Bildirim veya widget'tan gelen ekran isteği; tüketilince null olur. */
    private var requestedRoute by mutableStateOf<String?>(null)

    /** WhatsApp/mesaj uygulamasından paylaşılan liste metni; içe aktarılınca temizlenir. */
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRoute = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE)
        sharedText = intent?.sharedShoppingText()

        // Yayın imzası tutmuyorsa APK yeniden paketlenmiştir; veriye erişim açılmaz.
        val tampered = !BuildConfig.DEBUG && !AppIntegrity.isTrusted(this)

        setContent {
            ShoppingAppTheme {
                if (tampered) {
                    TamperedBuildScreen()
                } else {
                    AppNavigation(
                        routeRequest = requestedRoute,
                        onRouteHandled = { requestedRoute = null },
                        sharedText = sharedText,
                        onSharedTextHandled = { sharedText = null }
                    )
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
}

/** Paylaşım menüsünden gelen düz metin; liste içe aktarma sayfasını açar. */
private fun Intent.sharedShoppingText(): String? {
    if (action != Intent.ACTION_SEND || type != "text/plain") return null
    val text = getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
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
            Text(
                text = "Bu sürüm doğrulanamadı",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Uygulamanın imzası orijinal yayınla eşleşmiyor. Değiştirilmiş bir kopya " +
                    "olabilir; güvenliğin için verilerine erişim kapatıldı. Resmî sürümü yeniden yükle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
