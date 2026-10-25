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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.shoppingapp.money.CurrencyStore
import com.shoppingapp.R
import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.LocalAppUiStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Hangi hesaba, ekleme mi harcama mı yapılacağını taşır. */
private data class MoneyAction(val account: String, val adding: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onOpenPfand: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val balances = state.balances
    val uiStyle = LocalAppUiStyle.current

    var action by remember { mutableStateOf<MoneyAction?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val addCash: () -> Unit = { action = MoneyAction(MoneyAccount.CASH, adding = true) }
    val subtractCash: () -> Unit = { action = MoneyAction(MoneyAccount.CASH, adding = false) }
    val addBank: () -> Unit = { action = MoneyAction(MoneyAccount.BANK, adding = true) }
    val subtractBank: () -> Unit = { action = MoneyAction(MoneyAccount.BANK, adding = false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.s_cebim), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.s_nakit_banka_ve_pfand_birikimi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenReport) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = stringResource(R.string.report_title)
                        )
                    }
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { confirmReset = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.s_cebim_hareketlerini_sifirla),
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
        if (uiStyle == AppUiStyle.GALLERY) {
            SavingsGalleryContent(
                state = state,
                balances = balances,
                modifier = Modifier.fillMaxSize().padding(padding),
                addCash = addCash,
                subtractCash = subtractCash,
                addBank = addBank,
                subtractBank = subtractBank,
                onOpenPfand = onOpenPfand,
                onDeleteEntry = viewModel::deleteEntry
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = if (uiStyle == AppUiStyle.COMPACT) 10.dp else 16.dp,
                    end = if (uiStyle == AppUiStyle.COMPACT) 10.dp else 16.dp,
                    top = if (uiStyle == AppUiStyle.COMPACT) 4.dp else 0.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (uiStyle == AppUiStyle.COMPACT) 3.dp else 10.dp
                )
            ) {
                item { TotalCard(balances, uiStyle) }

                item {
                    AccountCard(
                        title = stringResource(R.string.money_cash),
                        subtitle = stringResource(R.string.money_cash_sub),
                        icon = Icons.Default.Payments,
                        balance = balances.cash,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = uiStyle,
                        onAdd = addCash,
                        onSubtract = subtractCash
                    )
                }

                item {
                    AccountCard(
                        title = stringResource(R.string.money_bank),
                        subtitle = stringResource(R.string.money_bank_sub),
                        icon = Icons.Default.AccountBalance,
                        balance = balances.bank,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = uiStyle,
                        onAdd = addBank,
                        onSubtract = subtractBank
                    )
                }

                item {
                    PfandCard(
                        total = balances.pfand,
                        units = state.pendingPfandUnits,
                        style = uiStyle,
                        onOpen = onOpenPfand
                    )
                }

                if (state.entries.isNotEmpty()) {
                    item {
                        SectionTitle(
                            stringResource(R.string.money_recent),
                            stringResource(R.string.money_recent_count, state.entries.size),
                            uiStyle
                        )
                    }
                    items(state.entries.take(50), key = { it.id }) { entry ->
                        MovementRow(
                            entry = entry,
                            style = uiStyle,
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                } else {
                    item { EmptyMovements(uiStyle) }
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
            title = { Text(stringResource(R.string.s_cebim_hareketlerini_sifirla)) },
            text = {
                Text(stringResource(R.string.s_nakit_ve_banka_bakiyelerini_olusturan_tum_hareke)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetMoney()
                        confirmReset = false
                    }
                ) { Text(stringResource(R.string.s_sifirla)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.s_vazgec)) }
            }
        )
    }
}

@Composable
private fun TotalCard(balances: MoneyBalances, style: AppUiStyle) {
    when (style) {
        AppUiStyle.MODERN -> ModernTotalCard(balances)
        AppUiStyle.CLASSIC -> ClassicTotalCard(balances)
        AppUiStyle.COMPACT -> CompactTotalCard(balances)
        AppUiStyle.GALLERY -> GalleryTotalCard(balances)
    }
}

