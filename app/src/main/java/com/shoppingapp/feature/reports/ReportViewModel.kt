package com.shoppingapp.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.data.repository.ShoppingRepository
import com.shoppingapp.feature.money.MoneyBalances
import com.shoppingapp.feature.money.MoneySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReportUiState(
    val entries: List<MoneyEntry> = emptyList(),
    val pendingPfand: List<PfandEntry> = emptyList(),
    val returnedPfand: List<PfandEntry> = emptyList(),
    val pantry: List<PantryRow> = emptyList(),
    val loading: Boolean = true
) {
    val pendingUnits: Int get() = pendingPfand.sumOf { it.quantity }
    val pfandValue: Double get() = pendingPfand.sumOf { it.unitAmount * it.quantity }
    val balances: MoneyBalances get() = MoneySummary.of(entries, pfandValue)
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    repository: ShoppingRepository
) : ViewModel() {

    val uiState: StateFlow<ReportUiState> = combine(
        repository.moneyEntries,
        repository.pendingPfand,
        repository.returnedPfand,
        repository.pantryRows
    ) { entries, pending, returned, pantry ->
        ReportUiState(
            entries = entries,
            pendingPfand = pending,
            returnedPfand = returned,
            pantry = pantry,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )
}
