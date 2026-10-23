package com.shoppingapp.feature.pantry

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.data.catalog.ProductIllustration
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.model.Store
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                        Text("Kiler", fontWeight = FontWeight.Bold)
                        Text(
                            "${state.entries.size} ürün takipte · ${state.critical.size} kritik",
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
                                contentDescription = "Kritikleri listeye ekle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Kiler ürünü") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (state.entries.isEmpty() && !state.loading) {
            EmptyPantry(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAdd = { showAdd = true }
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { PantrySummaryCard(state) }

            if (state.critical.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Kritik stok",
                        hint = "Eşik altına düştü · tek dokunuşla listeye ekle"
                    )
                }
                items(state.critical, key = { "critical-${it.row.id}" }) { entry ->
                    PantryItemCard(
                        entry = entry,
                        onIncrease = { viewModel.changeQuantity(entry.row, 1.0) },
                        onDecrease = { viewModel.changeQuantity(entry.row, -1.0) },
                        onAddToList = { viewModel.addToList(entry.row) },
                        onEdit = { editing = entry.row },
                        onDelete = { viewModel.delete(entry.row) }
                    )
                }
            }

            val criticalIds = state.critical.map { it.row.id }.toSet()
            val expiring = state.expiring.filter { it.row.id !in criticalIds }
            if (expiring.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Son kullanma yaklaşan",
                        hint = "${PantrySummary.EXPIRY_SOON_DAYS} gün veya daha az"
                    )
                }
                items(expiring, key = { "expiring-${it.row.id}" }) { entry ->
                    PantryItemCard(
                        entry = entry,
                        onIncrease = { viewModel.changeQuantity(entry.row, 1.0) },
                        onDecrease = { viewModel.changeQuantity(entry.row, -1.0) },
                        onAddToList = { viewModel.addToList(entry.row) },
                        onEdit = { editing = entry.row },
                        onDelete = { viewModel.delete(entry.row) }
                    )
                }
            }

            val shownIds = criticalIds + state.expiring.map { it.row.id }
            val rest = state.entries.filter { it.row.id !in shownIds }
            if (rest.isNotEmpty()) {
                item { SectionTitle(title = "Tüm kiler", hint = "${rest.size} ürün") }
                items(rest, key = { "all-${it.row.id}" }) { entry ->
                    PantryItemCard(
                        entry = entry,
                        onIncrease = { viewModel.changeQuantity(entry.row, 1.0) },
                        onDecrease = { viewModel.changeQuantity(entry.row, -1.0) },
                        onAddToList = { viewModel.addToList(entry.row) },
                        onEdit = { editing = entry.row },
                        onDelete = { viewModel.delete(entry.row) }
                    )
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
private fun PantrySummaryCard(state: PantryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                    "${state.critical.size} ürün alınacak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${state.entries.size} takipte · ${state.stockedCount} yeterli · " +
                        "${state.expiring.size} SKT yakın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String) {
    Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
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

@Composable
private fun PantryItemCard(
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
        shape = RoundedCornerShape(16.dp),
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
                    StatusChip(status)
                    Text(
                        "min ${quantity(row.minThreshold)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    row.expiryDate?.let { Text(expiryLabel(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    QuantityButton(Icons.Default.Remove, status.level != PantryLevel.EMPTY, "Azalt", onDecrease)
                    Text(
                        "${quantity(row.currentQuantity)} ${row.unit}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    QuantityButton(Icons.Default.Add, true, "Arttır", onIncrease)

                    TextButton(onClick = onAddToList, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Listeye", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Eşiği düzenle",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Kilerden kaldır",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: PantryStatus) {
    val (text, container, content) = when {
        status.level == PantryLevel.EMPTY ->
            Triple("Bitti", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        status.level == PantryLevel.LOW ->
            Triple("Kritik", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        status.expiryLevel == ExpiryLevel.EXPIRED ->
            Triple("SKT geçti", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        status.expiryLevel == ExpiryLevel.SOON ->
            Triple("SKT yakın", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Triple("Yeterli", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(6.dp), color = container) {
        Text(
            text,
            color = content,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(28.dp),
        shape = CircleShape
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(14.dp))
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
        Text(
            "Kiler boş",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Evde takip etmek istediğin ürünleri ekle: stok kritik eşiğin altına düşünce " +
                "tek dokunuşla alışveriş listesine geçer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(onClick = onAdd, modifier = Modifier.padding(top = 18.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Kiler ürünü ekle")
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
        title = { Text("Kiler ürünü ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ürün adı") },
                    placeholder = { Text("örn. süt, yumurta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { value -> quantityText = value.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text("Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { value -> thresholdText = value.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text("Kritik eşik") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Birim") },
                    placeholder = { Text("Adet, L, kg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        expiryDate?.let { "SKT: ${dateLabel(it)}" } ?: "Son kullanma tarihi seç (isteğe bağlı)"
                    )
                }

                if (stores.isNotEmpty()) {
                    Text("Market", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = storeId == 0L,
                                onClick = { storeId = 0L },
                                label = { Text("Yok") }
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
                    Text("Kategori", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = categoryId == 0L,
                                onClick = { categoryId = 0L },
                                label = { Text("Otomatik") }
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
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
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
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") }
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
        title = { Text("Kritik eşik", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "${row.name} bu miktara düşünce listeye eklenmesi önerilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Eşik (${row.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.replace(',', '.').toDoubleOrNull() ?: 0.0) }) {
                Text("Kaydet")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
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

private fun expiryLabel(epochMs: Long): String =
    DateTimeFormatter.ofPattern("d MMM", Locale.GERMANY)
        .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate())
        .let { "SKT $it" }
