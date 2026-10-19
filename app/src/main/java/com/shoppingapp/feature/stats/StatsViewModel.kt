package com.shoppingapp.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StatsUiState(
    val totalSpending: Double = 0.0,
    val pfandTotal: Double = 0.0,
    val pfandUnits: Int = 0,
    val activeItemCount: Int = 0,
    val purchasedItemCount: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    val statsState: StateFlow<StatsUiState> = combine(
        repository.allListItems,
        repository.purchaseHistory,
        repository.pendingPfand
    ) { items, history, pfandEntries ->
        val activeCount = items.count { !it.isPurchased }
        val purchasedCount = items.count { it.isPurchased }

        StatsUiState(
            totalSpending = history.sumOf { it.totalAmount },
            pfandTotal = pfandEntries.sumOf { it.unitAmount * it.quantity },
            pfandUnits = pfandEntries.sumOf { it.quantity },
            activeItemCount = activeCount,
            purchasedItemCount = purchasedCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
