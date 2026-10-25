package com.shoppingapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoppingapp.R
import com.shoppingapp.data.model.FrequentProductRow
import com.shoppingapp.data.model.Store
import com.shoppingapp.ui.components.ProductThumb
import java.util.Locale

// --------------------------------------------------------------- klasik başlık

@Composable
internal fun ClassicShoppingHeader(
    stores: List<Store>,
    selectedStoreId: Long?,
    onSelectStore: (Long?) -> Unit,
    frequent: List<FrequentProductRow>,
    onAddFrequent: (FrequentProductRow) -> Unit,
    pendingCount: Int,
    purchasedCount: Int,
    pfandTotal: Double,
    euroTotal: Double,
    onComplete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.s_bugunku_liste),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        ClassicStoreFilterRow(
            stores = stores,
            selectedStoreId = selectedStoreId,
            onSelect = onSelectStore
        )

        if (frequent.isNotEmpty()) {
            FrequentStrip(rows = frequent, onAdd = onAddFrequent)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(R.string.p_x_alinacak_x_alindi, pendingCount, purchasedCount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.s_bugunku_liste),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            stringResource(R.string.s_sepet),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(euro(euroTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.s_pfand),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(euro(pfandTotal), style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (pendingCount > 0) {
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag(ShoppingScreenTestTags.SUMMARY_COMPLETE),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.s_alisverisi_tamamla))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicStoreFilterRow(
    stores: List<Store>,
    selectedStoreId: Long?,
    onSelect: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ClassicStoreTab(
                label = stringResource(R.string.store_all),
                selected = selectedStoreId == null,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onSelect(null) }
            )
        }
        items(stores, key = { it.id }) { store ->
            ClassicStoreTab(
                label = store.name,
                selected = selectedStoreId == store.id,
                color = parseColor(store.colorHex),
                onClick = { onSelect(store.id) }
            )
        }
    }
}

