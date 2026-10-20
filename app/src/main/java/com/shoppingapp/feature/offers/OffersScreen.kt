package com.shoppingapp.feature.offers

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen() {
    var selectedOffer by remember { mutableStateOf<OfficialOfferLinks.StoreLink?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text("Prospektüsler", fontWeight = FontWeight.Bold)
                Text("Güncel market fırsatları", style = MaterialTheme.typography.bodySmall)
            }
        })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14583D))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color(0xFFD8F36A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF17352C))
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("Fırsatlar kapında", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Broşürleri uygulama içinde keşfet", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD7E7DD))
                        }
                    }
                }
            }
            item {
                Text("Market seç", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(OfficialOfferLinks.stores, key = { it.name }) { offer ->
                StoreOfferCard(offer = offer, onClick = { selectedOffer = offer })
            }
            item {
                Text(
                    "Prospektüsler marketlerin resmî sayfalarından canlı yüklenir. Fiyatlar ve içerik seçili mağazaya göre değişebilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    selectedOffer?.let { offer ->
        Dialog(
            onDismissRequest = { selectedOffer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                InAppOfferReader(offer = offer, onClose = { selectedOffer = null })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreOfferCard(offer: OfficialOfferLinks.StoreLink, onClick: () -> Unit) {
    val brandColor = Color(offer.brandColor)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp).background(brandColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(offer.shortName, color = brandColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(offer.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) { Text("Görüntüle") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppOfferReader(offer: OfficialOfferLinks.StoreLink, onClose: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        val currentView = webView
        if (currentView?.canGoBack() == true) currentView.goBack() else onClose()
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (webView?.canGoBack() == true) webView?.goBack() else onClose()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Resmî prospektüs · uygulama içinde", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { loadError = null; isLoading = true; webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Yeniden yükle")
            }
        }
        Divider()
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                !OfferNavigationPolicy.allowsInAppNavigation(request.url.toString())

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                loadError = null
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    loadError = "Sayfa yüklenemedi. İnternet bağlantını kontrol edip yeniden dene."
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: WebResourceResponse
                            ) {
                                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                                    isLoading = false
                                    loadError = "Market sayfası şu anda açılamıyor (${errorResponse.statusCode}). Biraz sonra yeniden dene."
                                }
                            }
                        }
                        loadUrl(offer.url)
                    }
                }
            )
            if (isLoading && loadError == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
            }
            loadError?.let { message ->
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { loadError = null; isLoading = true; webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("  Yeniden dene")
                    }
                    Text(
                        "Bazı market sayfaları bölge veya çerez onayı isteyebilir.",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
