package com.shoppingapp.feature.offers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shoppingapp.data.offers.Flyer
import com.shoppingapp.data.offers.FlyerPageUrls
import com.shoppingapp.data.offers.FlyerPhase
import com.shoppingapp.data.offers.FlyerStore
import com.shoppingapp.data.offers.FlyerStores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(viewModel: OffersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var openedFlyer by remember { mutableStateOf<Flyer?>(null) }
    val context = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text("Prospektüsler", fontWeight = FontWeight.Bold)
                Text(
                    "Haftalık broşürler · uygulama içinde",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StoreSelector(selected = state.selectedStore, onSelect = viewModel::selectStore)

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> LoadingState(store = state.selectedStore)

                    state.error != null -> ErrorState(
                        message = state.error.orEmpty(),
                        store = state.selectedStore,
                        onRetry = viewModel::retry
                    )

                    state.flyers.isEmpty() -> EmptyState(
                        store = state.selectedStore,
                        onRetry = viewModel::retry
                    )

                    else -> FlyerList(
                        flyers = state.flyers,
                        store = state.selectedStore,
                        onOpen = { openedFlyer = it }
                    )
                }
            }
        }
    }

    openedFlyer?.let { flyer ->
        FlyerReaderDialog(
            flyer = flyer,
            store = state.selectedStore,
            loadOffers = viewModel::offersForPage,
            onAddOffer = { offer, quantity ->
                viewModel.addOfferToFlyerList(offer, quantity)
                Toast.makeText(
                    context,
                    "${offer.name} fırsat listesine eklendi",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onClose = { openedFlyer = null }
        )
    }
}

@Composable
private fun StoreSelector(selected: FlyerStore, onSelect: (FlyerStore) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FlyerStores.all, key = { it.id }) { store ->
            val isSelected = store.id == selected.id
            val brand = Color(store.brandColor)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) brand else MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(store) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    store.shortName,
                    color = if (isSelected) Color.White else brand,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FlyerList(flyers: List<Flyer>, store: FlyerStore, onOpen: (Flyer) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeroBanner() }
        items(flyers, key = { it.leafletId }) { flyer ->
            FlyerCard(flyer = flyer, store = store, onOpen = { onOpen(flyer) })
        }
        item {
            Text(
                "Broşürler marketlerin yayınladığı haftalık prospektüslerden alınır (marktguru). " +
                    "Fiyatlar ve ürünler seçili mağazaya göre değişebilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun HeroBanner() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14583D))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFD8F36A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF17352C))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    "Fırsatlar kapında",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Broşürü seç, sayfaları kaydır, yakınlaştır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD7E7DD)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlyerCard(flyer: Flyer, store: FlyerStore, onOpen: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(FlyerPageUrls.cover(flyer.leafletId))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 86.dp, height = 122.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                PhaseBadge(flyer)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${store.shortName} Prospektü",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    FlyerDateLabel.range(flyer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    buildString {
                        append("${flyer.pageCount} sayfa")
                        if (flyer.offerCount > 0) append(" · ${flyer.offerCount} fırsat")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Uygulamada aç  →",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(store.brandColor)
                )
            }
        }
    }
}

@Composable
private fun PhaseBadge(flyer: Flyer) {
    val (container, content) = when (flyer.phase()) {
        FlyerPhase.CURRENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        FlyerPhase.UPCOMING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        FlyerPhase.PAST -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            FlyerDateLabel.phaseLabel(flyer),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun LoadingState(store: FlyerStore) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            "${store.name} prospektüsleri getiriliyor…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Broşür listesi internetten alınıyor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, store: FlyerStore, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Prospektüsler yüklenemedi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("  Yeniden dene")
        }
        TextButton(onClick = { openUrl(context, store.pageUrl) }) {
            Text("Resmî sayfayı tarayıcıda aç")
        }
    }
}

@Composable
private fun EmptyState(store: FlyerStore, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "${store.name} için güncel broşür bulunamadı",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Yeni haftanın broşürü henüz yayınlanmamış olabilir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("  Yeniden dene")
        }
        TextButton(onClick = { openUrl(context, store.pageUrl) }) {
            Text("Resmî sayfayı tarayıcıda aç")
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}
