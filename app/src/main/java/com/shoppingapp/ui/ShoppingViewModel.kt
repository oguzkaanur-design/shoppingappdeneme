package com.shoppingapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.catalog.BrandCatalog
import com.shoppingapp.data.model.*
import com.shoppingapp.data.repository.PurchaseLine
import com.shoppingapp.data.repository.PurchaseReceipt
import com.shoppingapp.data.repository.ShoppingRepository
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
    val selectedStoreId: Long? = null,
    val isLoading: Boolean = true
) {
    val pendingItems: List<UiShoppingItem> get() = items.filter { !it.listItem.isPurchased }
    val purchasedItems: List<UiShoppingItem> get() = items.filter { it.listItem.isPurchased }
    val pfandTotal: Double
        get() = pendingItems.sumOf { it.product.pfandAmount * it.listItem.quantity }
    val euroTotal: Double
        get() = pendingItems.sumOf { it.product.priceEuro * it.listItem.quantity }
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    private val _selectedStoreId = MutableStateFlow<Long?>(null)
    private val _receipt = MutableStateFlow<PurchaseReceipt?>(null)
    val receipt: StateFlow<PurchaseReceipt?> = _receipt.asStateFlow()

    val uiState: StateFlow<ShoppingUiState> = combine(
        repository.allListItems,
        repository.allProducts,
        repository.allStores,
        repository.allCategories,
        _selectedStoreId
    ) { items, products, stores, categories, selectedStore ->
        val productMap = products.associateBy { it.id }
        val storeMap = stores.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        val uiItems = items.mapNotNull { item ->
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
            stores = stores,
            categories = categories,
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
        }
    }

    fun changeQuantity(item: ListItem, delta: Double) {
        val next = (item.quantity + delta).coerceAtLeast(1.0)
        viewModelScope.launch {
            repository.updateListItem(item.copy(quantity = next))
        }
    }

    fun deleteItem(item: ListItem) {
        viewModelScope.launch {
            repository.deleteListItem(item)
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
        }
    }

    /** Liste + alışveriş geçmişi + kayıtlı ürünleri siler; market ayarları kalır. */
    fun resetShoppingData() {
        viewModelScope.launch {
            repository.resetShoppingData()
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            repository.clearPurchasedItems()
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
        }
    }

    fun dismissReceipt() {
        _receipt.value = null
    }
}
