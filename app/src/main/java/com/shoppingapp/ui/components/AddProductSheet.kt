package com.shoppingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.shoppingapp.data.catalog.BrandCatalog
import com.shoppingapp.data.model.Category
import com.shoppingapp.data.model.Store

/**
 * Ürün ekleme sayfası. Akış bilerek "önce market, sonra ürün adı" sırasında:
 * market seçilince o marketin kendi markalarıyla (MILSANI, Milbona, ja! …)
 * öneriler çıkar, market seçilmezse yazılan ad markasız eklenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductSheet(
    stores: List<Store>,
    categories: List<Category>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        storeId: Long,
        categoryId: Long,
        qty: Double,
        unit: String,
        pfand: Double,
        price: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedStoreId by remember { mutableStateOf(0L) }
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var quantityText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Adet") }
    var pfandText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }

    val qty = quantityText.replace(',', '.').toDoubleOrNull()
    val pfand = pfandText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val nameError = touched && name.isBlank()
    val qtyError = touched && (qty == null || qty <= 0.0)

    val suggestions = remember(name, selectedStoreId) {
        BrandCatalog.suggest(selectedStoreId.takeIf { it > 0L }, name)
    }
    val selectedStore = stores.firstOrNull { it.id == selectedStoreId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("Ürün ekle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Önce marketi seç: o marketin markalarıyla öneri gelir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1) Market
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Market", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        StoreChipSmall(
                            label = "Market yok",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            selected = selectedStoreId == 0L,
                            onClick = { selectedStoreId = 0L }
                        )
                    }
                    items(stores, key = { it.id }) { store ->
                        StoreChipSmall(
                            label = store.name,
                            color = parseColorOr(store.colorHex),
                            selected = selectedStoreId == store.id,
                            onClick = { selectedStoreId = store.id }
                        )
                    }
                }
            }

            // 2) Ürün adı + öneriler
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ürün adı") },
                    placeholder = { Text("örn. yoğurt, süt, mineralwasser") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError,
                    supportingText = when {
                        nameError -> {
                            { Text("Ürün adı gerekli") }
                        }

                        suggestions.isNotEmpty() -> {
                            { Text("${selectedStore?.name} · ${suggestions.size} öneri") }
                        }

                        selectedStore == null -> {
                            { Text("Marka önerileri için yukarıdan market seç.") }
                        }

                        else -> null
                    }
                )

                suggestions.forEach { suggestion ->
                    SuggestionRow(
                        title = suggestion.name,
                        brand = suggestion.brand,
                        onClick = {
                            name = suggestion.name
                            selectedCategoryId = suggestion.categoryId
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { value ->
                        quantityText = value.filter { it.isDigit() || it == ',' || it == '.' }
                    },
                    label = { Text("Miktar") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = qtyError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Birim") },
                    placeholder = { Text("kg, L, Adet") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { value ->
                        priceText = value.filter { it.isDigit() || it == ',' || it == '.' }
                    },
                    label = { Text("Fiyat (€)") },
                    placeholder = { Text("0,00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = pfandText,
                    onValueChange = { value ->
                        pfandText = value.filter { it.isDigit() || it == ',' || it == '.' }
                    },
                    label = { Text("Pfand (€)") },
                    placeholder = { Text("0,00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            if (categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Kategori",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (selectedCategoryId == 0L) {
                            "Otomatik: ürün adından tahmin edilir (görsel buna göre seçilir)."
                        } else {
                            "Seçili: ${categories.firstOrNull { it.id == selectedCategoryId }?.nameTr.orEmpty()}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == 0L,
                                onClick = { selectedCategoryId = 0L },
                                label = { Text("Otomatik") },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        items(categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.nameTr) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    touched = true
                    val validQty = qty ?: return@Button
                    if (name.isNotBlank() && validQty > 0.0) {
                        onConfirm(
                            name.trim(),
                            selectedStoreId,
                            selectedCategoryId,
                            validQty,
                            unit.ifBlank { "Adet" },
                            pfand.coerceAtLeast(0.0),
                            (priceText.replace(',', '.').toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Listeye ekle")
            }
        }
    }
}

@Composable
private fun SuggestionRow(title: String, brand: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (brand != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (brand != null) {
                    Text(
                        text = "Eigenmarke · $brand",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Ekle",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreChipSmall(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

private fun parseColorOr(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF047857))
