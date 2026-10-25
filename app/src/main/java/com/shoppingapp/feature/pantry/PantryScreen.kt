package com.shoppingapp.feature.pantry

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.R
import com.shoppingapp.data.catalog.ProductIllustration
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.model.Store
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.LocalAppUiStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class PantrySection(
    val key: String,
    val title: String,
    val hint: String,
    val entries: List<PantryRowUi>
)

/**
 * Kiler / Vorratskammer: evdeki stok, kritik eşik ve son kullanma tarihi.
 *
 * Uygulamanın Bring!'den ayrıştığı yer burası: ürün bitmeye yaklaşınca tek
 * dokunuşla alışveriş listesine düşer ve alışveriş tamamlanınca stok kendiliğinden artar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(viewModel: PantryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiStyle = LocalAppUiStyle.current
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PantryRow?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.s_kiler), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.p_x_urun_takipte_x_kritik, state.entries.size, state.critical.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.critical.isNotEmpty()) {
                        IconButton(onClick = viewModel::restockAll) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = stringResource(R.string.s_kritikleri_listeye_ekle),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiStyle == AppUiStyle.COMPACT) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.s_kiler_urunu))
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.s_kiler_urunu)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        if (state.entries.isEmpty() && !state.loading) {
            EmptyPantry(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAdd = { showAdd = true }
            )
            return@Scaffold
        }

        val criticalIds = state.critical.map { it.row.id }.toSet()
        val expiring = state.expiring.filter { it.row.id !in criticalIds }
        val shownIds = criticalIds + state.expiring.map { it.row.id }
        val sections = buildList {
            if (state.critical.isNotEmpty()) {
                add(
                    PantrySection(
                        key = "critical",
                        title = stringResource(R.string.pantry_critical_title),
                        hint = stringResource(R.string.pantry_critical_hint),
                        entries = state.critical
                    )
                )
            }
            if (expiring.isNotEmpty()) {
                add(
                    PantrySection(
                        key = "expiring",
                        title = stringResource(R.string.pantry_expiring_title),
                        hint = stringResource(
                            R.string.pantry_expiring_hint,
                            PantrySummary.EXPIRY_SOON_DAYS
                        ),
                        entries = expiring
                    )
                )
            }
            val rest = state.entries.filter { it.row.id !in shownIds }
            if (rest.isNotEmpty()) {
                add(
                    PantrySection(
                        key = "all",
                        title = stringResource(R.string.pantry_all),
                        hint = stringResource(R.string.p_x_urun, rest.size),
                        entries = rest
                    )
                )
            }
        }

        val onIncrease: (PantryRowUi) -> Unit = { viewModel.changeQuantity(it.row, 1.0) }
        val onDecrease: (PantryRowUi) -> Unit = { viewModel.changeQuantity(it.row, -1.0) }
        val onAddToList: (PantryRowUi) -> Unit = { viewModel.addToList(it.row) }
        val onEdit: (PantryRowUi) -> Unit = { editing = it.row }
        val onDelete: (PantryRowUi) -> Unit = { viewModel.delete(it.row) }

        if (uiStyle == AppUiStyle.GALLERY) {
            PantryGalleryGrid(
                state = state,
                sections = sections,
                modifier = Modifier.fillMaxSize().padding(padding),
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onAddToList = onAddToList,
                onEdit = onEdit,
                onDelete = onDelete
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = if (uiStyle == AppUiStyle.COMPACT) 10.dp else 16.dp,
                    end = if (uiStyle == AppUiStyle.COMPACT) 10.dp else 16.dp,
                    top = if (uiStyle == AppUiStyle.COMPACT) 4.dp else 8.dp,
                    bottom = 110.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (uiStyle == AppUiStyle.COMPACT) 2.dp else 8.dp
                )
            ) {
                item(key = "summary") { PantrySummaryCard(state, uiStyle) }
                sections.forEach { section ->
                    item(key = "header-${section.key}") {
                        PantrySectionTitle(section.title, section.hint, uiStyle)
                    }
                    items(section.entries, key = { "${section.key}-${it.row.id}" }) { entry ->
                        PantryItemCard(
                            entry = entry,
                            style = uiStyle,
                            onIncrease = { onIncrease(entry) },
                            onDecrease = { onDecrease(entry) },
                            onAddToList = { onAddToList(entry) },
                            onEdit = { onEdit(entry) },
                            onDelete = { onDelete(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        PantryAddDialog(
            stores = stores,
            categories = categories,
            onDismiss = { showAdd = false },
            onConfirm = { name, qty, threshold, expiry, unit, storeId, categoryId ->
                viewModel.addItem(name, qty, threshold, expiry, unit, storeId, categoryId)
                showAdd = false
            }
        )
    }

    editing?.let { row ->
        ThresholdDialog(
            row = row,
            onDismiss = { editing = null },
            onConfirm = { threshold ->
                viewModel.setThreshold(row, threshold)
                editing = null
            }
        )
    }
}

@Composable
private fun PantrySummaryCard(state: PantryUiState, style: AppUiStyle) {
    when (style) {
        AppUiStyle.MODERN -> PantryModernSummary(state)
        AppUiStyle.CLASSIC -> PantryClassicSummary(state)
        AppUiStyle.COMPACT -> PantryCompactSummary(state)
        AppUiStyle.GALLERY -> PantryGallerySummary(state)
    }
}

@Composable
private fun PantryModernSummary(state: PantryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Kitchen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    stringResource(R.string.p_x_urun_alinacak, state.critical.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.p_x_takipte_x_yeterli, state.entries.size, state.stockedCount) +
                        stringResource(R.string.pantry_expiring_count, state.expiring.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PantryClassicSummary(state: PantryUiState) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.s_kiler).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.s_stok),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.p_x_urun_alinacak, state.critical.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    state.entries.size.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                stringResource(R.string.p_x_takipte_x_yeterli, state.entries.size, state.stockedCount) +
                    stringResource(R.string.pantry_expiring_count, state.expiring.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PantryCompactSummary(state: PantryUiState) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Kitchen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    stringResource(R.string.p_x_urun_alinacak, state.critical.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.p_x_takipte_x_yeterli, state.entries.size, state.stockedCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PantryGallerySummary(state: PantryUiState) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.s_kiler).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        state.entries.size.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.p_x_urun_alinacak, state.critical.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Kitchen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GalleryStat(stringResource(R.string.s_stok), state.stockedCount)
                GalleryStat(stringResource(R.string.pantry_critical_title), state.critical.size)
                GalleryStat(stringResource(R.string.pantry_status_expiring), state.expiring.size)
            }
        }
    }
}

@Composable
private fun GalleryStat(label: String, value: Int) {
    Column {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PantrySectionTitle(title: String, hint: String, style: AppUiStyle) {
    when (style) {
        AppUiStyle.CLASSIC -> Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        AppUiStyle.COMPACT -> Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp, start = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
            Text(
                title,
                style = if (style == AppUiStyle.GALLERY) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PantryItemCard(
    entry: PantryRowUi,
    style: AppUiStyle,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToList: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    when (style) {
        AppUiStyle.MODERN -> PantryModernItemCard(
            entry, onIncrease, onDecrease, onAddToList, onEdit, onDelete
        )

        AppUiStyle.CLASSIC -> PantryClassicItem(
            entry, onIncrease, onDecrease, onAddToList, onEdit, onDelete
        )

        AppUiStyle.COMPACT -> PantryCompactItem(
            entry, onIncrease, onDecrease, onAddToList, onEdit, onDelete
        )

        AppUiStyle.GALLERY -> PantryGalleryCard(
            entry, onIncrease, onDecrease, onAddToList, onEdit, onDelete
        )
    }
}

@Composable
private fun PantryModernItemCard(
    entry: PantryRowUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToList: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val row = entry.row
    val status = entry.status
    val urgent = status.needsRestock

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (urgent) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ProductIllustration.forProductName(row.name),
                    fontSize = 24.sp,
                    lineHeight = 28.sp
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    StatusChip(status, AppUiStyle.MODERN)
                    Text(
                        stringResource(R.string.p_min_x, quantity(row.minThreshold)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    row.expiryDate?.let {
                        Text(
                            stringResource(R.string.pantry_expiry_chip, dateLabel(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    QuantityButton(
                        Icons.Default.Remove,
                        status.level != PantryLevel.EMPTY,
                        stringResource(R.string.s_azalt),
                        AppUiStyle.MODERN,
                        onDecrease
                    )
                    Text(
                        stringResource(R.string.p_x_x_4, quantity(row.currentQuantity), row.unit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    QuantityButton(
                        Icons.Default.Add,
                        true,
                        stringResource(R.string.s_artir),
                        AppUiStyle.MODERN,
                        onIncrease
                    )

                    TextButton(onClick = onAddToList, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.s_listeye), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.s_esigi_duzenle),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.s_kilerden_kaldir),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PantryClassicItem(
    entry: PantryRowUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToList: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val row = entry.row
    val status = entry.status
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ProductIllustration.forProductName(row.name),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                modifier = Modifier.width(34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    StatusChip(status, AppUiStyle.CLASSIC)
                    Text(
                        stringResource(R.string.p_min_x, quantity(row.minThreshold)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    row.expiryDate?.let {
                        Text(
                            stringResource(R.string.pantry_expiry_chip, dateLabel(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    QuantityButton(
                        Icons.Default.Remove,
                        status.level != PantryLevel.EMPTY,
                        stringResource(R.string.s_azalt),
                        AppUiStyle.CLASSIC,
                        onDecrease
                    )
                    Text(
                        stringResource(R.string.p_x_x_4, quantity(row.currentQuantity), row.unit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    QuantityButton(
                        Icons.Default.Add,
                        true,
                        stringResource(R.string.s_artir),
                        AppUiStyle.CLASSIC,
                        onIncrease
                    )
                    TextButton(onClick = onAddToList, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(stringResource(R.string.s_listeye), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.s_esigi_duzenle),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.s_kilerden_kaldir),
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PantryCompactItem(
    entry: PantryRowUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToList: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val row = entry.row
    val status = entry.status
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ProductIllustration.forProductName(row.name),
                fontSize = 19.sp,
                lineHeight = 22.sp,
                modifier = Modifier.width(26.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(end = 2.dp)) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusChip(status, AppUiStyle.COMPACT)
                    Text(
                        stringResource(R.string.p_min_x, quantity(row.minThreshold)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text(
                stringResource(R.string.p_x_x_4, quantity(row.currentQuantity), row.unit),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
            QuantityButton(
                Icons.Default.Remove,
                status.level != PantryLevel.EMPTY,
                stringResource(R.string.s_azalt),
                AppUiStyle.COMPACT,
                onDecrease
            )
            QuantityButton(
                Icons.Default.Add,
                true,
                stringResource(R.string.s_artir),
                AppUiStyle.COMPACT,
                onIncrease
            )
            IconButton(onClick = onAddToList, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = stringResource(R.string.s_listeye),
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.s_esigi_duzenle),
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.s_kilerden_kaldir),
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun PantryGalleryCard(
    entry: PantryRowUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToList: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val row = entry.row
    val status = entry.status
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ProductIllustration.forProductName(row.name),
                    fontSize = 42.sp,
                    lineHeight = 48.sp
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                        StatusChip(status, AppUiStyle.GALLERY)
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.s_kilerden_kaldir),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.p_min_x, quantity(row.minThreshold)) +
                        (row.expiryDate?.let { " · " + stringResource(R.string.pantry_expiry_chip, dateLabel(it)) } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    stringResource(R.string.p_x_x_4, quantity(row.currentQuantity), row.unit),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuantityButton(
                        Icons.Default.Remove,
                        status.level != PantryLevel.EMPTY,
                        stringResource(R.string.s_azalt),
                        AppUiStyle.GALLERY,
                        onDecrease
                    )
                    QuantityButton(
                        Icons.Default.Add,
                        true,
                        stringResource(R.string.s_artir),
                        AppUiStyle.GALLERY,
                        onIncrease
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.s_esigi_duzenle),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onAddToList, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = stringResource(R.string.s_listeye),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: PantryStatus, style: AppUiStyle = AppUiStyle.MODERN) {
    val (text, container, content) = when {
        status.level == PantryLevel.EMPTY -> Triple(
            stringResource(R.string.pantry_status_empty),
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError
        )
        status.level == PantryLevel.LOW -> Triple(
            stringResource(R.string.pantry_status_critical),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        status.expiryLevel == ExpiryLevel.EXPIRED ->
            Triple(stringResource(R.string.pantry_expired), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        status.expiryLevel == ExpiryLevel.SOON -> Triple(
            stringResource(R.string.pantry_status_expiring),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            stringResource(R.string.pantry_status_enough),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(
        shape = when (style) {
            AppUiStyle.CLASSIC -> RoundedCornerShape(0.dp)
            AppUiStyle.COMPACT -> RoundedCornerShape(3.dp)
            else -> MaterialTheme.shapes.extraSmall
        },
        color = container
    ) {
        Text(
            text,
            color = content,
            fontSize = if (style == AppUiStyle.COMPACT) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun QuantityButton(
    icon: ImageVector,
    enabled: Boolean,
    description: String,
    style: AppUiStyle = AppUiStyle.MODERN,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(
            when (style) {
                AppUiStyle.COMPACT -> 24.dp
                AppUiStyle.GALLERY -> 30.dp
                else -> 28.dp
            }
        ),
        shape = when (style) {
            AppUiStyle.CLASSIC -> RoundedCornerShape(0.dp)
            AppUiStyle.COMPACT -> MaterialTheme.shapes.extraSmall
            else -> CircleShape
        }
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(if (style == AppUiStyle.COMPACT) 13.dp else 14.dp)
        )
    }
}

@Composable
private fun PantryGalleryGrid(
    state: PantryUiState,
    sections: List<PantrySection>,
    modifier: Modifier = Modifier,
    onIncrease: (PantryRowUi) -> Unit,
    onDecrease: (PantryRowUi) -> Unit,
    onAddToList: (PantryRowUi) -> Unit,
    onEdit: (PantryRowUi) -> Unit,
    onDelete: (PantryRowUi) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "summary") {
            PantrySummaryCard(state, AppUiStyle.GALLERY)
        }
        sections.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "header-${section.key}") {
                PantrySectionTitle(section.title, section.hint, AppUiStyle.GALLERY)
            }
            gridItems(section.entries, key = { "${section.key}-${it.row.id}" }) { entry ->
                PantryGalleryCard(
                    entry = entry,
                    onIncrease = { onIncrease(entry) },
                    onDecrease = { onDecrease(entry) },
                    onAddToList = { onAddToList(entry) },
                    onEdit = { onEdit(entry) },
                    onDelete = { onDelete(entry) }
                )
            }
        }
    }
}

@Composable
private fun EmptyPantry(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Kitchen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(stringResource(R.string.s_kiler_bos),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(stringResource(R.string.s_evde_takip_etmek_istedigin_urunleri_ekle_stok_kr),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(onClick = onAdd, modifier = Modifier.padding(top = 18.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.s_kiler_urunu_ekle))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryAddDialog(
    stores: List<Store>,
    categories: List<com.shoppingapp.data.model.Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Long?, String, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var thresholdText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Adet") }
    var storeId by remember { mutableStateOf(0L) }
    var categoryId by remember { mutableStateOf(0L) }
    var expiryDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val quantity = quantityText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val threshold = thresholdText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val valid = name.isNotBlank() && quantity > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s_kiler_urunu_ekle), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.s_urun_adi)) },
                    placeholder = { Text(stringResource(R.string.s_orn_sut_yumurta)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { value -> quantityText = value.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text(stringResource(R.string.s_stok)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { value -> thresholdText = value.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text(stringResource(R.string.s_kritik_esik)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.s_birim)) },
                    placeholder = { Text(stringResource(R.string.s_adet_l_kg)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        expiryDate?.let {
                            stringResource(R.string.pantry_expiry_chip, dateLabel(it))
                        } ?: stringResource(R.string.pantry_expiry_pick)
                    )
                }

                if (stores.isNotEmpty()) {
                    Text(stringResource(R.string.s_market), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = storeId == 0L,
                                onClick = { storeId = 0L },
                                label = { Text(stringResource(R.string.s_yok)) }
                            )
                        }
                        items(stores, key = { it.id }) { store ->
                            FilterChip(
                                selected = storeId == store.id,
                                onClick = { storeId = store.id },
                                label = { Text(store.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(parseColor(store.colorHex))
                                    )
                                }
                            )
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    Text(stringResource(R.string.s_kategori), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = categoryId == 0L,
                                onClick = { categoryId = 0L },
                                label = { Text(stringResource(R.string.s_otomatik)) }
                            )
                        }
                        items(categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = categoryId == category.id,
                                onClick = { categoryId = category.id },
                                label = { Text(category.nameTr) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onConfirm(
                        name.trim(),
                        quantity,
                        threshold.coerceAtLeast(0.0),
                        expiryDate,
                        unit.ifBlank { "Adet" },
                        storeId,
                        categoryId
                    )
                }
            ) { Text(stringResource(R.string.s_ekle)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s_vazgec)) } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate ?: System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expiryDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.s_tamam)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.s_vazgec)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ThresholdDialog(row: PantryRow, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(quantity(row.minThreshold)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s_kritik_esik), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    stringResource(R.string.p_x_bu_miktara_dusunce_listeye_eklenmesi_o, row.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text(stringResource(R.string.p_esik_x, row.unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.replace(',', '.').toDoubleOrNull() ?: 0.0) }) {
                Text(stringResource(R.string.s_kaydet))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s_vazgec)) } }
    )
}

private fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF047857))

private fun quantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.GERMANY, "%.1f", value)

private fun dateLabel(epochMs: Long): String =
    DateTimeFormatter.ofPattern("d MMM", Locale.GERMANY)
        .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate())


