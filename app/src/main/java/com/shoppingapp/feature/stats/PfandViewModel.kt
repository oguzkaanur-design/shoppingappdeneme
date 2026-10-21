package com.shoppingapp.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PfandUiState(
    val pending: List<PfandEntry> = emptyList(),
    val returned: List<PfandEntry> = emptyList()
) {
    val pendingTotal: Double get() = pending.sumOf { it.unitAmount * it.quantity }
    val pendingUnits: Int get() = pending.sumOf { it.quantity }
    val returnedTotal: Double get() = returned.sumOf { it.unitAmount * it.quantity }
    val returnedUnits: Int get() = returned.sumOf { it.quantity }
}

@HiltViewModel
class PfandViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    val uiState: StateFlow<PfandUiState> = combine(
        repository.pendingPfand,
        repository.returnedPfand
    ) { pending, returned ->
        PfandUiState(pending = pending, returned = returned)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PfandUiState()
    )

    fun addEntry(label: String, unitAmount: Double, quantity: Int) {
        viewModelScope.launch {
            repository.addPfandEntry(label, unitAmount, quantity)
        }
    }

    fun deleteEntry(entry: PfandEntry) {
        viewModelScope.launch {
            repository.deletePfandEntry(entry)
        }
    }

    /** İadeyi tamamlar; bekleyen tutar Cebim'deki Nakit bakiyesine otomatik eklenir. */
    fun returnAll() {
        viewModelScope.launch {
            repository.cashInAllPfand()
        }
    }

    fun clearReturned() {
        viewModelScope.launch {
            repository.clearReturnedPfand()
        }
    }

    /** Bekleyen + iade edilmiş tüm Pfand kayıtlarını siler. */
    fun resetAll() {
        viewModelScope.launch {
            repository.resetPfandData()
        }
    }
}
