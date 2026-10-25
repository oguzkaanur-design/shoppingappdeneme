package com.shoppingapp.feature.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.shoppingapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReportTab { MOVEMENTS, PFAND, PANTRY }

/**
 * Cebim, Pfand ve kiler verilerini tablo hâlinde gösterir ve aynı verileri
 * çevrimdışı bir PDF'e yazar. PDF, mevcut dildeki etiketlerle üretilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(ReportTab.MOVEMENTS) }
    var exporting by remember { mutableStateOf(false) }

    val labels = ReportLabels(
        cash = stringResource(R.string.money_cash),
        bank = stringResource(R.string.money_bank),
        date = stringResource(R.string.report_col_date),
        description = stringResource(R.string.report_col_description),
        account = stringResource(R.string.report_col_account),
        amount = stringResource(R.string.report_col_amount),
        type = stringResource(R.string.report_col_type),
        units = stringResource(R.string.report_col_units),
        unitPrice = stringResource(R.string.report_col_unit_price),
        total = stringResource(R.string.s_toplam),
        status = stringResource(R.string.report_col_status),
        pending = stringResource(R.string.report_status_pending),
        returned = stringResource(R.string.report_status_returned),
        product = stringResource(R.string.report_col_product),
        stock = stringResource(R.string.s_stok),
        threshold = stringResource(R.string.s_kritik_esik),
        empty = stringResource(R.string.report_empty)
    )

    val exportPdf: () -> Unit = {
        if (!exporting) {
            exporting = true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val writer = ReportPdfWriter(context)
                        val generated = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
                            .format(Date())
                        writer.write(
                            ReportDocument(
                                brand = context.getString(R.string.app_name),
                                title = context.getString(R.string.report_title),
                                generatedLabel = context.getString(R.string.report_generated, generated),
                                summaryTitle = context.getString(R.string.report_summary),
                                cashLabel = context.getString(R.string.money_cash),
                                bankLabel = context.getString(R.string.money_bank),
                                pfandLabel = context.getString(R.string.s_pfand),
                                totalLabel = context.getString(R.string.s_toplam),
                                movementsTitle = context.getString(R.string.report_movements),
                                pfandTitle = context.getString(R.string.s_pfand),
                                pantryTitle = context.getString(R.string.s_kiler),
                                footer = context.getString(R.string.report_footer),
                                labels = labels,
                                cash = state.balances.cash,
                                bank = state.balances.bank,
                                pfand = state.pfandValue,
                                movements = ReportRows.movements(state.entries, labels),
                                pfandTable = ReportRows.pfand(
                                    state.pendingPfand,
                                    state.returnedPfand,
                                    labels
                                ),
                                pantryTable = ReportRows.pantry(state.pantry, labels)
                            )
                        )
                    }
                }.onSuccess { file ->
                    runCatching { sharePdf(context, file) }
                        .onFailure { snackbar.showSnackbar(context.getString(R.string.report_export_failed)) }
                }.onFailure {
                    snackbar.showSnackbar(context.getString(R.string.report_export_failed))
                }
                exporting = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                title = {
                    Column {
                        Text(stringResource(R.string.report_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.report_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = exportPdf) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = stringResource(R.string.report_export)
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
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SummaryCard(state) }

            item {
                FilledTonalButton(
                    onClick = exportPdf,
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.report_export),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tab == ReportTab.MOVEMENTS,
                        onClick = { tab = ReportTab.MOVEMENTS },
                        label = { Text(stringResource(R.string.report_movements)) }
                    )
                    FilterChip(
                        selected = tab == ReportTab.PFAND,
                        onClick = { tab = ReportTab.PFAND },
                        label = { Text(stringResource(R.string.s_pfand)) }
                    )
                    FilterChip(
                        selected = tab == ReportTab.PANTRY,
                        onClick = { tab = ReportTab.PANTRY },
                        label = { Text(stringResource(R.string.s_kiler)) }
                    )
                }
            }

            item {
                val table = when (tab) {
                    ReportTab.MOVEMENTS -> ReportRows.movements(state.entries, labels)
                    ReportTab.PFAND -> ReportRows.pfand(state.pendingPfand, state.returnedPfand, labels)
                    ReportTab.PANTRY -> ReportRows.pantry(state.pantry, labels)
                }
                DataTable(table)
            }

            item {
                Text(
                    stringResource(R.string.report_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: ReportUiState) {
    val balances = state.balances
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.report_total_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                ReportRows.money(balances.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryValue(stringResource(R.string.money_cash), ReportRows.money(balances.cash))
                SummaryValue(stringResource(R.string.money_bank), ReportRows.money(balances.bank))
                SummaryValue(
                    stringResource(R.string.s_pfand),
                    ReportRows.money(state.pfandValue),
                    hint = stringResource(R.string.report_units_value, state.pendingUnits)
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, hint: String? = null) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DataTable(table: ReportTable) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                table.headers.forEachIndexed { index, header ->
                    Text(
                        header,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (index in table.endAligned) TextAlign.End else TextAlign.Start,
                        modifier = Modifier.weight(table.weights[index])
                    )
                }
            }
            table.rows.forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIndex % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { index, value ->
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (index in table.endAligned) TextAlign.End else TextAlign.Start,
                            color = when {
                                index in table.endAligned && value.startsWith("−") ->
                                    MaterialTheme.colorScheme.error
                                index in table.endAligned && value.startsWith("+") ->
                                    MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (index in table.endAligned) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(table.weights[index])
                        )
                    }
                }
            }
        }
    }
}

private fun sharePdf(context: Context, file: java.io.File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.report_share_title))
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
