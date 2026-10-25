package com.shoppingapp.feature.scanner

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.shoppingapp.R
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
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
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

    // Kamera önizlemesi açık renkli olabildiği için durum çubuğu ikonları koyu
    // kalamaz; tarayıcı açıkken beyaz ikonlar kullanılır, çıkışta geri alınır.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previousLightStatusBars = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previousLightStatusBars }
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

    // İzin yokken yalnızca izin ekranı çizilir; tarama kontrolleri altta kalırsa üst üste biner.
    if (!hasPermission) {
        PermissionRequest(
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Üst kontrollerin ve durum çubuğu ikonlarının okunabilirliği için hafif karartma.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        1f to Color.Transparent
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.s_kapat), tint = Color.White)
                }
                Text(text = stringResource(R.string.s_barkod_tara),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showManualEntry = true }) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = stringResource(R.string.s_barkodu_elle_gir),
                        tint = Color.White
                    )
                }
                IconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = stringResource(R.string.s_fener),
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
                    Text(text = stringResource(R.string.s_barkodu_cerceveye_getir_urunu_bulunca_hangi_mark),
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
                            Text(text = stringResource(R.string.s_urun_araniyor),
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
                                                text = stringResource(
                                                    R.string.scanner_last_price,
                                                    String.format(Locale.GERMANY, "%.2f", last.priceEuro),
                                                    last.storeName?.let { " · $it" }.orEmpty(),
                                                    date
                                                ),
                                                color = Color.White.copy(alpha = 0.8f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(text = stringResource(R.string.s_hangi_marketten_aldin),
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
                                            label = { Text(stringResource(R.string.s_market_yok)) },
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
                                        Text(stringResource(R.string.s_vazgec), color = Color.White.copy(alpha = 0.75f))
                                    }
                                    Button(
                                        onClick = { viewModel.confirmAdd(selectedStoreId) },
                                        modifier = Modifier.weight(1.8f)
                                    ) {
                                        Text(
                                            if (selectedStoreId == 0L) {
                                                stringResource(R.string.s_listeye_ekle)
                                            } else {
                                                stringResource(
                                                    R.string.s_listeye_ekle_x,
                                                    storeName(stores, selectedStoreId)
                                                )
                                            }
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
                                    text = stringResource(R.string.p_listeye_eklendi_x_x_urun, storeName(stores, state.lastAdded!!.storeId), state.addedCount),
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
                            Text(text = stringResource(R.string.s_kamerayi_bir_barkoda_dogrult),
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
                            Text(
                                if (state.addedCount > 0) {
                                    stringResource(R.string.p_bitir_x, state.addedCount)
                                } else {
                                    stringResource(R.string.s_kapat)
                                }
                            )
                        }
                    } else {
                        Text(text = stringResource(R.string.s_secilen_market_urunun_etiketinde_gorunur),
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
            title = { Text(stringResource(R.string.s_barkodu_elle_gir)) },
            text = {
                Column {
                    Text(stringResource(R.string.s_kamera_okumazsa_barkod_numarasini_yaz_urun_yine_),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.s_barkod_ean)) },
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
                ) { Text(stringResource(R.string.s_ara)) }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntry = false }) { Text(stringResource(R.string.s_iptal)) }
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

@Composable
private fun storeName(stores: List<Store>, storeId: Long): String =
    stores.firstOrNull { it.id == storeId }?.name
        ?: stringResource(R.string.s_market_yok)

@Composable
private fun PermissionRequest(onRequest: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
        Text(text = stringResource(R.string.s_barkod_taramak_icin_kamera_izni_gerekiyor),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp)
        )
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.s_izin_ver))
        }
        Text(text = stringResource(R.string.s_vazgec),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable { onClose() }
                .padding(8.dp)
        )
    }
}
