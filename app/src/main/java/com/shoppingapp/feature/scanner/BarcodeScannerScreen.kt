package com.shoppingapp.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.shoppingapp.ui.components.ProductThumb
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.shoppingapp.data.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onClose: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128
                )
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }
    var selectedStoreId by remember { mutableStateOf(0L) }

    // Yeni bir barkod okunduğunda market seçimi sıfırlanır.
    LaunchedEffect(state.pending?.barcode) {
        selectedStoreId = 0L
    }

    DisposableEffect(Unit) {
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            executor.shutdown()
            scanner.close()
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(executor) { proxy: ImageProxy ->
            val mediaImage = proxy.image
            if (mediaImage == null) {
                proxy.close()
                return@setAnalyzer
            }
            val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let(viewModel::onBarcodeDetected)
                }
                .addOnCompleteListener { proxy.close() }
        }

        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }

    LaunchedEffect(torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PermissionRequest(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onClose = onClose
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
                Text(
                    text = "Barkod tara",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showManualEntry = true }) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Barkodu elle gir",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Fener",
                        tint = if (torchOn) Color(0xFFFFD54F) else Color.White
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .height(190.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                    )
                    Text(
                        text = "Barkodu çerçeveye getir. Ürünü bulunca hangi marketten aldığını seçersin.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.88f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when {
                        state.isLookingUp -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF34D399)
                            )
                            Text(
                                text = "Ürün aranıyor...",
                                color = Color.White,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }

                        state.error != null -> Text(
                            text = state.error.orEmpty(),
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        state.pending != null -> {
                            val pending = state.pending!!
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProductThumb(
                                        product = pending.product,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .weight(1f)
                                    ) {
                                        Text(
                                            text = pending.product.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2
                                        )
                                        pending.lastPrice?.let { last ->
                                            val date = remember(last.purchaseDate) {
                                                SimpleDateFormat("d MMM", Locale.GERMANY)
                                                    .format(Date(last.purchaseDate))
                                            }
                                            Text(
                                                text = buildString {
                                                    append("Son: €")
                                                    append(String.format(Locale.GERMANY, "%.2f", last.priceEuro))
                                                    last.storeName?.let { append(" · $it") }
                                                    append(" · $date")
                                                },
                                                color = Color.White.copy(alpha = 0.8f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            text = "Hangi marketten aldın?",
                                            color = Color(0xFF6EE7B7),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                LazyRow(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedStoreId == 0L,
                                            onClick = { selectedStoreId = 0L },
                                            label = { Text("Market yok") },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = scanChipColors()
                                        )
                                    }
                                    items(stores, key = { it.id }) { store ->
                                        FilterChip(
                                            selected = selectedStoreId == store.id,
                                            onClick = { selectedStoreId = store.id },
                                            label = { Text(store.name) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = scanChipColors()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.dismissPending() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Vazgeç", color = Color.White.copy(alpha = 0.75f))
                                    }
                                    Button(
                                        onClick = { viewModel.confirmAdd(selectedStoreId) },
                                        modifier = Modifier.weight(1.8f)
                                    ) {
                                        Text(
                                            if (selectedStoreId == 0L) "Listeye ekle"
                                            else "Listeye ekle · ${storeName(stores, selectedStoreId)}"
                                        )
                                    }
                                }
                            }
                        }

                        state.lastAdded != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductThumb(
                                product = state.lastAdded!!.product,
                                modifier = Modifier.size(56.dp)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    text = state.lastAdded!!.product.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2
                                )
                                Text(
                                    text = "Listeye eklendi · ${storeName(stores, state.lastAdded!!.storeId)} · ${state.addedCount} ürün",
                                    color = Color(0xFF6EE7B7),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7)
                            )
                            Text(
                                text = "Kamerayı bir barkoda doğrult",
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    if (state.pending == null) {
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(if (state.addedCount > 0) "Bitir (${state.addedCount})" else "Kapat")
                        }
                    } else {
                        Text(
                            text = "Seçilen market ürünün etiketinde görünür.",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showManualEntry) {
        AlertDialog(
            onDismissRequest = { showManualEntry = false },
            title = { Text("Barkodu elle gir") },
            text = {
                Column {
                    Text(
                        "Kamera okumazsa barkod numarasını yaz; ürün yine alınanlara eklenir.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it.filter(Char::isDigit) },
                        label = { Text("Barkod (EAN)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualBarcode.length >= 6) {
                            viewModel.onBarcodeDetected(manualBarcode)
                            manualBarcode = ""
                            showManualEntry = false
                        }
                    }
                ) { Text("Ara") }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntry = false }) { Text("İptal") }
            }
        )
    }
}

/** Kamera önizlemesinin üstündeki koyu kart için okunabilir çip renkleri. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun scanChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.White.copy(alpha = 0.08f),
    labelColor = Color.White,
    selectedContainerColor = Color(0xFF34D399),
    selectedLabelColor = Color(0xFF052E22)
)

private fun storeName(stores: List<Store>, storeId: Long): String =
    stores.firstOrNull { it.id == storeId }?.name ?: "Market seçilmedi"

@Composable
private fun PermissionRequest(onRequest: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .padding(16.dp)
        )
        Text(
            text = "Barkod taramak için kamera izni gerekiyor",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp)
        )
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text("İzin ver")
        }
        Text(
            text = "Vazgeç",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable { onClose() }
                .padding(8.dp)
        )
    }
}
