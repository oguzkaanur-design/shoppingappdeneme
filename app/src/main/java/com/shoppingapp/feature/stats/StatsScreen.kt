package com.shoppingapp.feature.stats

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.shoppingapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val statsState by viewModel.statsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.s_istatistikler), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.s_geri))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatCard(
                    title = stringResource(R.string.stats_total_spending),
                    value = euro(statsState.totalSpending),
                    subtitle = stringResource(R.string.stats_purchase_count, statsState.purchaseCount),
                    icon = Icons.Default.ShoppingCart,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            item {
                BudgetCard(state = statsState, onSave = viewModel::setMonthlyLimit)
            }
            item {
                StatCard(
                    title = stringResource(R.string.stats_pfand_title),
                    value = euro(statsState.pfandTotal),
                    subtitle = stringResource(R.string.stats_pfand_waiting, statsState.pfandUnits),
                    icon = Icons.Default.Refresh,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            item {
                StatCard(
                    title = stringResource(R.string.stats_active_title),
                    value = stringResource(R.string.stats_active_value, statsState.activeItemCount),
                    subtitle = stringResource(R.string.stats_purchased_before, statsState.purchasedItemCount),
                    icon = Icons.Default.Star,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (!statsState.hasHistory) {
                item { EmptyHistoryNotice() }
            } else {
                item { SpendingChartCard(statsState) }
                if (statsState.categories.isNotEmpty()) {
                    item { CategoryCard(statsState.categories) }
                }
                if (statsState.stores.isNotEmpty()) {
                    item { StoreCard(statsState) }
                }
                if (statsState.trends.isNotEmpty()) {
                    item { PriceTrendCard(statsState.trends) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpendingChartCard(state: StatsUiState) {
    var monthly by remember { mutableStateOf(false) }
    val periods = if (monthly) state.monthly else state.weekly

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.s_harcama_grafigi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (monthly) "Son ${periods.size} ay" else "Son ${periods.size} hafta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilterChip(
                    selected = !monthly,
                    onClick = { monthly = false },
                    label = { Text(stringResource(R.string.s_hafta)) }
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = monthly,
                    onClick = { monthly = true },
                    label = { Text(stringResource(R.string.s_ay)) }
                )
            }
            SpendBarChart(periods = periods, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/** Compose Canvas ile çizilen basit sütun grafiği: ek kütüphane gerektirmez. */
@Composable
fun SpendBarChart(periods: List<PeriodSpend>, modifier: Modifier = Modifier) {
    if (periods.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val max = periods.maxOf { it.total }.coerceAtLeast(0.01)

    Canvas(modifier = modifier.fillMaxWidth().height(178.dp)) {
        val labelHeight = 20.dp.toPx()
        val valueSpace = 16.dp.toPx()
        val chartHeight = size.height - labelHeight - valueSpace
        val slot = size.width / periods.size
        val barWidth = (slot * 0.52f).coerceAtMost(38.dp.toPx())
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        periods.forEachIndexed { index, period ->
            val centerX = slot * index + slot / 2f
            val barHeight = (period.total / max).toFloat().coerceIn(0f, 1f) * chartHeight

            drawRoundRect(
                color = trackColor.copy(alpha = 0.45f),
                topLeft = Offset(centerX - barWidth / 2f, valueSpace),
                size = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, valueSpace + chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
                paint.color = valueColor.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.GERMANY, "%.0f", period.total),
                    centerX,
                    (valueSpace + chartHeight - barHeight - 4.dp.toPx()).coerceAtLeast(10.sp.toPx()),
                    paint
                )
            }
            paint.color = labelColor.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                period.label,
                centerX,
                size.height - 4.dp.toPx(),
                paint
            )
        }
    }
}

@Composable
private fun CategoryCard(categories: List<CategorySlice>) {
    AnalyticsCard(
        title = stringResource(R.string.stats_category_title),
        hint = stringResource(R.string.stats_category_hint)
    ) {
        categories.take(6).forEach { category ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.p_x_3, amount(category.total)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "  %${(category.share * 100).toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = category.share.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StoreCard(state: StatsUiState) {
    AnalyticsCard(
        title = stringResource(R.string.stats_store_title),
        hint = stringResource(R.string.stats_store_hint)
    ) {
        state.stores.forEach { store ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(parseColor(store.colorHex))
                )
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(store.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.p_x_alisveris_x_toplam, store.visits, amount(store.total)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.p_x_3, amount(store.averageBasket)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.cheapestBasket?.storeId == store.storeId) {
                        Text(stringResource(R.string.s_en_dusuk_sepet),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceTrendCard(trends: List<PriceTrend>) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.GERMANY) }
    AnalyticsCard(
        title = stringResource(R.string.stats_price_title),
        hint = stringResource(R.string.stats_price_hint)
    ) {
        trends.forEach { trend ->
            val change = trend.change
            val up = (change ?: 0.0) > 0.0
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        trend.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                    Text(
                        buildString {
                            append(stringResource(R.string.stats_trend_last, amount(trend.lastPrice)))
                            trend.previousPrice?.let {
                                append(" · ")
                                append(stringResource(R.string.stats_trend_previous, amount(it)))
                            }
                            append(" · ${dateFormat.format(Date(trend.lastPurchaseAt))}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (change != null) {
                    Icon(
                        if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (up) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        trend.changePercent?.let { String.format(Locale.GERMANY, "%+.0f%%", it) } ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (up) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    hint: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EmptyHistoryNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.s_grafikler_icin_henuz_veri_yok), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.stats_empty_history),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp).padding(end = 16.dp),
                tint = contentColor
            )
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.8f))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = contentColor)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Aylık bütçe kartı: bu ayın harcaması, limit ve kalan tutar. Limit yoksa
 * kart kullanıcıyı tek dokunuşla limit belirlemeye yönlendirir.
 */
@Composable
private fun BudgetCard(state: StatsUiState, onSave: (Double) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val overBudget = state.hasBudget && state.budgetRemaining < 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.s_aylik_butce), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.hasBudget) {
                            "${euro(state.monthSpending)} / ${euro(state.monthlyLimit)} · bu ay"
                        } else {
                            "Bu ay ${euro(state.monthSpending)} harcandı"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.s_butceyi_duzenle),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.hasBudget) {
                LinearProgressIndicator(
                    progress = state.budgetProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    if (overBudget) {
                        "${euro(-state.budgetRemaining)} bütçeyi aştın"
                    } else {
                        "${euro(state.budgetRemaining)} kaldı"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                Text(stringResource(R.string.s_aylik_limit_koy_harcama_bu_kartta_ilerleme_olara),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            current = state.monthlyLimit,
            onDismiss = { showDialog = false },
            onSave = { value ->
                onSave(value)
                showDialog = false
            }
        )
    }
}

@Composable
private fun BudgetDialog(current: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember {
        mutableStateOf(
            if (current > 0.0) {
                if (current % 1.0 == 0.0) current.toInt().toString()
                else String.format(Locale.GERMANY, "%.2f", current).replace('.', ',')
            } else {
                ""
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s_aylik_butce), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { value ->
                        text = value.filter { it.isDigit() || it == ',' || it == '.' }
                    },
                    label = { Text(stringResource(R.string.s_limit_euro)) },
                    placeholder = { Text(stringResource(R.string.s_orn_250)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text(stringResource(R.string.s_bos_birakip_kaydedersen_butce_takibi_kapanir),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text.replace(',', '.').toDoubleOrNull() ?: 0.0)
                }
            ) { Text(stringResource(R.string.s_kaydet)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s_vazgec)) }
        }
    )
}

private fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF047857))

private fun euro(value: Double): String = String.format(Locale.GERMANY, "€%.2f", value)

private fun amount(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
