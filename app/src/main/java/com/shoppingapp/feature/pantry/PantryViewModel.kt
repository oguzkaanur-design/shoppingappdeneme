package com.shoppingapp.feature.pantry

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.R
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repository: ShoppingRepository,
    @ApplicationContext private val context: Context
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
            _message.value = if (id == null) {
                context.getString(R.string.pantry_add_failed)
            } else {
                context.getString(R.string.pantry_item_added, name)
            }
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
            _message.value = context.getString(R.string.toast_threshold_set, row.name, threshold.toInt())
        }
    }

    fun addToList(row: PantryRow) {
        viewModelScope.launch {
            repository.addPantryRowToList(row)
            _message.value = context.getString(R.string.toast_pantry_added, row.name)
        }
    }

    /** Kritik stoktaki tüm ürünleri tek dokunuşla listeye ekler. */
    fun restockAll() {
        val rows = uiState.value.critical.map { it.row }
        if (rows.isEmpty()) return
        viewModelScope.launch {
            rows.forEach { repository.addPantryRowToList(it) }
            _message.value = context.getString(R.string.toast_pantry_added_many, rows.size)
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
            _message.value = context.getString(R.string.toast_pantry_removed, row.name)
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