@Composable
private fun ModernTotalCard(balances: MoneyBalances) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = MaterialTheme.shapes.large,
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
                    Text(stringResource(R.string.s_cebimdeki_toplam),
                        style = MaterialTheme.typography.labelLarge,
                        color = content.copy(alpha = 0.75f)
                    )
                    Text(
                        money(balances.total),
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                }
            }

            Text(
                text = if (balances.pfand > 0.0) {
                    stringResource(R.string.savings_total_pfand, money(balances.pfand))
                } else {
                    stringResource(R.string.savings_total_hint)
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
                Breakdown(stringResource(R.string.money_cash), balances.cash, content, Modifier.weight(1f))
                BreakdownDivider(content)
                Breakdown(stringResource(R.string.money_bank), balances.bank, content, Modifier.weight(1f))
                BreakdownDivider(content)
                Breakdown(stringResource(R.string.s_pfand), balances.pfand, content, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClassicTotalCard(balances: MoneyBalances) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.s_cebimdeki_toplam).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                money(balances.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            LedgerLine(stringResource(R.string.money_cash), balances.cash)
            LedgerLine(stringResource(R.string.money_bank), balances.bank)
            LedgerLine(stringResource(R.string.s_pfand), balances.pfand)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = if (balances.pfand > 0.0) {
                    stringResource(R.string.savings_total_pfand, money(balances.pfand))
                } else {
                    stringResource(R.string.savings_total_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun LedgerLine(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            money(value),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactTotalCard(balances: MoneyBalances) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.s_cebimdeki_toplam),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    money(balances.total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                CompactBreakdown(stringResource(R.string.money_cash), balances.cash)
                CompactBreakdown(stringResource(R.string.money_bank), balances.bank)
                CompactBreakdown(stringResource(R.string.s_pfand), balances.pfand)
            }
        }
    }
}

@Composable
private fun CompactBreakdown(label: String, value: Double) {
    Text(
        "$label ${money(value)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        maxLines = 1
    )
}

@Composable
private fun GalleryTotalCard(balances: MoneyBalances) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        val content = MaterialTheme.colorScheme.onPrimaryContainer
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.s_cebimdeki_toplam).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.7f)
                    )
                    Text(
                        money(balances.total),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = content)
                }
            }
            Text(
                text = if (balances.pfand > 0.0) {
                    stringResource(R.string.savings_total_pfand, money(balances.pfand))
                } else {
                    stringResource(R.string.savings_total_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Breakdown(stringResource(R.string.money_cash), balances.cash, content, Modifier.weight(1f))
                BreakdownDivider(content)
                Breakdown(stringResource(R.string.money_bank), balances.bank, content, Modifier.weight(1f))
                BreakdownDivider(content)
                Breakdown(stringResource(R.string.s_pfand), balances.pfand, content, Modifier.weight(1f))
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
            money(value),
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
    style: AppUiStyle,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    when (style) {
        AppUiStyle.MODERN -> ModernAccountCard(
            title, subtitle, icon, balance, containerColor, contentColor, onAdd, onSubtract
        )

        AppUiStyle.CLASSIC -> ClassicAccountCard(
            title, subtitle, icon, balance, containerColor, contentColor, onAdd, onSubtract
        )

        AppUiStyle.COMPACT -> CompactAccountCard(
            title, subtitle, icon, balance, onAdd, onSubtract
        )

        AppUiStyle.GALLERY -> GalleryAccountCard(
            title, subtitle, icon, balance, containerColor, contentColor, onAdd, onSubtract
        )
    }
}

@Composable
private fun ModernAccountCard(
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
        shape = MaterialTheme.shapes.large,
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
                    money(balance),
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
                    contentDescription = stringResource(R.string.p_x_bakiyesinden_dus, title),
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
                    contentDescription = stringResource(R.string.p_x_bakiyesine_ekle, title),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ClassicAccountCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    balance: Double,
    containerColor: Color,
    contentColor: Color,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    title.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    money(balance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedIconButton(
                onClick = onSubtract,
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(R.string.p_x_bakiyesinden_dus, title),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.p_x_bakiyesine_ekle, title),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactAccountCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    balance: Double,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                money(balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onSubtract, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(R.string.p_x_bakiyesinden_dus, title),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.p_x_bakiyesine_ekle, title),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GalleryAccountCard(
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(19.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                money(balance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedIconButton(
                    onClick = onSubtract,
                    modifier = Modifier.size(34.dp),
                    border = BorderStroke(1.dp, contentColor.copy(alpha = 0.4f)),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = contentColor)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.p_x_bakiyesinden_dus, title),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = contentColor,
                        contentColor = containerColor
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.p_x_bakiyesine_ekle, title),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PfandCard(total: Double, units: Int, style: AppUiStyle, onOpen: () -> Unit) {
    when (style) {
        AppUiStyle.MODERN -> ModernPfandCard(total, units, onOpen)
        AppUiStyle.CLASSIC -> ClassicPfandCard(total, units, onOpen)
        AppUiStyle.COMPACT -> CompactPfandCard(total, units, onOpen)
        AppUiStyle.GALLERY -> GalleryPfandCard(total, units, onOpen)
    }
}

@Composable
private fun ModernPfandCard(total: Double, units: Int, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
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
                Text(stringResource(R.string.s_pfand), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (units > 0) {
                        stringResource(R.string.savings_pfand_waiting, units)
                    } else {
                        stringResource(R.string.savings_pfand_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                money(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.s_pfand_detaylari),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClassicPfandCard(total: Double, units: Int, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Redeem,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    stringResource(R.string.s_pfand).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = if (units > 0) {
                        stringResource(R.string.savings_pfand_waiting, units)
                    } else {
                        stringResource(R.string.savings_pfand_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                money(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.s_pfand_detaylari),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactPfandCard(total: Double, units: Int, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Redeem,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.s_pfand),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp)
            )
            Text(
                text = if (units > 0) {
                    stringResource(R.string.savings_pfand_waiting, units)
                } else {
                    stringResource(R.string.savings_pfand_none)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            )
            Text(
                money(total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.s_pfand_detaylari),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GalleryPfandCard(total: Double, units: Int, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Redeem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.s_pfand), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (units > 0) {
                        stringResource(R.string.savings_pfand_waiting, units)
                    } else {
                        stringResource(R.string.savings_pfand_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                money(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.s_pfand_detaylari),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MovementRow(entry: MoneyEntry, style: AppUiStyle, onDelete: () -> Unit) {
    when (style) {
        AppUiStyle.MODERN -> ModernMovementRow(entry, onDelete)
        AppUiStyle.CLASSIC -> ClassicMovementRow(entry, onDelete)
        AppUiStyle.COMPACT -> CompactMovementRow(entry, onDelete)
        AppUiStyle.GALLERY -> GalleryMovementRow(entry, onDelete)
    }
}

@Composable
private fun ModernMovementRow(entry: MoneyEntry, onDelete: () -> Unit) {
    val positive = entry.amount >= 0.0
    val tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val accountLabel = accountLabel(entry.account)
    val date = remember(entry.createdAt) {
        SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(entry.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
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
                    stringResource(R.string.p_x_x, accountLabel, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                signedMoney(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.s_hareketi_sil),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ClassicMovementRow(entry: MoneyEntry, onDelete: () -> Unit) {
    val positive = entry.amount >= 0.0
    val tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val accountLabel = accountLabel(entry.account)
    val date = movementDate(entry)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.note.ifBlank { accountLabel },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.p_x_x, accountLabel, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                signedMoney(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.s_hareketi_sil),
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CompactMovementRow(entry: MoneyEntry, onDelete: () -> Unit) {
    val positive = entry.amount >= 0.0
    val tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val accountLabel = accountLabel(entry.account)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.note.ifBlank { accountLabel },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.p_x_x, accountLabel, movementDate(entry)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            signedMoney(entry.amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.s_hareketi_sil),
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun GalleryMovementRow(entry: MoneyEntry, onDelete: () -> Unit) {
    val positive = entry.amount >= 0.0
    val tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val accountLabel = accountLabel(entry.account)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    entry.note.ifBlank { accountLabel },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.s_hareketi_sil),
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                signedMoney(entry.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                stringResource(R.string.p_x_x, accountLabel, movementDate(entry)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun movementDate(entry: MoneyEntry): String =
    SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(entry.createdAt))

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
            Text(
                if (action.adding) {
                    stringResource(R.string.money_add_to, label)
                } else {
                    stringResource(R.string.money_subtract_from, label)
                }
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.p_su_anki_bakiye_x, money(currentBalance)),
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
                    label = { Text(stringResource(R.string.s_tutar_euro, CurrencyStore.symbol())) },
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
                    label = { Text(stringResource(R.string.s_not_istege_bagli)) },
                    placeholder = { Text(stringResource(R.string.s_orn_maas_kira_harclik)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.p_islemden_sonra_x, money(after)),
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
                    Text(stringResource(R.string.s_bakiye_eksiye_dusecek_yine_de_kaydedebilirsin),
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
                Text(
                    if (action.adding) {
                        stringResource(R.string.s_ekle)
                    } else {
                        stringResource(R.string.action_subtract)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s_vazgec)) }
        }
    )
}

@Composable
private fun SectionTitle(title: String, trailing: String, style: AppUiStyle) {
    when (style) {
        AppUiStyle.CLASSIC -> Column(modifier = Modifier.padding(top = 10.dp)) {
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
                    trailing,
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
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 1.dp),
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (style == AppUiStyle.GALLERY) 14.dp else 12.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = if (style == AppUiStyle.GALLERY) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleSmall
                },
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
}

@Composable
private fun EmptyMovements(style: AppUiStyle) {
    Text(
        text = stringResource(R.string.s_henuz_hareket_yok_nakit_veya_banka_kartindaki_il),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = if (style == AppUiStyle.COMPACT) 14.dp else 24.dp
            )
    )
}

@Composable
private fun SavingsGalleryContent(
    state: SavingsUiState,
    balances: MoneyBalances,
    modifier: Modifier = Modifier,
    addCash: () -> Unit,
    subtractCash: () -> Unit,
    addBank: () -> Unit,
    subtractBank: () -> Unit,
    onOpenPfand: () -> Unit,
    onDeleteEntry: (MoneyEntry) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "total") {
            GalleryTotalCard(balances)
        }
        item(key = "cash") {
            GalleryAccountCard(
                title = stringResource(R.string.money_cash),
                subtitle = stringResource(R.string.money_cash_sub),
                icon = Icons.Default.Payments,
                balance = balances.cash,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onAdd = addCash,
                onSubtract = subtractCash
            )
        }
        item(key = "bank") {
            GalleryAccountCard(
                title = stringResource(R.string.money_bank),
                subtitle = stringResource(R.string.money_bank_sub),
                icon = Icons.Default.AccountBalance,
                balance = balances.bank,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onAdd = addBank,
                onSubtract = subtractBank
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "pfand") {
            GalleryPfandCard(
                total = balances.pfand,
                units = state.pendingPfandUnits,
                onOpen = onOpenPfand
            )
        }
        if (state.entries.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent") {
                SectionTitle(
                    stringResource(R.string.money_recent),
                    stringResource(R.string.money_recent_count, state.entries.size),
                    AppUiStyle.GALLERY
                )
            }
            gridItems(state.entries.take(50), key = { it.id }) { entry ->
                GalleryMovementRow(entry = entry, onDelete = { onDeleteEntry(entry) })
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                EmptyMovements(AppUiStyle.GALLERY)
            }
        }
    }
}

@Composable
private fun accountLabel(account: String): String = when (account) {
    MoneyAccount.BANK -> stringResource(R.string.money_bank)
    else -> stringResource(R.string.money_cash)
}
