package com.shoppingapp.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shoppingapp.R
import com.shoppingapp.data.aisle.AisleGroup
import com.shoppingapp.data.aisle.AisleOrder
import com.shoppingapp.data.model.Category
import com.shoppingapp.data.model.Store
import com.shoppingapp.feature.share.ListShare
import com.shoppingapp.feature.share.ShareLine
import com.shoppingapp.data.repository.PurchaseReceipt
import com.shoppingapp.ui.components.AddProductSheet
import com.shoppingapp.ui.components.ProductThumb
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    onNavigateToStats: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var marketMode by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Alışveriş listesi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "München · ${uiState.pendingItems.size} ürün alınacak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = "İstatistikler",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { marketMode = !marketMode }) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = if (marketMode) "Normal listeye dön" else "Market modu",
                            tint = if (marketMode) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = if (marketMode) {
                                Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(4.dp)
                            } else {
                                Modifier
                            }
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ürün ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Diğer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Listeyi paylaş") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                enabled = uiState.pendingItems.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    shareList(context, uiState)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Son alışverişi tekrarla") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                enabled = uiState.frequent.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    viewModel.repeatLastPurchase()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Yedekleme ve ayarlar") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToBackup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alışveriş verilerini sıfırla") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    showResetDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.items.isNotEmpty()) {
                ExtendedFloatingActionButton(
                onClick = onNavigateToScanner,
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("Barkod tara") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    StoreChip(
                        label = "Tümü",
                        color = MaterialTheme.colorScheme.primary,
                        selected = uiState.selectedStoreId == null,
                        onClick = { viewModel.selectStoreFilter(null) }
                    )
                }
                items(uiState.stores, key = { it.id }) { store ->
                    StoreChip(
                        label = store.name,
                        color = parseColor(store.colorHex),
                        selected = uiState.selectedStoreId == store.id,
                        onClick = { viewModel.selectStoreFilter(store.id) }
                    )
                }
            }

            if (!marketMode && uiState.frequent.isNotEmpty()) {
                FrequentStrip(
                    rows = uiState.frequent,
                    onAdd = viewModel::addFrequent
                )
            }

            if (!marketMode) {
                SummaryCard(
                    pendingCount = uiState.pendingItems.size,
                    purchasedCount = uiState.purchasedItems.size,
                    pfandTotal = uiState.pfandTotal,
                    euroTotal = uiState.euroTotal,
                    onComplete = { showCompleteDialog = true }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                EmptyListState(onScan = onNavigateToScanner, onAdd = { showAddDialog = true })
            } else if (marketMode) {
                MarketModeList(
                    pending = uiState.pendingItems,
                    purchased = uiState.purchasedItems,
                    onToggle = { item -> viewModel.togglePurchased(item.listItem) },
                    onComplete = { showCompleteDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Seçili market varsa ürünler o marketin reyon sırasına göre gruplanır.
                val groups: List<AisleGroup<UiShoppingItem>> = when {
                    uiState.selectedStore != null -> AisleOrder.group(
                        items = uiState.pendingItems,
                        order = uiState.aisleOrder,
                        categoryOf = { it.category?.id }
                    )

                    uiState.pendingItems.isEmpty() -> emptyList()
                    else -> listOf(AisleGroup(1, null, uiState.pendingItems))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    if (uiState.pendingItems.isNotEmpty()) {
                        item { SectionHeader("Alınacaklar", uiState.pendingItems.size) }
                        groups.forEach { group ->
                            if (uiState.selectedStore != null) {
                                item(key = "aisle-${group.aisleNumber}-${group.categoryId}") {
                                    AisleHeader(
                                        number = group.aisleNumber,
                                        title = group.categoryId?.let { id ->
                                            uiState.categories.firstOrNull { it.id == id }?.nameTr
                                        } ?: "Diğer",
                                        count = group.items.size
                                    )
                                }
                            }
                            items(group.items, key = { it.listItem.id }) { item ->
                                ShoppingItemCard(
                                    item = item,
                                    onToggle = { viewModel.togglePurchased(item.listItem) },
                                    onDelete = { viewModel.deleteItem(item.listItem) },
                                    onQuantityChange = { delta -> viewModel.changeQuantity(item.listItem, delta) }
                                )
                            }
                        }
                    }

                    if (uiState.purchasedItems.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Alınanlar (${uiState.purchasedItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearPurchased() }) {
                                    Text("Temizle")
                                }
                            }
                        }
                        items(uiState.purchasedItems, key = { it.listItem.id }) { item ->
                            ShoppingItemCard(
                                item = item,
                                onToggle = { viewModel.togglePurchased(item.listItem) },
                                onDelete = { viewModel.deleteItem(item.listItem) },
                                onQuantityChange = { delta -> viewModel.changeQuantity(item.listItem, delta) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductSheet(
            stores = uiState.stores,
            categories = uiState.categories,
            sheetState = addSheetState,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, storeId, categoryId, qty, unit, pfand, price ->
                viewModel.addNewItem(name, storeId, categoryId, qty, unit, pfand, price)
                showAddDialog = false
            }
        )
    }

    if (showResetDialog) {
        ResetShoppingDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.resetShoppingData()
                showResetDialog = false
            }
        )
    }

    if (showCompleteDialog) {
        CompletePurchaseDialog(
            lines = uiState.pendingItems,
            onDismiss = { showCompleteDialog = false },
            onConfirm = { prices ->
                viewModel.completePurchase(prices)
                showCompleteDialog = false
            }
        )
    }

    receipt?.let {
        ReceiptDialog(receipt = it, onDismiss = viewModel::dismissReceipt)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SummaryCard(
    pendingCount: Int,
    purchasedCount: Int,
    pfandTotal: Double,
    euroTotal: Double,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bugünkü liste",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$pendingCount alınacak · $purchasedCount alındı",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sepet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = euro(euroTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pfand ${euro(pfandTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            if (pendingCount > 0) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Alışverişi tamamla")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun EmptyListState(onScan: () -> Unit, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = "Liste henüz boş",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "Barkod okutarak alınanlara ekle ya da elle ürün gir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(onClick = onScan, modifier = Modifier.padding(top = 20.dp)) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Barkod tara")
        }
        TextButton(onClick = onAdd) { Text("Elle ürün ekle") }
    }
}

@Composable
fun ShoppingItemCard(
    item: UiShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    val isPurchased = item.listItem.isPurchased
    val container = if (isPurchased) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPurchased) 0.dp else 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(start = 4.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isPurchased,
                onCheckedChange = { onToggle() }
            )

            ProductThumb(
                product = item.product,
                modifier = Modifier
                    .size(46.dp)
                    .alpha(if (isPurchased) 0.55f else 1f)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isPurchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    item.store?.let { store ->
                        Tag(
                            text = store.name,
                            background = parseColor(store.colorHex),
                            contentColor = Color.White
                        )
                    }
                    if (item.product.pfandAmount > 0) {
                        Tag(
                            text = "Pfand ${euro(item.product.pfandAmount * item.listItem.quantity)}",
                            background = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    QuantityButton(
                        icon = Icons.Outlined.Remove,
                        enabled = item.listItem.quantity > 1.0,
                        description = "Azalt"
                    ) { onQuantityChange(-1.0) }
                    Text(
                        text = "${quantity(item.listItem.quantity)} ${item.product.unit}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    QuantityButton(
                        icon = Icons.Default.Add,
                        enabled = true,
                        description = "Arttır"
                    ) { onQuantityChange(1.0) }

                    if (item.product.priceEuro > 0) {
                        Text(
                            text = euro(item.product.priceEuro * item.listItem.quantity),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(26.dp),
        shape = CircleShape
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Tag(text: String, background: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = background) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ResetShoppingDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alışveriş verilerini sıfırla", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Şunlar silinir:")
                Text("• Alınacaklar ve alınanlar listesi")
                Text("• Alışveriş geçmişi ve istatistikler")
                Text("• Kayıtlı ürünler ve kaydedilmiş fiyatlar")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Market ve kategori ayarları ile Pfand kayıtları korunur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Sıfırla") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
private fun CompletePurchaseDialog(
    lines: List<UiShoppingItem>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Double>) -> Unit
) {
    val priceTexts = remember {
        mutableStateMapOf<Long, String>().apply {
            lines.forEach { entry ->
                put(
                    entry.listItem.id,
                    if (entry.product.priceEuro > 0.0) amount(entry.product.priceEuro) else ""
                )
            }
        }
    }

    fun priceOf(id: Long): Double =
        priceTexts[id]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0

    val total = lines.sumOf { priceOf(it.listItem.id) * it.listItem.quantity }
    val pfand = lines.sumOf { it.product.pfandAmount * it.listItem.quantity }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Alışverişi tamamla", fontWeight = FontWeight.Bold)
                Text(
                    "${lines.size} kalem · birim fiyatları kontrol et",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lines, key = { it.listItem.id }) { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductThumb(
                                product = entry.product,
                                modifier = Modifier.size(42.dp),
                                cornerRadius = 10.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = entry.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${quantity(entry.listItem.quantity)} ${entry.product.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = priceTexts[entry.listItem.id].orEmpty(),
                                onValueChange = { value ->
                                    priceTexts[entry.listItem.id] = value.filter {
                                        it.isDigit() || it == ',' || it == '.'
                                    }
                                },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(104.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toplam", style = MaterialTheme.typography.titleMedium)
                    Text(
                        euro(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Pfand",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        euro(pfand),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Tamamlayınca liste temizlenir ve harcama geçmişe işlenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = total > 0.0,
                onClick = {
                    onConfirm(lines.associate { it.listItem.id to priceOf(it.listItem.id) })
                }
            ) {
                Text("Alışverişi tamamla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
private fun ReceiptDialog(receipt: PurchaseReceipt, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alışveriş kaydedildi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${receipt.itemCount} ürün · ${receipt.storeName}",
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Toplam")
                    Text(euro(receipt.total), fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pfand", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(euro(receipt.pfand), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Harcama istatistiklere işlendi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Tamam") }
        }
    )
}

@Composable
private fun FrequentStrip(
    rows: List<com.shoppingapp.data.model.FrequentProductRow>,
    onAdd: (com.shoppingapp.data.model.FrequentProductRow) -> Unit
) {
    Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        Text(
            "Sık alınanlar · dokun ekle",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows, key = { it.productId }) { row ->
                AssistChip(
                    onClick = { onAdd(row) },
                    label = {
                        Text(
                            row.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AisleHeader(number: Int, title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                "Reyon $number",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )
        Text(
            "$count ürün",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Market modu: rafta telefonu tek elle kullanmak için büyük dokunma alanları.
 * Satırın tamamı işaretler; küçük +/- düğmeleri yok.
 */
@Composable
private fun MarketModeList(
    pending: List<UiShoppingItem>,
    purchased: List<UiShoppingItem>,
    onToggle: (UiShoppingItem) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Market modu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${pending.size} alınacak · ${purchased.size} alındı · satıra dokun",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = onComplete, enabled = pending.isNotEmpty()) {
                        Text("Tamamla")
                    }
                }
            }
        }

        items(pending, key = { "market-${it.listItem.id}" }) { item ->
            MarketModeRow(item = item, checked = false, onToggle = { onToggle(item) })
        }

        if (purchased.isNotEmpty()) {
            item {
                Text(
                    "Alınanlar (${purchased.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, top = 10.dp)
                )
            }
            items(purchased, key = { "market-${it.listItem.id}" }) { item ->
                MarketModeRow(item = item, checked = true, onToggle = { onToggle(item) })
            }
        }
    }
}

@Composable
private fun MarketModeRow(item: UiShoppingItem, checked: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (checked) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (checked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${quantity(item.listItem.quantity)} ${item.product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    item.store?.let {
                        Text(
                            it.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = parseColor(it.colorHex)
                        )
                    }
                }
            }
        }
    }
}

private fun shareList(context: Context, state: ShoppingUiState) {
    val lines = state.pendingItems.map { entry ->
        ShareLine(
            name = entry.product.name,
            quantity = entry.listItem.quantity,
            unit = entry.product.unit,
            storeName = entry.store?.name,
            purchased = entry.listItem.isPurchased
        )
    }
    val title = state.selectedStore?.let { "Alışveriş listesi · ${it.name}" } ?: "Alışveriş listesi"
    val text = ListShare.text(lines = lines, title = title)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Listeyi paylaş"))
}

private fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF047857))

private fun euro(value: Double): String = String.format(Locale.GERMANY, "€%.2f", value)

private fun amount(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)

private fun quantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.GERMANY, "%.1f", value)
