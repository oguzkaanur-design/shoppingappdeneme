package com.shoppingapp.feature.scanner

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.model.LastPriceRow
import com.shoppingapp.data.model.Product
import com.shoppingapp.data.model.Store
import com.shoppingapp.data.repository.ProductLookupRepository
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Barkod okundu, ürün bulundu; kullanıcı hangi marketten aldığını seçecek. */
data class PendingScan(
    val product: Product,
    val barcode: String,
    /** Aynı ürüne en son ödenen fiyat; geçmiş yoksa null. */
    val lastPrice: LastPriceRow? = null
)

/** Listeye eklenen son tarama. */
data class AddedScan(val product: Product, val storeId: Long)

data class ScannerUiState(
    val isLookingUp: Boolean = false,
    val pending: PendingScan? = null,
    val lastAdded: AddedScan? = null,
    val addedCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val lookupRepository: ProductLookupRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    val stores: StateFlow<List<Store>> = shoppingRepository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastBarcode: String? = null
    private var lastScanAt = 0L

    fun onBarcodeDetected(barcode: String) {
        val value = barcode.trim()
        if (value.length < 6) return

        val now = SystemClock.elapsedRealtime()
        val isSameCodeAgain = value == lastBarcode && now - lastScanAt < 2500
        if (isSameCodeAgain || _state.value.isLookingUp) return

        lastBarcode = value
        lastScanAt = now
        _state.update { it.copy(isLookingUp = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val product = lookupRepository.findOrCreate(value)
                product to shoppingRepository.lastPriceFor(product.id)
            }.onSuccess { (product, lastPrice) ->
                _state.update {
                    it.copy(
                        isLookingUp = false,
                        pending = PendingScan(
                            product = product,
                            barcode = value,
                            lastPrice = lastPrice
                        ),
                        error = null
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        isLookingUp = false,
                        error = "Barkod okundu ama ürün kaydedilemedi. Tekrar deneyin."
                    )
                }
            }
        }
    }

    /**
     * Ürünü seçilen marketle listeye ekler.
     * @param storeId 0 → market belirtilmedi.
     */
    fun confirmAdd(storeId: Long) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            shoppingRepository.addScannedToList(pending.product, storeId)
            _state.update {
                it.copy(
                    pending = null,
                    lastAdded = AddedScan(product = pending.product, storeId = storeId),
                    addedCount = it.addedCount + 1
                )
            }
        }
    }

    /** Taramayı iptal eder; ürün kataloğa kaydedilmiş olarak kalır. */
    fun dismissPending() {
        _state.update { it.copy(pending = null) }
        lastBarcode = null
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