@Composable
private fun ClassicStoreTab(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Box(
            Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}

// -------------------------------------------------------------- kompakt başlık

@Composable
internal fun CompactShoppingHeader(
    stores: List<Store>,
    selectedStoreId: Long?,
    onSelectStore: (Long?) -> Unit,
    frequent: List<FrequentProductRow>,
    onAddFrequent: (FrequentProductRow) -> Unit,
    pendingCount: Int,
    purchasedCount: Int,
    euroTotal: Double,
    onComplete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StoreFilterRow(
            stores = stores,
            selectedStoreId = selectedStoreId,
            onSelect = onSelectStore
        )
        if (frequent.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(frequent, key = { it.productId }) { row ->
                    AssistChip(
                        onClick = { onAddFrequent(row) },
                        label = {
                            Text(
                                row.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.p_x_alinacak_x_alindi, pendingCount, purchasedCount),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                euro(euroTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (pendingCount > 0) {
                FilledTonalButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .height(32.dp)
                        .testTag(ShoppingScreenTestTags.SUMMARY_COMPLETE),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        stringResource(R.string.s_tamamla),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// -------------------------------------------------------------- klasik satırlar

@Composable
internal fun ClassicShoppingItem(
    item: UiShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    val purchased = item.listItem.isPurchased
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = purchased,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (purchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (purchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    item.store?.let { store ->
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(parseColor(store.colorHex))
                        )
                        Text(
                            store.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 5.dp)
                        )
                    }
                    if (item.product.pfandAmount > 0) {
                        Text(
                            stringResource(
                                R.string.p_pfand_x,
                                euro(item.product.pfandAmount * item.listItem.quantity)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    ClassicStepButton(
                        icon = Icons.Outlined.Remove,
                        enabled = item.listItem.quantity > 1.0,
                        description = stringResource(R.string.s_azalt)
                    ) { onQuantityChange(-1.0) }
                    Text(
                        stringResource(
                            R.string.p_x_x_4,
                            quantity(item.listItem.quantity),
                            item.product.unit
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    ClassicStepButton(
                        icon = Icons.Default.Add,
                        enabled = item.listItem.quantity < 999.0,
                        description = stringResource(R.string.s_artir)
                    ) { onQuantityChange(1.0) }
                }
            }
            if (item.product.priceEuro > 0) {
                Text(
                    euro(item.product.priceEuro * item.listItem.quantity),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.s_sil),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ClassicStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .size(26.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.padding(5.dp))
    }
}

// ------------------------------------------------------------- kompakt satırlar

@Composable
internal fun CompactShoppingItem(
    item: UiShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    val purchased = item.listItem.isPurchased
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (purchased) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                )
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .heightIn(min = 42.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = purchased,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(28.dp)
            )
            ProductThumb(
                product = item.product,
                modifier = Modifier
                    .size(30.dp)
                    .alpha(if (purchased) 0.5f else 1f),
                cornerRadius = 6.dp
            )
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append(item.product.name)
                        item.store?.let { store ->
                            append("  ·  ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(store.name)
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (purchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (purchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (item.product.pfandAmount > 0) {
                    Text(
                        stringResource(
                            R.string.p_pfand_x,
                            euro(item.product.pfandAmount * item.listItem.quantity)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CompactStep(
                icon = Icons.Outlined.Remove,
                enabled = item.listItem.quantity > 1.0,
                description = stringResource(R.string.s_azalt)
            ) { onQuantityChange(-1.0) }
            Text(
                stringResource(
                    R.string.p_x_x_4,
                    quantity(item.listItem.quantity),
                    item.product.unit
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp),
                maxLines = 1
            )
            CompactStep(
                icon = Icons.Default.Add,
                enabled = item.listItem.quantity < 999.0,
                description = stringResource(R.string.s_artir)
            ) { onQuantityChange(1.0) }
            if (item.product.priceEuro > 0) {
                Text(
                    euro(item.product.priceEuro * item.listItem.quantity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp),
                    maxLines = 1
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.s_sil),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun CompactStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(24.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.padding(5.dp))
    }
}

// ---------------------------------------------------------------------- vitrin

@Composable
internal fun GalleryShoppingList(
    pending: List<UiShoppingItem>,
    purchased: List<UiShoppingItem>,
    stores: List<Store>,
    selectedStoreId: Long?,
    onSelectStore: (Long?) -> Unit,
    frequent: List<FrequentProductRow>,
    onAddFrequent: (FrequentProductRow) -> Unit,
    pfandTotal: Double,
    euroTotal: Double,
    onComplete: () -> Unit,
    onToggle: (UiShoppingItem) -> Unit,
    onDelete: (UiShoppingItem) -> Unit,
    onQuantityChange: (UiShoppingItem, Double) -> Unit,
    onClearPurchased: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = rememberLazyGridState(),
        modifier = modifier.fillMaxSize().testTag(ShoppingScreenTestTags.LIST),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                StoreFilterRow(
                    stores = stores,
                    selectedStoreId = selectedStoreId,
                    onSelect = onSelectStore
                )
                if (frequent.isNotEmpty()) {
                    FrequentStrip(rows = frequent, onAdd = onAddFrequent)
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            GalleryHero(
                pendingCount = pending.size,
                purchasedCount = purchased.size,
                pfandTotal = pfandTotal,
                euroTotal = euroTotal,
                onComplete = onComplete
            )
        }

        if (pending.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(
                        R.string.p_x_x_6,
                        stringResource(R.string.section_to_buy),
                        pending.size
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }
            items(pending, key = { "gallery-${it.listItem.id}" }) { item ->
                GalleryItemCard(
                    item = item,
                    onToggle = { onToggle(item) },
                    onDelete = { onDelete(item) },
                    onQuantityChange = { delta -> onQuantityChange(item, delta) }
                )
            }
        }

        if (purchased.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.p_alinanlar_x, purchased.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearPurchased) {
                        Text(stringResource(R.string.s_temizle))
                    }
                }
            }
            items(purchased, key = { "gallery-${it.listItem.id}" }) { item ->
                GalleryItemCard(
                    item = item,
                    onToggle = { onToggle(item) },
                    onDelete = { onDelete(item) },
                    onQuantityChange = { delta -> onQuantityChange(item, delta) }
                )
            }
        }
    }
}

@Composable
private fun GalleryHero(
    pendingCount: Int,
    purchasedCount: Int,
    pfandTotal: Double,
    euroTotal: Double,
    onComplete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.s_bugunku_liste),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        pendingCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.p_x_alinacak_x_alindi, pendingCount, purchasedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.s_sepet),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        euro(euroTotal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.p_pfand_x, euro(pfandTotal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            if (pendingCount > 0) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag(ShoppingScreenTestTags.SUMMARY_COMPLETE)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.s_alisverisi_tamamla))
                }
            }
        }
    }
}

@Composable
private fun GalleryItemCard(
    item: UiShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    val purchased = item.listItem.isPurchased
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (purchased) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (purchased) 0.dp else 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ProductThumb(
                    product = item.product,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .alpha(if (purchased) 0.55f else 1f),
                    cornerRadius = 18.dp
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Checkbox(
                        checked = purchased,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.size(30.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                ) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.s_sil),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (purchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (purchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                item.store?.let { store ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(parseColor(store.colorHex))
                        )
                        Text(
                            store.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactStep(
                        icon = Icons.Outlined.Remove,
                        enabled = item.listItem.quantity > 1.0,
                        description = stringResource(R.string.s_azalt)
                    ) { onQuantityChange(-1.0) }
                    Text(
                        stringResource(
                            R.string.p_x_x_4,
                            quantity(item.listItem.quantity),
                            item.product.unit
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp),
                        maxLines = 1
                    )
                    CompactStep(
                        icon = Icons.Default.Add,
                        enabled = item.listItem.quantity < 999.0,
                        description = stringResource(R.string.s_artir)
                    ) { onQuantityChange(1.0) }
                    Spacer(Modifier.weight(1f))
                    if (item.product.priceEuro > 0) {
                        Text(
                            euro(item.product.priceEuro * item.listItem.quantity),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
