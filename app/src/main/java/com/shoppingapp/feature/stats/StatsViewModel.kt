package com.shoppingapp.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.Category
import com.shoppingapp.data.model.ListItem
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.data.model.PurchaseHistory
import com.shoppingapp.data.model.PurchaseHistoryLine
import com.shoppingapp.data.model.Store
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategorySlice(
    val categoryId: Long,
    val name: String,
    val total: Double,
    val share: Double
)

data class StoreSlice(
    val storeId: Long,
    val name: String,
    val colorHex: String,
    val total: Double,
    val visits: Int,
    val averageBasket: Double
)

data class StatsUiState(
    val totalSpending: Double = 0.0,
    val pfandTotal: Double = 0.0,
    val pfandUnits: Int = 0,
    val activeItemCount: Int = 0,
    val purchasedItemCount: Int = 0,
    val weekly: List<PeriodSpend> = emptyList(),
    val monthly: List<PeriodSpend> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
    val stores: List<StoreSlice> = emptyList(),
    val trends: List<PriceTrend> = emptyList(),
    val purchaseCount: Int = 0
) {
    val hasHistory: Boolean get() = purchaseCount > 0 && totalSpending > 0.0
    val cheapestBasket: StoreSlice? get() = stores.minByOrNull { it.averageBasket }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    private data class Core(
        val items: List<ListItem>,
        val history: List<PurchaseHistory>,
        val pfand: List<PfandEntry>
    )

    private data class Extra(
        val lines: List<PurchaseHistoryLine>,
        val categories: List<Category>,
        val stores: List<Store>
    )

    val statsState: StateFlow<StatsUiState> = combine(
        combine(
            repository.allListItems,
            repository.purchaseHistory,
            repository.pendingPfand
        ) { items, history, pfand -> Core(items, history, pfand) },
        combine(
            repository.historyLines,
            repository.allCategories,
            repository.allStores
        ) { lines, categories, stores -> Extra(lines, categories, stores) }
    ) { core, extra ->
        val categoryMap = extra.categories.associateBy { it.id }
        val storeMap = extra.stores.associateBy { it.id }

        // Satır bazlı toplam varsa o kullanılır; eski kayıtlarda yalnızca fiş toplamı vardır.
        val lineTotal = SpendingAnalytics.total(extra.lines)
        val spending = if (lineTotal > 0.0) lineTotal else core.history.sumOf { it.totalAmount }

        val categorySlices = SpendingAnalytics.byCategory(extra.lines).map { spend ->
            CategorySlice(
                categoryId = spend.categoryId,
                name = categoryMap[spend.categoryId]?.nameTr ?: "Diğer",
                total = spend.total,
                share = if (spending > 0.0) spend.total / spending else 0.0
            )
        }
        val storeSlices = SpendingAnalytics.byStore(extra.lines).map { spend ->
            StoreSlice(
                storeId = spend.storeId,
                name = storeMap[spend.storeId]?.name ?: "Market",
                colorHex = storeMap[spend.storeId]?.colorHex ?: "#047857",
                total = spend.total,
                visits = spend.visits,
                averageBasket = spend.averageBasket
            )
        }

        StatsUiState(
            totalSpending = spending,
            pfandTotal = core.pfand.sumOf { it.unitAmount * it.quantity },
            pfandUnits = core.pfand.sumOf { it.quantity },
            activeItemCount = core.items.count { !it.isPurchased },
            purchasedItemCount = core.items.count { it.isPurchased },
            weekly = SpendingAnalytics.weekly(extra.lines),
            monthly = SpendingAnalytics.monthly(extra.lines),
            categories = categorySlices,
            stores = storeSlices,
            trends = SpendingAnalytics.priceTrends(extra.lines),
            purchaseCount = core.history.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )
}
