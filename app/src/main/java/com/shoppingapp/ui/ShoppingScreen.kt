package com.shoppingapp.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.shoppingapp.feature.share.ShareStrings
import com.shoppingapp.feature.share.ShareLine
import com.shoppingapp.data.repository.PurchaseReceipt
import com.shoppingapp.ui.components.AddProductSheet
import com.shoppingapp.ui.components.ProductThumb
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.LocalAppUiStyle
import java.util.Locale

/** Compose UI testlerinin yerelleşmiş metinlere bağlı kalmadan düğüm bulması için. */
internal object ShoppingScreenTestTags {
    const val LIST = "shopping_list"
    const val SUMMARY_COMPLETE = "summary_complete"
    const val COMPACT_COMPLETE = "compact_complete"
    const val COMPLETE_DIALOG = "complete_dialog"
    const val COMPLETE_DIALOG_DISMISS = "complete_dialog_dismiss"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    onNavigateToStats: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToReceipt: () -> Unit = {},
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var marketMode by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiStyle = LocalAppUiStyle.current
    val isGallery = uiStyle == AppUiStyle.GALLERY

    // Yatay telefonda dikey alan kalmadığı için geniş "Barkod tara" FAB'ı
    // özet kartındaki "Alışverişi tamamla" düğmesinin üstüne biniyordu.
    // Kısa ekranlarda tarama eylemi üst çubuğa taşınır, FAB gizlenir.
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenHeightDp < 480

