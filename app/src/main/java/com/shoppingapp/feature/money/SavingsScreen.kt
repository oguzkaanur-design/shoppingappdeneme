package com.shoppingapp.feature.money

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Hangi hesaba, ekleme mi harcama mı yapılacağını taşır. */
private data class MoneyAction(val account: String, val adding: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onOpenPfand: () -> Unit = {},
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val balances = state.balances

    var action by remember { mutableStateOf<MoneyAction?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cebim", fontWeight = FontWeight.Bold)
                        Text(
                            "Nakit, banka ve Pfand birikimi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { confirmReset = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Cebim hareketlerini sıfırla",
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
            item { TotalCard(balances) }

            item {
                AccountCard(
                    title = "Nakit",
                    subtitle = "Cüzdandaki para",
                    icon = Icons.Default.Payments,
                    balance = balances.cash,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onAdd = { action = MoneyAction(MoneyAccount.CASH, adding = true) },
                    onSubtract = { action = MoneyAction(MoneyAccount.CASH, adding = false) }
                )
            }

            item {
                AccountCard(
                    title = "Banka",
                    subtitle = "Hesap / kart bakiyesi",
                    icon = Icons.Default.AccountBalance,
                    balance = balances.bank,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onAdd = { action = MoneyAction(MoneyAccount.BANK, adding = true) },
                    onSubtract = { action = MoneyAction(MoneyAccount.BANK, adding = false) }
                )
            }

            item {
                PfandCard(
                    total = balances.pfand,
                    units = state.pendingPfandUnits,
                    onOpen = onOpenPfand
                )
            }

            if (state.entries.isNotEmpty()) {
                item { SectionTitle("Son hareketler", "${state.entries.size} kayıt") }
                items(state.entries.take(50), key = { it.id }) { entry ->
                    MovementRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            } else {
                item {
                    Text(
                        text = "Henüz hareket yok. Nakit veya banka kartındaki + ile ilk bakiyeni ekle; " +
                            "eksi (−) ile harcama düşebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 24.dp)
                    )
                }
            }
        }
    }

    action?.let { pending ->
        MoneyActionDialog(
            action = pending,
            currentBalance = balances.balanceOf(pending.account),
            onDismiss = { action = null },
            onConfirm = { delta, note ->
                viewModel.addMovement(pending.account, delta, note)
                action = null
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Cebim hareketlerini sıfırla") },
            text = {
                Text(
                    "Nakit ve banka bakiyelerini oluşturan tüm hareketler silinir. " +
                        "Pfand kayıtları ve alışveriş listesi etkilenmez."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetMoney()
                        confirmReset = false
                    }
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun TotalCard(balances: MoneyBalances) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        val content = MaterialTheme.colorScheme.onPrimaryContainer
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = content)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "Cebimdeki toplam",
                        style = MaterialTheme.typography.labelLarge,
                        color = content.copy(alpha = 0.75f)
                    )
                    Text(
                        euro(balances.total),
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                }
            }

            Text(
                text = if (balances.pfand > 0.0) {
                    "Nakit + banka + iade bekleyen ${euro(balances.pfand)} Pfand"
                } else {
                    "Nakit + banka · Pfand iadesi eklenince otomatik büyür"
                },
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Breakdown("Nakit", balances.cash, content, Modifier.weight(1f))
                BreakdownDivider(content)
                Breakdown("Banka", balances.bank, content, Modifier.weight(1f))
                BreakdownDivider(content)
                Breakdown("Pfand", balances.pfand, content, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Breakdown(label: String, value: Double, content: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content.copy(alpha = 0.7f)
        )
        Text(
            euro(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}

@Composable
private fun BreakdownDivider(content: Color) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(content.copy(alpha = 0.18f))
    )
}

@Composable
private fun AccountCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    balance: Double,
    containerColor: Color,
    contentColor: Color,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    euro(balance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.65f)
                )
            }

            OutlinedIconButton(
                onClick = onSubtract,
                modifier = Modifier.size(40.dp),
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.4f)),
                colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = contentColor)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "$title bakiyesinden düş",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = contentColor,
                    contentColor = containerColor
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "$title bakiyesine ekle",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PfandCard(total: Double, units: Int, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Redeem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                Text("Pfand", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (units > 0) {
                        "$units adet iade bekliyor · toplama otomatik eklenir"
                    } else {
                        "İade bekleyen Pfand yok"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                euro(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Pfand detayları",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MovementRow(entry: MoneyEntry, onDelete: () -> Unit) {
    val positive = entry.amount >= 0.0
    val tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val accountLabel = accountLabel(entry.account)
    val date = remember(entry.createdAt) {
        SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(entry.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (positive) Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 6.dp)
            ) {
                Text(
                    text = entry.note.ifBlank { accountLabel },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$accountLabel · $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                signedEuro(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Hareketi sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MoneyActionDialog(
    action: MoneyAction,
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (delta: Double, note: String) -> Unit
) {
    val label = accountLabel(action.account)
    var amountText by remember(action) { mutableStateOf("") }
    var note by remember(action) { mutableStateOf("") }

    val parsed = MoneyInput.parse(amountText)
    val delta = when {
        parsed == null -> 0.0
        action.adding -> parsed
        else -> -parsed
    }
    val after = MoneySummary.preview(currentBalance, delta)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (action.adding) "$label bakiyesine ekle" else "$label bakiyesinden düş")
        },
        text = {
            Column {
                Text(
                    "Şu anki bakiye: ${euro(currentBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        if (value.length <= 10 && value.all { it.isDigit() || it == ',' || it == '.' }) {
                            amountText = value
                        }
                    },
                    label = { Text("Tutar (€)") },
                    singleLine = true,
                    isError = amountText.isNotEmpty() && parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 60) note = it },
                    label = { Text("Not (isteğe bağlı)") },
                    placeholder = { Text("örn. maaş, kira, harçlık") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Text(
                    text = "İşlemden sonra: ${euro(after)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (after < 0.0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(top = 10.dp)
                )
                if (after < 0.0) {
                    Text(
                        "Bakiye eksiye düşecek; yine de kaydedebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let { onConfirm(delta, note) } },
                enabled = parsed != null
            ) {
                Text(if (action.adding) "Ekle" else "Düş")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
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
        Text(
            trailing,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun accountLabel(account: String): String = when (account) {
    MoneyAccount.BANK -> "Banka"
    else -> "Nakit"
}
