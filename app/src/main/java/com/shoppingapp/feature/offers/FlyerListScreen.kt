package com.shoppingapp.feature.offers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.offers.FlyerPriceLabel
import java.io.File

/**
 * Prospektüsten eklenen fırsat ürünleri. Normal alışveriş listesinden ayrıdır çünkü
 * her satır broşür fiyatı ve son geçerlilik tarihiyle birlikte gösterilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyerListScreen(viewModel: FlyerListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val active = items.filter { !isExpired(it.validToEpochMs) }
    val expired = items.filter { isExpired(it.validToEpochMs) }
    val activeTotal = active.sumOf { it.unitPrice * it.quantity }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text("Fırsatlar", fontWeight = FontWeight.Bold)
                    Text(
                        "Prospektüsten eklediklerin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                if (items.isNotEmpty()) {
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Listeyi temizle")
                    }
                }
            }
        )
    }) { padding ->
        if (items.isEmpty()) {
            EmptyFlyerList(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TotalCard(count = active.size, total = activeTotal)
            }
            if (active.isNotEmpty()) {
                item { SectionTitle("Geçerli fırsatlar") }
                items(active, key = { it.id }) { item ->
                    FlyerItemRow(
                        item = item,
                        onIncrease = { viewModel.increase(item) },
                        onDecrease = { viewModel.decrease(item) },
                        onRemove = { viewModel.remove(item) }
                    )
                }
            }
            if (expired.isNotEmpty()) {
                item { SectionTitle("Süresi dolmuş") }
                items(expired, key = { it.id }) { item ->
                    FlyerItemRow(
                        item = item,
                        onIncrease = { viewModel.increase(item) },
                        onDecrease = { viewModel.decrease(item) },
                        onRemove = { viewModel.remove(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalCard(count: Int, total: Double) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "$count fırsat ürünü",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Prospektüs fiyatlarıyla toplam",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                FlyerPriceLabel.format(total).orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun FlyerItemRow(
    item: FlyerListItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val expired = isExpired(item.validToEpochMs)
    val brandColor = Color(item.storeColor)
    val lineTotal = item.unitPrice * item.quantity

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brandColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagePath != null && File(item.imagePath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(item.imagePath)).build(),
                        contentDescription = item.productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        item.productName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = brandColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.storeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (expired) {
                        "Süresi doldu (${validUntilLabel(item.validToEpochMs)})"
                    } else {
                        "${validUntilLabel(item.validToEpochMs)} tarihine kadar"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${FlyerPriceLabel.format(item.unitPrice).orEmpty()} × ${item.quantity} = " +
                        FlyerPriceLabel.format(lineTotal).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Kaldır",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Azalt")
                    }
                    Text(
                        "${item.quantity}",
                        modifier = Modifier.width(22.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Artır")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFlyerList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        Text("Fırsat listen boş", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Prospektüste ＋ işaretli bir ürüne dokun, fiyatı ve son geçerlilik tarihiyle buraya eklensin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
