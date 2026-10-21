package com.shoppingapp.data.repository

import com.shoppingapp.data.dao.FlyerListDao
import com.shoppingapp.data.model.FlyerListItem
import com.shoppingapp.data.offers.FlyerImageStore
import com.shoppingapp.data.offers.FlyerOffer
import com.shoppingapp.data.offers.FlyerStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prospektüsten eklenen fırsat ürünlerinin kalıcı listesi. Aynı fırsat ikinci kez
 * eklenirse yeni satır açılmaz, adedi artırılır.
 */
@Singleton
class FlyerListRepository @Inject constructor(
    private val dao: FlyerListDao,
    private val imageStore: FlyerImageStore
) {

    val items: Flow<List<FlyerListItem>> = dao.observeAll()
    val count: Flow<Int> = dao.observeCount()

    suspend fun add(offer: FlyerOffer, store: FlyerStore, quantity: Int): FlyerListItem {
        val existing = dao.findByOfferId(offer.id)
        if (existing != null) {
            val updated = existing.copy(
                quantity = existing.quantity + quantity,
                unitPrice = offer.price ?: existing.unitPrice,
                validToEpochMs = offer.validTo?.toEpochMilli() ?: existing.validToEpochMs
            )
            dao.update(updated)
            return updated
        }

        val imagePath = offer.hotspot?.let { hotspot ->
            runCatching { imageStore.saveCrop(offer.leafletId, hotspot) }.getOrNull()
        }
        val item = FlyerListItem(
            offerId = offer.id,
            storeName = store.name,
            storeColor = store.brandColor,
            productName = offer.name,
            description = offer.description,
            unitPrice = offer.price ?: 0.0,
            quantity = quantity,
            imagePath = imagePath,
            validFromEpochMs = offer.validFrom?.toEpochMilli() ?: System.currentTimeMillis(),
            validToEpochMs = offer.validTo?.toEpochMilli() ?: System.currentTimeMillis()
        )
        val id = dao.insert(item)
        return item.copy(id = id)
    }

    suspend fun setQuantity(item: FlyerListItem, quantity: Int) {
        if (quantity <= 0) {
            remove(item)
        } else {
            dao.update(item.copy(quantity = quantity))
        }
    }

    suspend fun remove(item: FlyerListItem) {
        dao.delete(item)
        imageStore.delete(item.imagePath)
    }

    suspend fun clear() {
        dao.getAll().forEach { imageStore.delete(it.imagePath) }
        dao.clear()
    }
}
