package com.shoppingapp.feature.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PantryRowUi(
    val row: PantryRow,
    val status: PantryStatus
)

data class PantryUiState(
    val entries: List<PantryRowUi> = emptyList(),
    val loading: Boolean = true
) {
    val critical: List<PantryRowUi> get() = entries.filter { it.status.needsRestock }
    val expiring: List<PantryRowUi> get() = entries.filter {
        it.status.expiryLevel == ExpiryLevel.EXPIRED || it.status.expiryLevel == ExpiryLevel.SOON
    }
    val stockedCount: Int get() = entries.count { it.status.level == PantryLevel.OK }
}

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val stores: StateFlow<List<com.shoppingapp.data.model.Store>> = repository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<com.shoppingapp.data.model.Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<PantryUiState> = repository.pantryRows
        .map { rows ->
            // Sıralama ve durum her veri değişiminde yeniden hesaplanır.
            val now = System.currentTimeMillis()
            PantryUiState(
                entries = PantrySummary.sort(rows, now).map { row ->
                    PantryRowUi(row = row, status = PantrySummary.evaluate(row, now))
                },
                loading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PantryUiState()
        )

    fun addItem(
        name: String,
        quantity: Double,
        minThreshold: Double,
        expiryDate: Long?,
        unit: String,
        storeId: Long,
        categoryId: Long
    ) {
        viewModelScope.launch {
            val id = repository.addPantryItem(
                name = name,
                quantity = quantity,
                minThreshold = minThreshold,
                expiryDate = expiryDate,
                unit = unit,
                storeId = storeId,
                categoryId = categoryId
            )
            _message.value = if (id == null) "Ürün eklenemedi" else "$name kilere eklendi"
        }
    }

    fun changeQuantity(row: PantryRow, delta: Double) {
        viewModelScope.launch {
            val next = (row.currentQuantity + delta).coerceAtLeast(0.0)
            repository.setPantryQuantity(row.id, next)
        }
    }

    fun setThreshold(row: PantryRow, threshold: Double) {
        viewModelScope.launch {
            repository.addPantryItemForProduct(
                productId = row.productId,
                quantity = 0.0,
                minThreshold = threshold,
                expiryDate = row.expiryDate
            )
            _message.value = "${row.name}: kritik eşik ${threshold.toInt()}"
        }
    }

    fun addToList(row: PantryRow) {
        viewModelScope.launch {
            repository.addPantryRowToList(row)
            _message.value = "${row.name} alışveriş listesine eklendi"
        }
    }

    /** Kritik stoktaki tüm ürünleri tek dokunuşla listeye ekler. */
    fun restockAll() {
        val rows = uiState.value.critical.map { it.row }
        if (rows.isEmpty()) return
        viewModelScope.launch {
            rows.forEach { repository.addPantryRowToList(it) }
            _message.value = "${rows.size} ürün alışveriş listesine eklendi"
        }
    }

    fun delete(row: PantryRow) {
        viewModelScope.launch {
            repository.deletePantryItem(
                com.shoppingapp.data.model.PantryItem(
                    id = row.id,
                    productId = row.productId,
                    currentQuantity = row.currentQuantity,
                    minThreshold = row.minThreshold,
                    expiryDate = row.expiryDate
                )
            )
            _message.value = "${row.name} kilerden kaldırıldı"
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
