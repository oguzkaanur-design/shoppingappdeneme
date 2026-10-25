package com.shoppingapp.feature.receipt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.Locale
import com.shoppingapp.money.CurrencyStore
import com.shoppingapp.money.MoneyFormat
import com.shoppingapp.R

/**
 * Fiş tarama akışı: fotoğraf çek veya galeriden seç → OCR satırları önizle →
 * market seç → alışverişi tamamla. Fiyatlar kaydetmeden önce düzeltilebilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    onClose: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    // Fiyat alanı yazarken her tuşta yeniden biçimlenmesin: metin yerelde tutulur,
    // geçerli sayı olduğunda view model'a yazılır.
    val priceTexts = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(state.lines) {
        priceTexts.clear()
        state.lines.forEachIndexed { index, line ->
            priceTexts[index] = formatPrice(line.unitPrice)
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let(viewModel::analyze)
    }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::analyze) }

    val startCamera = {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraUri = uri
        takePicture.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.s_fis_tara), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.s_geri))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.s_fisin_fotografini_cek_urunler_ve_fiyatlar_okunup),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.analyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.s_fis_okunuyor), style = MaterialTheme.typography.bodyMedium)
                }
            } else if (state.lines.isEmpty()) {
                OutlinedButton(onClick = startCamera, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Text(stringResource(R.string.s_fotograf_cek), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Text(stringResource(R.string.s_galeriden_sec), modifier = Modifier.padding(start = 8.dp))
                }
                state.error?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    stringResource(R.string.p_x_urun_okundu_x, state.lines.size, money(state.sum)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                state.total?.let { receiptTotal ->
                    Text(
                        stringResource(R.string.p_fis_toplami_x, money(receiptTotal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(stringResource(R.string.s_market), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stores, key = { it.id }) { store ->
                        FilterChip(
                            selected = state.storeId == store.id,
                            onClick = { viewModel.selectStore(store.id) },
                            label = { Text(store.name) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(state.lines.size, key = { index -> "$index-${state.lines[index].name}" }) { index ->
                        val line = state.lines[index]
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        line.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2
                                    )
                                    Text(
                                        stringResource(R.string.p_x_2, formatQty(line.quantity)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = priceTexts[index] ?: formatPrice(line.unitPrice),
                                    onValueChange = { value ->
                                        priceTexts[index] = value
                                        value.replace(',', '.').toDoubleOrNull()?.let { parsed ->
                                            viewModel.updatePrice(index, parsed)
                                        }
                                    },
                                    modifier = Modifier.width(104.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    prefix = { Text(CurrencyStore.symbol()) }
                                )
                                IconButton(onClick = { viewModel.removeLine(index) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.s_satiri_sil))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { viewModel.reset(); startCamera() }) {
                        Text(stringResource(R.string.s_yeni_fotograf))
                    }
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier.weight(1f),
                        enabled = state.lines.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.p_alisverisi_tamamla_x, money(state.sum)))
                    }
                }
            }

            if (state.lines.isEmpty() && !state.analyzing && state.error == null) {
                Text(stringResource(R.string.s_ipucu_fisin_tamami_karede_ve_duz_olsun_okunan_sa),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    state.saved?.let { receipt ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaved(); onClose() },
            title = { Text(stringResource(R.string.s_alisveris_kaydedildi), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(
                        R.string.receipt_saved_summary,
                        receipt.itemCount,
                        receipt.storeName,
                        money(receipt.total)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSaved(); onClose() }) { Text(stringResource(R.string.s_tamam)) }
            }
        )
    }
}

private fun formatPrice(value: Double): String =
    String.format(Locale.GERMANY, "%.2f", value).replace('.', ',')

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.GERMANY, "%.1f", value)

private fun money(value: Double): String = MoneyFormat.amount(value)
