package com.shoppingapp.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingsUiState(
    val entries: List<MoneyEntry> = emptyList(),
    val pendingPfand: Double = 0.0,
    val pendingPfandUnits: Int = 0
) {
    /** Nakit + banka + iade bekleyen Pfand; ekranın gösterdiği toplam. */
    val balances: MoneyBalances get() = MoneySummary.of(entries, pendingPfand)
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    val uiState: StateFlow<SavingsUiState> = combine(
        repository.moneyEntries,
        repository.pendingPfand
    ) { entries, pfand ->
        SavingsUiState(
            entries = entries,
            pendingPfand = pfand.sumOf { it.unitAmount * it.quantity },
            pendingPfandUnits = pfand.sumOf { it.quantity }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsUiState()
    )

    /** [amount] işaretli gelir: + ekleme, − harcama. */
    fun addMovement(account: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addMoneyEntry(account, amount, note)
        }
    }

    fun deleteEntry(entry: MoneyEntry) {
        viewModelScope.launch {
            repository.deleteMoneyEntry(entry)
        }
    }

    /** Yalnızca nakit/banka hareketlerini siler; Pfand defteri korunur. */
    fun resetMoney() {
        viewModelScope.launch {
            repository.resetMoneyData()
        }
    }
}
