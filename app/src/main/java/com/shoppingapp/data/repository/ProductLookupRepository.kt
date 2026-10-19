package com.shoppingapp.data.repository

import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.model.Product
import com.shoppingapp.data.remote.OffProduct
import com.shoppingapp.data.remote.OpenFoodFactsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductLookupRepository @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val shoppingDao: ShoppingDao
) {

    /**
     * Looks a barcode up locally first, then on Open Food Facts. Always returns a
     * product: if the network is down the barcode still becomes a usable item.
     */
    suspend fun findOrCreate(barcode: String): Product {
        shoppingDao.getProductByBarcode(barcode)?.let { return it }

        val remote = runCatching { api.getProduct(barcode).product }
            .getOrNull()

        val product = fromRemote(barcode, remote)
        val id = shoppingDao.insertProduct(product)
        return product.copy(id = id)
    }

    private fun fromRemote(barcode: String, remote: OffProduct?): Product {
        val rawName = remote?.name?.takeIf { it.isNotBlank() } ?: remote?.genericName
        val brand = remote?.brands?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
        // Alman market etiketlerinde marka ada önde yazılır: "Milbona Joghurt mild".
        val displayName = when {
            rawName == null -> "Ürün $barcode"
            brand == null -> rawName
            rawName.contains(brand, ignoreCase = true) -> rawName
            else -> "$brand $rawName"
        }

        return Product(
            name = displayName,
            barcode = barcode,
            categoryId = mapCategory(remote?.categoriesTags),
            imageUrl = remote?.imageFrontSmallUrl
                ?: remote?.imageFrontUrl
                ?: remote?.imageUrl,
            unit = remote?.quantity?.takeIf { it.isNotBlank() } ?: "Adet",
            defaultStoreId = 1,
            pfandAmount = guessPfand(remote)
        )
    }

    private fun mapCategory(tags: List<String>?): Long {
        val text = tags.orEmpty().joinToString(" ").lowercase()
        return when {
            listOf("beverage", "drink", "water", "juice", "soda", "beer", "wine")
                .any { text.contains(it) } -> 3L
            listOf("milk", "dairy", "cheese", "yogurt", "yoghurt", "butter")
                .any { text.contains(it) } -> 2L
            listOf("snack", "chocolate", "candy", "biscuit", "sweet", "dessert")
                .any { text.contains(it) } -> 4L
            listOf("fruit", "vegetable", "fresh", "produce")
                .any { text.contains(it) } -> 1L
            else -> 5L
        }
    }

    /** Single-use drinks in Germany are usually 0,25 € Pfand. */
    private fun guessPfand(remote: OffProduct?): Double {
        val text = (remote?.categoriesTags.orEmpty().joinToString(" ") + " " + (remote?.name ?: "")).lowercase()
        val oneWay = listOf("water", "wasser", "cola", "beer", "bier", "soda", "limonade", "energy")
        return if (oneWay.any { text.contains(it) }) 0.25 else 0.0
    }
}
