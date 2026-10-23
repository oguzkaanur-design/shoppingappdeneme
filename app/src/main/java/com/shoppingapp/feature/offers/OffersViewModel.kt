package com.shoppingapp.feature.offers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.offers.Flyer
import com.shoppingapp.data.offers.FlyerCache
import com.shoppingapp.data.offers.FlyerOffer
import com.shoppingapp.data.offers.FlyerOfferRepository
import com.shoppingapp.data.offers.FlyerRepository
import com.shoppingapp.data.offers.FlyerSelection
import com.shoppingapp.data.offers.FlyerStore
import com.shoppingapp.data.offers.FlyerStores
import com.shoppingapp.data.repository.FlyerListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffersUiState(
    val selectedStore: FlyerStore = FlyerStores.all.first(),
    val loading: Boolean = true,
    val flyers: List<Flyer> = emptyList(),
    val error: String? = null,
    /** Liste internetten değil cihazdaki son kopyadan geldi. */
    val fromCache: Boolean = false,
    val cachedAt: Long? = null
)

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val repository: FlyerRepository,
    private val offerRepository: FlyerOfferRepository,
    private val flyerListRepository: FlyerListRepository,
    private val cache: FlyerCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    private val cachedFlyers = mutableMapOf<String, List<Flyer>>()
    private var loadJob: Job? = null

    init {
        load(FlyerStores.all.first())
    }

    fun selectStore(store: FlyerStore) {
        if (store.id == _uiState.value.selectedStore.id && !_uiState.value.loading) return
        load(store)
    }

    fun retry() {
        val store = _uiState.value.selectedStore
        cachedFlyers.remove(store.id)
        load(store)
    }

    /** Okuyucudaki bir sayfanın tıklanabilir ürünlerini getirir. */
    suspend fun offersForPage(leafletId: Long, pageIndex: Int): List<FlyerOffer> =
        try {
            offerRepository.offersForPage(leafletId, pageIndex)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // Tıklanabilir katman yardımcı özellik; hata broşürü engellemesin ama
            // logcat'te neden görünmediği anlaşılsın.
            Log.w("OffersViewModel", "leaflet $leafletId s.$pageIndex teklifleri alınamadı", error)
            emptyList()
        }

    /** Prospektüsten seçilen ürünü, seçili marketin fiyatı ve geçerliliğiyle listeye ekler. */
    fun addOfferToFlyerList(offer: FlyerOffer, quantity: Int) {
        val store = _uiState.value.selectedStore
        viewModelScope.launch {
            runCatching { flyerListRepository.add(offer, store, quantity) }
        }
    }

    private fun load(store: FlyerStore) {
        loadJob?.cancel()

        cachedFlyers[store.id]?.let { flyers ->
            _uiState.value = OffersUiState(selectedStore = store, loading = false, flyers = flyers)
            return
        }

        _uiState.value = OffersUiState(selectedStore = store, loading = true)
        loadJob = viewModelScope.launch {
            try {
                val flyers = FlyerSelection.relevant(repository.flyers(store))
                cachedFlyers[store.id] = flyers
                cache.write(store.id, flyers)
                if (_uiState.value.selectedStore.id == store.id) {
                    _uiState.value = OffersUiState(selectedStore = store, loading = false, flyers = flyers)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // Ağ/ayrıştırma hatasında son bilinen liste gösterilir; ekran boş kalmaz.
                val cached = runCatching { cache.read(store.id) }.getOrNull()
                if (_uiState.value.selectedStore.id == store.id) {
                    if (cached != null) {
                        _uiState.value = OffersUiState(
                            selectedStore = store,
                            loading = false,
                            flyers = FlyerSelection.relevant(cached),
                            fromCache = true,
                            cachedAt = cache.savedAt(store.id)
                        )
                    } else {
                        _uiState.value = OffersUiState(
                            selectedStore = store,
                            loading = false,
                            error = error.message ?: "Prospektüsler yüklenemedi."
                        )
                    }
                }
            }
        }
    }
}
