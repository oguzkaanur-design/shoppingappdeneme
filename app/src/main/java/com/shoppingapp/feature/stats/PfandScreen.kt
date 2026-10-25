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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.R
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.data.pfand.PfandPlanError
import com.shoppingapp.data.pfand.PfandPlanResult
import com.shoppingapp.data.pfand.PfandReturnPlan
import com.shoppingapp.data.pfand.PfandReturnPlanner
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var unitPrice by remember { mutableStateOf(amount(presets.first().amount)) }
    var quantityText by remember { mutableStateOf("1") }
    val quantity = PfandQuantity.parse(quantityText)
    var confirmReturn by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.s_pfand), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.s_iade_edilecek_sise_teneke_ve_kasalar),
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
                                contentDescription = stringResource(R.string.s_pfand_verilerini_sifirla),
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
                    quantityText = quantityText,
                    onPresetSelected = {
                        selectedPreset = it
                        unitPrice = amount(it.amount)
                    },
                    onUnitPriceChange = { unitPrice = it },
                    onQuantityChange = { quantityText = it },
                    onAdd = {
                        val price = unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                        quantity?.let { viewModel.addEntry(selectedPreset.label, price, it) }
                        quantityText = "1"
                    }
                )
            }

            if (state.pending.isNotEmpty()) {
                item {
                    SectionTitle(
                        stringResource(R.string.pfand_pending_title),
                        stringResource(R.string.pfand_pending_units, state.pendingUnits)
                    )
                }
                items(state.pending, key = { it.id }) { entry ->
                    PfandRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            } else {
                item {
                    Text(text = stringResource(R.string.s_henuz_pfand_eklemedin_yukaridan_tur_secip_adet_g),
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
                            text = stringResource(R.string.p_iade_gecmisi_x, euro(state.returnedTotal)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearReturned() }) {
                            Text(stringResource(R.string.s_temizle))
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
            title = { Text(stringResource(R.string.s_pfand_verilerini_sifirla)) },
            text = {
                Text(
                    stringResource(
                        R.string.pfand_reset_extra,
                        stringResource(
                            R.string.p_bekleyen_x_ve_iade_edilmis_tum_pfand_kay,
                            euro(state.pendingTotal)
                        )
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAll()
                        confirmReset = false
                    }
                ) { Text(stringResource(R.string.s_sifirla)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.s_vazgec)) }
            }
        )
    }

    if (confirmReturn) {
        ReturnPfandDialog(
            entries = state.pending,
            pendingTotal = state.pendingTotal,
            pendingUnits = state.pendingUnits,
            onDismiss = { confirmReturn = false },
            onConfirm = { paidCents ->
                confirmReturn = false
                viewModel.returnAmount(paidCents) { result ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (result.success) {
                                context.getString(
                                    R.string.pfand_return_done,
                                    euro(result.amountCents / 100.0),
                                    result.returnedUnits
                                )
                            } else {
                                context.getString(R.string.pfand_return_failed)
                            }
                        )
                    }
                }
            }
        )
    }
}

/**
 * İade diyaloğu: marketin ödediği tutar girilir, tutara denk gelen şişe/kasa adedi
 * canlı olarak hesaplanır. Yalnızca o adetler iade alınmış sayılır.
 */
@Composable
internal fun ReturnPfandDialog(
    entries: List<PfandEntry>,
    pendingTotal: Double,
    pendingUnits: Int,
    onDismiss: () -> Unit,
    onConfirm: (paidCents: Int) -> Unit
) {
    // Alan, cihazın (veya uygulama içi dil seçiminin) ondalık ayırıcısını gösterir.
    val locale = LocalConfiguration.current.locales[0]
    val separator = remember(locale) { PfandAmount.separatorFor(locale) }
    var amountText by remember { mutableStateOf("") }
    val paidCents = PfandAmount.parseCents(amountText)
    // Kısayolun yazacağı tutar, planlayıcının toplamıyla aynı kaynaktan gelir.
    val pendingCents = remember(entries) {
        entries.filter { !it.isReturned && it.quantity > 0 }
            .sumOf { PfandReturnPlanner.unitCents(it) * it.quantity }
    }
    val plan = remember(entries, paidCents) {
        paidCents?.let { PfandReturnPlanner.plan(entries, it) }
    }
    val planError = (plan as? PfandPlanResult.Error)?.reason
    val invalidInput = amountText.isNotBlank() && paidCents == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s_iadeyi_tamamla)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.pfand_return_prompt),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = PfandAmount.sanitize(it, separator) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PfandScreenTestTags.RETURN_AMOUNT),
                    label = { Text(stringResource(R.string.pfand_return_amount_label)) },
                    placeholder = { Text(stringResource(R.string.pfand_return_amount_placeholder)) },
                    prefix = { Text("€") },
                    singleLine = true,
                    isError = invalidInput || planError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.pfand_return_pending,
                                euro(pendingTotal),
                                pendingUnits
                            )
                        )
                    }
                )
                TextButton(
                    onClick = { amountText = PfandAmount.format(pendingCents, separator) },
                    enabled = pendingCents > 0,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag(PfandScreenTestTags.RETURN_ALL)
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.pfand_return_all))
                }
                Spacer(Modifier.size(10.dp))
                when {
                    invalidInput -> DialogError(stringResource(R.string.pfand_return_invalid_amount))
                    planError != null -> DialogError(planErrorText(planError, paidCents, entries))
                    plan is PfandPlanResult.Ready -> ReturnPlanSummary(plan.plan)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ready = plan as? PfandPlanResult.Ready ?: return@Button
                    onConfirm(ready.plan.amountCents)
                },
                enabled = plan is PfandPlanResult.Ready,
                modifier = Modifier.testTag(PfandScreenTestTags.RETURN_CONFIRM)
            ) { Text(stringResource(R.string.s_iadeyi_al)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s_vazgec)) }
        }
    )
}

