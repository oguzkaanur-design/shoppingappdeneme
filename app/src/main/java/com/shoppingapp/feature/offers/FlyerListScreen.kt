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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shoppingapp.R
import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.offers.FlyerPriceLabel
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.LocalAppUiStyle
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
    val uiStyle = LocalAppUiStyle.current

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.s_firsatlar), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.s_prospektusten_eklediklerin),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                if (items.isNotEmpty()) {
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.s_listeyi_temizle))
                    }
                }
            }
        )
    }) { padding ->
        if (items.isEmpty()) {
            EmptyFlyerList(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        if (uiStyle == AppUiStyle.GALLERY) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "total") {
                    TotalCard(count = active.size, total = activeTotal, style = AppUiStyle.GALLERY)
                }
                if (active.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "active-title") {
                        SectionTitle(stringResource(R.string.s_gecerli_firsatlar), AppUiStyle.GALLERY)
                    }
                    gridItems(active, key = { it.id }) { item ->
                        FlyerItemRow(
                            item = item,
                            style = AppUiStyle.GALLERY,
                            onIncrease = { viewModel.increase(item) },
                            onDecrease = { viewModel.decrease(item) },
                            onRemove = { viewModel.remove(item) }
                        )
                    }
                }
                if (expired.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "expired-title") {
                        SectionTitle(stringResource(R.string.s_suresi_dolmus), AppUiStyle.GALLERY)
                    }
                    gridItems(expired, key = { it.id }) { item ->
                        FlyerItemRow(
                            item = item,
                            style = AppUiStyle.GALLERY,
                            onIncrease = { viewModel.increase(item) },
                            onDecrease = { viewModel.decrease(item) },
                            onRemove = { viewModel.remove(item) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    horizontal = if (uiStyle == AppUiStyle.COMPACT) 10.dp else 16.dp,
                    vertical = if (uiStyle == AppUiStyle.COMPACT) 6.dp else 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (uiStyle == AppUiStyle.COMPACT) 3.dp else 10.dp
                )
            ) {
                item {
                    TotalCard(count = active.size, total = activeTotal, style = uiStyle)
                }
                if (active.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.s_gecerli_firsatlar), uiStyle) }
                    items(active, key = { it.id }) { item ->
                        FlyerItemRow(
                            item = item,
                            style = uiStyle,
                            onIncrease = { viewModel.increase(item) },
                            onDecrease = { viewModel.decrease(item) },
                            onRemove = { viewModel.remove(item) }
                        )
                    }
                }
                if (expired.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.s_suresi_dolmus), uiStyle) }
                    items(expired, key = { it.id }) { item ->
                        FlyerItemRow(
                            item = item,
                            style = uiStyle,
                            onIncrease = { viewModel.increase(item) },
                            onDecrease = { viewModel.decrease(item) },
                            onRemove = { viewModel.remove(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalCard(count: Int, total: Double, style: AppUiStyle) {
    when (style) {
        AppUiStyle.MODERN -> ModernTotalCard(count, total)
        AppUiStyle.CLASSIC -> ClassicTotalCard(count, total)
        AppUiStyle.COMPACT -> CompactTotalCard(count, total)
        AppUiStyle.GALLERY -> GalleryTotalCard(count, total)
    }
}

@Composable
private fun ModernTotalCard(count: Int, total: Double) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    stringResource(R.string.p_x_firsat_urunu, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(stringResource(R.string.s_prospektus_fiyatlariyla_toplam),
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
private fun ClassicTotalCard(count: Int, total: Double) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.p_x_firsat_urunu, count).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    FlyerPriceLabel.format(total).orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                stringResource(R.string.s_prospektus_fiyatlariyla_toplam),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactTotalCard(count: Int, total: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.p_x_firsat_urunu, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            FlyerPriceLabel.format(total).orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun GalleryTotalCard(count: Int, total: Double) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    stringResource(R.string.p_x_firsat_urunu, count),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.s_prospektus_fiyatlariyla_toplam),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
            Text(
                FlyerPriceLabel.format(total).orEmpty(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, style: AppUiStyle) {
    when (style) {
        AppUiStyle.CLASSIC -> Column(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        AppUiStyle.COMPACT -> Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 3.dp)
        )

        else -> Text(
            text,
            style = if (style == AppUiStyle.GALLERY) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun FlyerItemRow(
    item: FlyerListItem,
    style: AppUiStyle,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    when (style) {
        AppUiStyle.MODERN -> ModernFlyerItemRow(item, onIncrease, onDecrease, onRemove)
        AppUiStyle.CLASSIC -> ClassicFlyerItemRow(item, onIncrease, onDecrease, onRemove)
        AppUiStyle.COMPACT -> CompactFlyerItemRow(item, onIncrease, onDecrease, onRemove)
        AppUiStyle.GALLERY -> GalleryFlyerItemRow(item, onIncrease, onDecrease, onRemove)
    }
}

@Composable
private fun ModernFlyerItemRow(
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
        shape = MaterialTheme.shapes.large,
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
                        stringResource(
                            R.string.flyer_expired_at,
                            validUntilLabel(item.validToEpochMs)
                        )
                    } else {
                        stringResource(
                            R.string.flyer_valid_until,
                            validUntilLabel(item.validToEpochMs)
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.p_x_x_2, FlyerPriceLabel.format(item.unitPrice).orEmpty(), item.quantity) +
                        FlyerPriceLabel.format(lineTotal).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.s_kaldir),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.s_azalt))
                    }
                    Text(
                        stringResource(R.string.p_x, item.quantity),
                        modifier = Modifier.width(22.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.s_artir))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicFlyerItemRow(
    item: FlyerListItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val expired = isExpired(item.validToEpochMs)
    val brandColor = Color(item.storeColor)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlyerProductThumb(
                item = item,
                modifier = Modifier.size(48.dp),
                corner = 0.dp
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.storeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = brandColor
                )
                Text(
                    validityLabel(item, expired),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    priceLine(item),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.s_kaldir),
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.s_azalt),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.p_x, item.quantity),
                        modifier = Modifier.width(22.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.s_artir),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CompactFlyerItemRow(
    item: FlyerListItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val expired = isExpired(item.validToEpochMs)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlyerProductThumb(
                item = item,
                modifier = Modifier.size(36.dp),
                corner = 4.dp
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 7.dp)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(priceLine(item))
                        append(" · ")
                        append(if (expired) validUntilLabel(item.validToEpochMs) else item.storeName)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(R.string.s_azalt),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                stringResource(R.string.p_x, item.quantity),
                modifier = Modifier.width(20.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.s_artir),
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.s_kaldir),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun GalleryFlyerItemRow(
    item: FlyerListItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val expired = isExpired(item.validToEpochMs)
    val brandColor = Color(item.storeColor)
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                FlyerProductThumb(
                    item = item,
                    modifier = Modifier.fillMaxWidth().height(104.dp),
                    corner = 0.dp
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.s_kaldir),
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.storeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = brandColor
                )
                Text(
                    validityLabel(item, expired),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    priceLine(item),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.s_azalt),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.p_x, item.quantity),
                        modifier = Modifier.width(22.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.s_artir),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyerProductThumb(
    item: FlyerListItem,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    val brandColor = Color(item.storeColor)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
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
}

@Composable
private fun validityLabel(item: FlyerListItem, expired: Boolean): String =
    if (expired) {
        stringResource(R.string.flyer_expired_at, validUntilLabel(item.validToEpochMs))
    } else {
        stringResource(R.string.flyer_valid_until, validUntilLabel(item.validToEpochMs))
    }

private fun priceLine(item: FlyerListItem): String {
    val unit = FlyerPriceLabel.format(item.unitPrice).orEmpty()
    val total = FlyerPriceLabel.format(item.unitPrice * item.quantity).orEmpty()
    return "$unit × ${item.quantity} · $total"
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
        Text(stringResource(R.string.s_firsat_listen_bos), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.s_prospektuste_isaretli_bir_urune_dokun_fiyati_ve_),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
