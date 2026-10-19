package com.shoppingapp.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.data.model.PfandEntry
import java.util.Locale

private data class PfandPreset(
    val label: String,
    val amount: Double,
    val icon: ImageVector
)

private val presets = listOf(
    PfandPreset("Teneke", 0.25, Icons.Default.LocalDrink),
    PfandPreset("Plastik şişe", 0.25, Icons.Default.WaterDrop),
    PfandPreset("Cam şişe (0,08)", 0.08, Icons.Default.WineBar),
    PfandPreset("Cam şişe (0,15)", 0.15, Icons.Default.WineBar),
    PfandPreset("Kasa", 1.50, Icons.Default.Inventory2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PfandScreen(viewModel: PfandViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var unitPrice by remember { mutableStateOf(amount(presets.first().amount)) }
    var quantity by remember { mutableStateOf(1) }
    var confirmReturn by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pfand", fontWeight = FontWeight.Bold)
                        Text(
                            "İade edilecek şişe, teneke ve kasalar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.pending.isNotEmpty() || state.returned.isNotEmpty()) {
                        IconButton(onClick = { confirmReset = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Pfand verilerini sıfırla",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TotalCard(
                    total = state.pendingTotal,
                    units = state.pendingUnits,
                    hasEntries = state.pending.isNotEmpty(),
                    onReturn = { confirmReturn = true }
                )
            }

            item {
                AddCard(
                    selected = selectedPreset,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    onPresetSelected = {
                        selectedPreset = it
                        unitPrice = amount(it.amount)
                    },
                    onUnitPriceChange = { unitPrice = it },
                    onQuantityChange = { quantity = (quantity + it).coerceAtLeast(1) },
                    onAdd = {
                        val price = unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                        viewModel.addEntry(selectedPreset.label, price, quantity)
                        quantity = 1
                    }
                )
            }

            if (state.pending.isNotEmpty()) {
                item { SectionTitle("Bekleyen Pfand", "${state.pendingUnits} adet") }
                items(state.pending, key = { it.id }) { entry ->
                    PfandRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            } else {
                item {
                    Text(
                        text = "Henüz Pfand eklemedin. Yukarıdan tür seçip adet girebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            if (state.returned.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "İade geçmişi · ${euro(state.returnedTotal)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearReturned() }) {
                            Text("Temizle")
                        }
                    }
                }
                items(state.returned.take(20), key = { "r${it.id}" }) { entry ->
                    ReturnedRow(entry)
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Pfand verilerini sıfırla") },
            text = {
                Text(
                    "Bekleyen (${euro(state.pendingTotal)}) ve iade edilmiş tüm Pfand kayıtları silinir. " +
                        "Liste ve alışveriş geçmişi etkilenmez."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAll()
                        confirmReset = false
                    }
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") }
            }
        )
    }

    if (confirmReturn) {
        AlertDialog(
            onDismissRequest = { confirmReturn = false },
            title = { Text("İadeyi tamamla") },
            text = {
                Text(
                    "${euro(state.pendingTotal)} tutarındaki ${state.pendingUnits} adet Pfand iade alındı olarak işaretlensin mi?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.returnAll()
                        confirmReturn = false
                    }
                ) { Text("İade alındı") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReturn = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun TotalCard(total: Double, units: Int, hasEntries: Boolean, onReturn: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "Toplam Pfand değeri",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                    Text(
                        euro(total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = if (hasEntries) "$units adet iade bekliyor" else "Henüz iade bekleyen yok",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )

            if (hasEntries) {
                Button(
                    onClick = onReturn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("İadeyi tamamla")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCard(
    selected: PfandPreset,
    unitPrice: String,
    quantity: Int,
    onPresetSelected: (PfandPreset) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pfand ekle", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tür seç, adet ve birim fiyatı istediğin gibi düzenle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets, key = { it.label }) { preset ->
                    FilterChip(
                        selected = preset.label == selected.label,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset.label) },
                        leadingIcon = {
                            Icon(preset.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { value ->
                        onUnitPriceChange(value.filter { it.isDigit() || it == ',' || it == '.' })
                    },
                    label = { Text("Birim fiyat (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { onQuantityChange(-1) },
                        enabled = quantity > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Azalt", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { onQuantityChange(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Arttır", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = "Toplam: ${euro((unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0) * quantity)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Listeye ekle")
            }
        }
    }
}

@Composable
private fun PfandRow(entry: PfandEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconFor(entry.label),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${entry.quantity} × ${euro(entry.unitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                euro(entry.unitAmount * entry.quantity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun ReturnedRow(entry: PfandEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "${entry.label} · ${entry.quantity} × ${euro(entry.unitAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        Text(
            euro(entry.unitAmount * entry.quantity),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun iconFor(label: String): ImageVector = when {
    label.startsWith("Teneke") -> Icons.Default.LocalDrink
    label.startsWith("Plastik") -> Icons.Default.WaterDrop
    label.startsWith("Cam") -> Icons.Default.WineBar
    else -> Icons.Default.Inventory2
}

private fun euro(value: Double): String = String.format(Locale.GERMANY, "€%.2f", value)

private fun amount(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