    // Kısa yatay ekranda başlık bloğu listenin içinde kaydırılır. Blok ekrandan
    // çıktıktan sonra "Alışverişi tamamla" eylemi üst çubukta kompakt bir simge
    // olarak kalır; böylece liste sonuna kadar kaydırılsa da eylem erişilebilir.
    val scrollHeader = compactHeight && !isGallery &&
        !uiState.isLoading && uiState.items.isNotEmpty()
    val showCompactComplete = if (isGallery) {
        // Vitrin düzeni kaydırılabilir bir başlık kullanır; tamamlama eylemi
        // üst çubukta her zaman erişilebilir kalır.
        uiState.pendingItems.isNotEmpty()
    } else {
        scrollHeader &&
            uiState.pendingItems.isNotEmpty() &&
            listState.firstVisibleItemIndex > 0
    }

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
                        Text(text = stringResource(R.string.s_alisveris_listesi),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.p_munchen_x_urun_alinacak, uiState.pendingItems.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (showCompactComplete) {
                        FilledTonalIconButton(
                            onClick = { showCompleteDialog = true },
                            modifier = Modifier.testTag(ShoppingScreenTestTags.COMPACT_COMPLETE),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.s_alisverisi_tamamla)
                            )
                        }
                    }
                    if (compactHeight) {
                        IconButton(onClick = onNavigateToScanner) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.s_barkod_tara),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = stringResource(R.string.s_istatistikler),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { marketMode = !marketMode }) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = if (marketMode) {
                                stringResource(R.string.market_mode_exit)
                            } else {
                                stringResource(R.string.s_market_modu)
                            },
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
                            contentDescription = stringResource(R.string.s_urun_ekle),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.s_diger),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.s_listeyi_paylas)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                enabled = uiState.pendingItems.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    shareList(context, uiState)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.s_fisle_tamamla)) },
                                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToReceipt()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.s_son_alisverisi_tekrarla)) },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                enabled = uiState.frequent.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    viewModel.repeatLastPurchase()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.s_yedekleme_ve_ayarlar)) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToBackup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.s_alisveris_verilerini_sifirla)) },
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
            if (!compactHeight && uiState.items.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToScanner,
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text(stringResource(R.string.s_barkod_tara)) },
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
            // Yatayda kaydırılan liste, dikeyde başlık dışarıda kalacak şekilde
            // aynı ofseti korumasın; döndürmede listenin başına dönülür.
            LaunchedEffect(scrollHeader) {
                listState.scrollToItem(0)
            }

            if (!scrollHeader && (!isGallery || marketMode)) {
                if (marketMode) {
                    StoreFilterRow(
                        stores = uiState.stores,
                        selectedStoreId = uiState.selectedStoreId,
                        onSelect = viewModel::selectStoreFilter
                    )
                } else {
                    when (uiStyle) {
                        AppUiStyle.MODERN -> ShoppingHeader(
                            stores = uiState.stores,
                            selectedStoreId = uiState.selectedStoreId,
                            onSelectStore = viewModel::selectStoreFilter,
                            frequent = uiState.frequent,
                            onAddFrequent = viewModel::addFrequent,
                            pendingCount = uiState.pendingItems.size,
                            purchasedCount = uiState.purchasedItems.size,
                            pfandTotal = uiState.pfandTotal,
                            euroTotal = uiState.euroTotal,
                            onComplete = { showCompleteDialog = true }
                        )

                        AppUiStyle.CLASSIC -> ClassicShoppingHeader(
                            stores = uiState.stores,
                            selectedStoreId = uiState.selectedStoreId,
                            onSelectStore = viewModel::selectStoreFilter,
                            frequent = uiState.frequent,
                            onAddFrequent = viewModel::addFrequent,
                            pendingCount = uiState.pendingItems.size,
                            purchasedCount = uiState.purchasedItems.size,
                            pfandTotal = uiState.pfandTotal,
                            euroTotal = uiState.euroTotal,
                            onComplete = { showCompleteDialog = true }
                        )

                        AppUiStyle.COMPACT -> CompactShoppingHeader(
                            stores = uiState.stores,
                            selectedStoreId = uiState.selectedStoreId,
                            onSelectStore = viewModel::selectStoreFilter,
                            frequent = uiState.frequent,
                            onAddFrequent = viewModel::addFrequent,
                            pendingCount = uiState.pendingItems.size,
                            purchasedCount = uiState.purchasedItems.size,
                            euroTotal = uiState.euroTotal,
                            onComplete = { showCompleteDialog = true }
                        )

                        AppUiStyle.GALLERY -> Unit
                    }
                }
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
                    listState = listState,
                    header = if (scrollHeader) {
                        {
                            StoreFilterRow(
                                stores = uiState.stores,
                                selectedStoreId = uiState.selectedStoreId,
                                onSelect = viewModel::selectStoreFilter
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isGallery) {
                GalleryShoppingList(
                    pending = uiState.pendingItems,
                    purchased = uiState.purchasedItems,
                    stores = uiState.stores,
                    selectedStoreId = uiState.selectedStoreId,
                    onSelectStore = viewModel::selectStoreFilter,
                    frequent = uiState.frequent,
                    onAddFrequent = viewModel::addFrequent,
                    pfandTotal = uiState.pfandTotal,
                    euroTotal = uiState.euroTotal,
                    onComplete = { showCompleteDialog = true },
                    onToggle = { item -> viewModel.togglePurchased(item.listItem) },
                    onDelete = { item -> viewModel.deleteItem(item.listItem) },
                    onQuantityChange = { item, delta -> viewModel.changeQuantity(item.listItem, delta) },
                    onClearPurchased = viewModel::clearPurchased
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
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(ShoppingScreenTestTags.LIST),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    if (scrollHeader) {
                        item(key = "compact-header") {
                            when (uiStyle) {
                                AppUiStyle.CLASSIC -> ClassicShoppingHeader(
                                    stores = uiState.stores,
                                    selectedStoreId = uiState.selectedStoreId,
                                    onSelectStore = viewModel::selectStoreFilter,
                                    frequent = uiState.frequent,
                                    onAddFrequent = viewModel::addFrequent,
                                    pendingCount = uiState.pendingItems.size,
                                    purchasedCount = uiState.purchasedItems.size,
                                    pfandTotal = uiState.pfandTotal,
                                    euroTotal = uiState.euroTotal,
                                    onComplete = { showCompleteDialog = true }
                                )

                                AppUiStyle.COMPACT -> CompactShoppingHeader(
                                    stores = uiState.stores,
                                    selectedStoreId = uiState.selectedStoreId,
                                    onSelectStore = viewModel::selectStoreFilter,
                                    frequent = uiState.frequent,
                                    onAddFrequent = viewModel::addFrequent,
                                    pendingCount = uiState.pendingItems.size,
                                    purchasedCount = uiState.purchasedItems.size,
                                    euroTotal = uiState.euroTotal,
                                    onComplete = { showCompleteDialog = true }
                                )

                                else -> ShoppingHeader(
                                    stores = uiState.stores,
                                    selectedStoreId = uiState.selectedStoreId,
                                    onSelectStore = viewModel::selectStoreFilter,
                                    frequent = uiState.frequent,
                                    onAddFrequent = viewModel::addFrequent,
                                    pendingCount = uiState.pendingItems.size,
                                    purchasedCount = uiState.purchasedItems.size,
                                    pfandTotal = uiState.pfandTotal,
                                    euroTotal = uiState.euroTotal,
                                    onComplete = { showCompleteDialog = true }
                                )
                            }
                        }
                    }

                    if (uiState.pendingItems.isNotEmpty()) {
                        item {
                            SectionHeader(
                                stringResource(R.string.section_to_buy),
                                uiState.pendingItems.size
                            )
                        }
                        groups.forEach { group ->
                            if (uiState.selectedStore != null) {
                                item(key = "aisle-${group.aisleNumber}-${group.categoryId}") {
                                    AisleHeader(
                                        number = group.aisleNumber,
                                        title = group.categoryId?.let { id ->
                                            uiState.categories.firstOrNull { it.id == id }?.nameTr
                                        } ?: stringResource(R.string.aisle_other),
                                        count = group.items.size
                                    )
                                }
                            }
                            items(group.items, key = { it.listItem.id }) { item ->
                                StyleShoppingItem(
                                    style = uiStyle,
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
                                    text = stringResource(R.string.p_alinanlar_x, uiState.purchasedItems.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearPurchased() }) {
                                    Text(stringResource(R.string.s_temizle))
                                }
                            }
                        }
                        items(uiState.purchasedItems, key = { it.listItem.id }) { item ->
                            StyleShoppingItem(
                                style = uiStyle,
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
internal fun StoreFilterRow(
    stores: List<Store>,
    selectedStoreId: Long?,
    onSelect: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StoreChip(
                label = stringResource(R.string.store_all),
                color = MaterialTheme.colorScheme.primary,
                selected = selectedStoreId == null,
                onClick = { onSelect(null) }
            )
        }
        items(stores, key = { it.id }) { store ->
            StoreChip(
                label = store.name,
                color = parseColor(store.colorHex),
                selected = selectedStoreId == store.id,
                onClick = { onSelect(store.id) }
            )
        }
    }
}

@Composable
private fun ShoppingHeader(
    stores: List<Store>,
    selectedStoreId: Long?,
    onSelectStore: (Long?) -> Unit,
    frequent: List<com.shoppingapp.data.model.FrequentProductRow>,
    onAddFrequent: (com.shoppingapp.data.model.FrequentProductRow) -> Unit,
    pendingCount: Int,
    purchasedCount: Int,
    pfandTotal: Double,
    euroTotal: Double,
    onComplete: () -> Unit
) {
    Column {
        StoreFilterRow(
            stores = stores,
            selectedStoreId = selectedStoreId,
            onSelect = onSelectStore
        )
        if (frequent.isNotEmpty()) {
            FrequentStrip(rows = frequent, onAdd = onAddFrequent)
        }
        SummaryCard(
            pendingCount = pendingCount,
            purchasedCount = purchasedCount,
            pfandTotal = pfandTotal,
            euroTotal = euroTotal,
            onComplete = onComplete
        )
    }
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
                    Text(text = stringResource(R.string.s_bugunku_liste),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.p_x_alinacak_x_alindi, pendingCount, purchasedCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.s_sepet),
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
                        text = stringResource(R.string.p_pfand_x, euro(pfandTotal)),
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
                        .testTag(ShoppingScreenTestTags.SUMMARY_COMPLETE)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.s_alisverisi_tamamla))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = stringResource(R.string.p_x_x_6, title, count),
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
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.size(48.dp)
            )
        }
        Text(text = stringResource(R.string.s_liste_henuz_bos),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(text = stringResource(R.string.s_barkod_okutarak_alinanlara_ekle_ya_da_elle_urun_),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(onClick = onScan, modifier = Modifier.padding(top = 20.dp)) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.s_barkod_tara))
        }
        TextButton(onClick = onAdd) { Text(stringResource(R.string.s_elle_urun_ekle)) }
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
                            text = stringResource(
                                R.string.p_pfand_x,
                                euro(item.product.pfandAmount * item.listItem.quantity)
                            ),
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
                        text = stringResource(R.string.p_x_x_4, quantity(item.listItem.quantity), item.product.unit),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    QuantityButton(
                        icon = Icons.Default.Add,
                        enabled = true,
                        description = stringResource(R.string.s_artir)
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
                    contentDescription = stringResource(R.string.s_sil),
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
        title = { Text(stringResource(R.string.s_alisveris_verilerini_sifirla), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.s_sunlar_silinir))
                Text(stringResource(R.string.s_alinacaklar_ve_alinanlar_listesi))
                Text(stringResource(R.string.s_alisveris_gecmisi_ve_istatistikler))
                Text(stringResource(R.string.s_kayitli_urunler_ve_kaydedilmis_fiyatlar))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.s_market_ve_kategori_ayarlari_ile_pfand_kayitlari_),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.s_sifirla)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s_vazgec)) }
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
            Column(Modifier.testTag(ShoppingScreenTestTags.COMPLETE_DIALOG)) {
                Text(stringResource(R.string.s_alisverisi_tamamla), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.p_x_kalem_birim_fiyatlari_kontrol_et, lines.size),
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
                                    text = stringResource(R.string.p_x_x_4, quantity(entry.listItem.quantity), entry.product.unit),
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
                                placeholder = { Text(stringResource(R.string.s_0_00)) },
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
                    Text(stringResource(R.string.s_toplam), style = MaterialTheme.typography.titleMedium)
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
                    Text(stringResource(R.string.s_pfand),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        euro(pfand),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(stringResource(R.string.s_tamamlayinca_liste_temizlenir_ve_harcama_gecmise),
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
                Text(stringResource(R.string.s_alisverisi_tamamla))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(ShoppingScreenTestTags.COMPLETE_DIALOG_DISMISS)
            ) { Text(stringResource(R.string.s_vazgec)) }
        }
    )
}

