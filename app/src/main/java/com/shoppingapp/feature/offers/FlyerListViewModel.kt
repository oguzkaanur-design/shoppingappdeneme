package com.shoppingapp.feature.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.repository.FlyerListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlyerListViewModel @Inject constructor(
    private val repository: FlyerListRepository
) : ViewModel() {

    val items: StateFlow<List<FlyerListItem>> = repository.items.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val count: StateFlow<Int> = repository.count.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun increase(item: FlyerListItem) {
        viewModelScope.launch { repository.setQuantity(item, item.quantity + 1) }
    }

    fun decrease(item: FlyerListItem) {
        viewModelScope.launch { repository.setQuantity(item, item.quantity - 1) }
    }

    fun remove(item: FlyerListItem) {
        viewModelScope.launch { repository.remove(item) }
    }

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}
