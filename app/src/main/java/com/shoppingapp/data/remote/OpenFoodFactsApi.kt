package com.shoppingapp.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts — free, no API key, German/European product database.
 * Docs: https://openfoodfacts.github.io/openfoodfacts-server/api/
 */
interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = FIELD_LIST
    ): OffResponse

    @GET("cgi/search.pl")
    suspend fun searchProduct(
        @Query("search_terms") terms: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 1,
        @Query("fields") fields: String = FIELD_LIST
    ): OffResponse

    companion object {
        const val FIELD_LIST =
            "product_name,generic_name,brands,quantity,categories_tags,image_front_small_url,image_url,image_front_url"
    }
}

data class OffResponse(
    val status: Int? = null,
    val product: OffProduct? = null,
    val products: List<OffProduct>? = null
)

data class OffProduct(
    @SerializedName("product_name") val name: String? = null,
    @SerializedName("generic_name") val genericName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerializedName("categories_tags") val categoriesTags: List<String>? = null,
    @SerializedName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    @SerializedName("image_front_url") val imageFrontUrl: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)
