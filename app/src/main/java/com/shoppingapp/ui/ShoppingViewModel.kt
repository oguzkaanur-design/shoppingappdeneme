package com.shoppingapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.shoppingapp.data.aisle.AisleOrder
import com.shoppingapp.data.catalog.BrandCatalog
import com.shoppingapp.data.model.*
import com.shoppingapp.data.repository.PurchaseLine
import com.shoppingapp.data.repository.PurchaseReceipt
import com.shoppingapp.R
import com.shoppingapp.data.repository.ShoppingRepository
import com.shoppingapp.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiShoppingItem(
    val listItem: ListItem,
    val product: Product,
    val store: Store?,
    val category: Category?
)

data class ShoppingUiState(
    val items: List<UiShoppingItem> = emptyList(),
    val stores: List<Store> = emptyList(),
    val categories: List<Category> = emptyList(),
    val frequent: List<FrequentProductRow> = emptyList(),
    val selectedStoreId: Long? = null,
    val isLoading: Boolean = true
) {
    val pendingItems: List<UiShoppingItem> get() = items.filter { !it.listItem.isPurchased }
    val purchasedItems: List<UiShoppingItem> get() = items.filter { it.listItem.isPurchased }
    val selectedStore: Store? get() = stores.firstOrNull { it.id == selectedStoreId }
    val pfandTotal: Double
        get() = pendingItems.sumOf { it.product.pfandAmount * it.listItem.quantity }
    val euroTotal: Double
        get() = pendingItems.sumOf { it.product.priceEuro * it.listItem.quantity }

    /**
     * Seçili marketin reyon sırası. "Tümü" görünümünde reyon sırası anlamsız
     * olduğu için boş döner ve liste düz gösterilir.
     */
    val aisleOrder: List<Long>
        get() = selectedStore?.let { AisleOrder.parse(it.aisleOrderJson) }.orEmpty()
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repository: ShoppingRepository,
    private val widgetUpdater: WidgetUpdater,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedStoreId = MutableStateFlow<Long?>(null)
    private val _receipt = MutableStateFlow<PurchaseReceipt?>(null)
    private val _message = MutableStateFlow<String?>(null)
    val receipt: StateFlow<PurchaseReceipt?> = _receipt.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()

    private data class Base(
        val items: List<ListItem>,
        val products: List<Product>,
        val stores: List<Store>,
        val categories: List<Category>
    )

    val uiState: StateFlow<ShoppingUiState> = combine(
        combine(
            repository.allListItems,
            repository.allProducts,
            repository.allStores,
            repository.allCategories
        ) { items, products, stores, categories -> Base(items, products, stores, categories) },
        _selectedStoreId,
        repository.frequentProducts
    ) { base, selectedStore, frequent ->
        val productMap = base.products.associateBy { it.id }
        val storeMap = base.stores.associateBy { it.id }
        val categoryMap = base.categories.associateBy { it.id }

        val uiItems = base.items.mapNotNull { item ->
            val product = productMap[item.productId] ?: return@mapNotNull null
            UiShoppingItem(
                listItem = item,
                product = product,
                store = storeMap[item.storeId],
                category = categoryMap[product.categoryId]
            )
        }.filter {
            selectedStore == null || it.listItem.storeId == selectedStore
        }.sortedBy { it.listItem.addedAt }

        ShoppingUiState(
            items = uiItems,
            stores = base.stores,
            categories = base.categories,
            frequent = frequent,
            selectedStoreId = selectedStore,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShoppingUiState()
    )

    init {
        viewModelScope.launch {
            repository.initDefaultData()
        }
    }

    fun selectStoreFilter(storeId: Long?) {
        _selectedStoreId.value = storeId
    }

    fun togglePurchased(item: ListItem) {
        viewModelScope.launch {
            repository.updateListItem(item.copy(isPurchased = !item.isPurchased))
            widgetUpdater.refresh()
        }
    }

    fun changeQuantity(item: ListItem, delta: Double) {
        val next = (item.quantity + delta).coerceAtLeast(1.0)
        viewModelScope.launch {
            repository.updateListItem(item.copy(quantity = next))
            widgetUpdater.refresh()
        }
    }

    fun deleteItem(item: ListItem) {
        viewModelScope.launch {
            repository.deleteListItem(item)
            widgetUpdater.refresh()
        }
    }

    /**
     * @param storeId 0 → market seçilmedi (ürün yalnızca "Tümü" filtresinde görünür)
     * @param categoryId 0 → ürün adından tahmin edilir, eşleşme yoksa genel görsel
     */
    fun addNewItem(
        name: String,
        storeId: Long,
        categoryId: Long,
        quantity: Double,
        unit: String,
        pfandAmount: Double,
        priceEuro: Double
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val resolvedCategory = when {
                categoryId > 0L -> categoryId
                else -> BrandCatalog.detectCategory(cleanName) ?: 0L
            }
            val product = Product(
                name = cleanName,
                barcode = "CUSTOM_${System.currentTimeMillis()}",
                categoryId = resolvedCategory,
                unit = unit.ifBlank { "Adet" },
                defaultStoreId = storeId.takeIf { it > 0L },
                pfandAmount = pfandAmount.coerceAtLeast(0.0),
                imageUrl = null,
                priceEuro = priceEuro.coerceAtLeast(0.0)
            )
            val productId = repository.addProduct(product)
            repository.addListItem(productId = productId, storeId = storeId, quantity = quantity)
            widgetUpdater.refresh()
        }
    }

    /** "Sık alınanlar" şeridinden tek dokunuşla listeye ekler. */
    fun addFrequent(row: FrequentProductRow) {
        viewModelScope.launch {
            repository.addFrequentToList(row)
            _message.value = context.getString(R.string.toast_added_to_list, row.name)
            widgetUpdater.refresh()
        }
    }

    /** Son tamamlanan alışverişteki ürünleri listeye geri koyar. */
    fun repeatLastPurchase() {
        viewModelScope.launch {
            val added = repository.repeatLastPurchase()
            _message.value = if (added > 0) {
                context.getString(R.string.toast_repeat_added, added)
            } else {
                context.getString(R.string.toast_repeat_none)
            }
            widgetUpdater.refresh()
        }
    }

    /** Liste + alışveriş geçmişi + kayıtlı ürünleri siler; market ayarları kalır. */
    fun resetShoppingData() {
        viewModelScope.launch {
            repository.resetShoppingData()
            widgetUpdater.refresh()
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            repository.clearPurchasedItems()
            widgetUpdater.refresh()
        }
    }

    /**
     * @param prices unit prices keyed by list item id, as entered in the checkout sheet.
     */
    fun completePurchase(prices: Map<Long, Double>) {
        val pending = uiState.value.pendingItems
        if (pending.isEmpty()) return

        val storeId = pending
            .groupingBy { it.listItem.storeId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: 1L

        viewModelScope.launch {
            val result = repository.completePurchase(
                lines = pending.map { entry ->
                    PurchaseLine(
                        listItemId = entry.listItem.id,
                        productId = entry.product.id,
                        quantity = entry.listItem.quantity,
                        unitPrice = (prices[entry.listItem.id] ?: 0.0).coerceAtLeast(0.0),
                        pfandPerUnit = entry.product.pfandAmount
                    )
                },
                storeId = storeId
            )
            _receipt.value = result
            widgetUpdater.refresh()
        }
    }

    fun dismissReceipt() {
        _receipt.value = null
    }

    fun dismissMessage() {
        _message.value = null
    }
}