@Composable
private fun ReceiptDialog(receipt: PurchaseReceipt, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s_alisveris_kaydedildi), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.p_x_urun_x, receipt.itemCount, receipt.storeName),
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.s_toplam))
                    Text(euro(receipt.total), fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.s_pfand), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(euro(receipt.pfand), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.s_harcama_istatistiklere_islendi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.s_tamam)) }
        }
    )
}

@Composable
internal fun FrequentStrip(
    rows: List<com.shoppingapp.data.model.FrequentProductRow>,
    onAdd: (com.shoppingapp.data.model.FrequentProductRow) -> Unit
) {
    Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        Text(stringResource(R.string.s_sik_alinanlar_dokun_ekle),
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
                stringResource(R.string.p_reyon_x, number),
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
            stringResource(R.string.p_x_urun, count),
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
    listState: LazyListState,
    header: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.testTag(ShoppingScreenTestTags.LIST),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (header != null) {
            item(key = "market-header") { header() }
        }

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
                        Text(stringResource(R.string.s_market_modu),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(R.string.p_x_alinacak_x_alindi_satira_dokun, pending.size, purchased.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = onComplete, enabled = pending.isNotEmpty()) {
                        Text(stringResource(R.string.s_tamamla))
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
                    stringResource(R.string.p_alinanlar_x, purchased.size),
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
                        stringResource(R.string.p_x_x_4, quantity(item.listItem.quantity), item.product.unit),
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

@Composable
private fun StyleShoppingItem(
    style: AppUiStyle,
    item: UiShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    when (style) {
        AppUiStyle.MODERN -> ShoppingItemCard(
            item = item,
            onToggle = onToggle,
            onDelete = onDelete,
            onQuantityChange = onQuantityChange
        )

        AppUiStyle.CLASSIC -> ClassicShoppingItem(
            item = item,
            onToggle = onToggle,
            onDelete = onDelete,
            onQuantityChange = onQuantityChange
        )

        AppUiStyle.COMPACT -> CompactShoppingItem(
            item = item,
            onToggle = onToggle,
            onDelete = onDelete,
            onQuantityChange = onQuantityChange
        )

        // Vitrin düzeni kendi iki sütunlu ızgarasını kullanır.
        AppUiStyle.GALLERY -> Unit
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
    val title = state.selectedStore
        ?.let { context.getString(R.string.share_title_store, it.name) }
        ?: context.getString(R.string.s_alisveris_listesi)
    val text = ListShare.text(
        lines = lines,
        title = title,
        strings = ShareStrings(
            empty = context.getString(R.string.share_empty),
            noStore = context.getString(R.string.s_market_yok),
            itemCount = { count -> context.getString(R.string.share_item_count, count) }
        )
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.s_listeyi_paylas)))
}

internal fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF047857))

internal fun euro(value: Double): String = String.format(Locale.GERMANY, "€%.2f", value)

private fun amount(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)

internal fun quantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.GERMANY, "%.1f", value)