@Composable
private fun DialogError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag(PfandScreenTestTags.RETURN_ERROR)
    )
}

@Composable
private fun ReturnPlanSummary(plan: PfandReturnPlan) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.pfand_return_calc,
                    plan.returnedUnits,
                    euro(plan.amountCents / 100.0)
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag(PfandScreenTestTags.RETURN_CALC)
            )
            plan.lines
                .groupBy { it.label to it.unitCents }
                .forEach { (key, lines) ->
                    Text(
                        text = stringResource(
                            R.string.pfand_return_line,
                            key.first,
                            lines.sumOf { it.quantity },
                            euro(key.second / 100.0)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            Text(
                text = stringResource(
                    R.string.pfand_return_remaining,
                    plan.remainingUnits,
                    euro(plan.remainingCents / 100.0)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun planErrorText(
    error: PfandPlanError,
    paidCents: Int?,
    entries: List<PfandEntry>
): String = when (error) {
    PfandPlanError.NO_PENDING -> stringResource(R.string.pfand_return_stale)
    PfandPlanError.INVALID_AMOUNT -> stringResource(R.string.pfand_return_invalid_amount)
    PfandPlanError.EXCEEDS_PENDING -> stringResource(R.string.pfand_return_exceeds)
    PfandPlanError.NO_EXACT_MATCH -> {
        val smallestUnit = entries.minOfOrNull { PfandReturnPlanner.unitCents(it) } ?: 0
        if (paidCents != null && paidCents < smallestUnit) {
            stringResource(R.string.pfand_return_too_little)
        } else {
            stringResource(R.string.pfand_return_no_match)
        }
    }
}

internal object PfandScreenTestTags {
    const val RETURN_AMOUNT = "pfand_return_amount"
    const val RETURN_ALL = "pfand_return_all"
    const val RETURN_CONFIRM = "pfand_return_confirm"
    const val RETURN_CALC = "pfand_return_calc"
    const val RETURN_ERROR = "pfand_return_error"
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
                    Text(stringResource(R.string.s_toplam_pfand_degeri),
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
                text = if (hasEntries) {
                    stringResource(R.string.pfand_waiting_count, units)
                } else {
                    stringResource(R.string.pfand_none_waiting)
                },
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
                    Text(stringResource(R.string.s_iadeyi_al))
                }
                Text(
                    text = stringResource(R.string.p_iade_tamamlaninca_x_cebim_deki_nakit_bak, euro(total)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCard(
    selected: PfandPreset,
    unitPrice: String,
    quantityText: String,
    onPresetSelected: (PfandPreset) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
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
            Text(stringResource(R.string.s_pfand_ekle), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.s_tur_sec_adet_ve_birim_fiyati_istedigin_gibi_duze),
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
                    label = { Text(stringResource(R.string.s_birim_fiyat_euro)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all(Char::isDigit)) onQuantityChange(value)
                    },
                    label = { Text(stringResource(R.string.s_adet)) },
                    singleLine = true,
                    isError = quantityText.isNotEmpty() && PfandQuantity.parse(quantityText) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f)
                )
            }

            Text(
                text = stringResource(R.string.p_toplam_x, euro((unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0) * (PfandQuantity.parse(quantityText) ?: 0))),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(
                onClick = onAdd,
                enabled = PfandQuantity.parse(quantityText) != null && (unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.s_listeye_ekle))
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
                    stringResource(R.string.p_x_x_5, entry.quantity, euro(entry.unitAmount)),
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
                    contentDescription = stringResource(R.string.s_sil),
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
            text = stringResource(R.string.p_x_x_x, entry.label, entry.quantity, euro(entry.unitAmount)),
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
