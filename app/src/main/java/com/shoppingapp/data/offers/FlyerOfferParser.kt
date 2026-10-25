package com.shoppingapp.data.offers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shoppingapp.money.MoneyFormat
import java.time.Instant
import java.util.Locale

/**
 * Prospektüs tekliflerini ve sayfadaki tıklanabilir alanları ayrıştırır.
 * Ağ erişimi [FlyerOfferRepository] içindedir; bu sınıf saf ve test edilebilir.
 */
object FlyerOfferParser {

    /** `/api/v1/leaflets/{id}?as=mobiledetailed` yanıtındaki `children` listesi. */
    fun parseHotspots(leafletJson: String): List<FlyerHotspot> {
        val root = runCatching { JsonParser.parseString(leafletJson) }.getOrNull() ?: return emptyList()
        if (!root.isJsonObject) return emptyList()
        val children = root.asJsonObject.get("children")
            ?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()

        return children.mapNotNull { child ->
            if (!child.isJsonObject) return@mapNotNull null
            val obj = child.asJsonObject
            val offerId = obj.string("id")?.substringAfterLast('/')?.toLongOrNull() ?: return@mapNotNull null
            val pageIndex = obj.int("pageIndex") ?: return@mapNotNull null
            val coordinates = obj.get("coordinates")?.takeIf { it.isJsonObject }?.asJsonObject
                ?: return@mapNotNull null

            val fromX = coordinates.float("fromX") ?: return@mapNotNull null
            val fromY = coordinates.float("fromY") ?: return@mapNotNull null
            val toX = coordinates.float("tox") ?: return@mapNotNull null
            val toY = coordinates.float("toy") ?: return@mapNotNull null
            if (toX <= fromX || toY <= fromY) return@mapNotNull null

            FlyerHotspot(offerId, pageIndex, fromX, fromY, toX, toY)
        }
    }

    fun parseLeafletName(leafletJson: String): String? {
        val root = runCatching { JsonParser.parseString(leafletJson) }.getOrNull() ?: return null
        return if (root.isJsonObject) root.asJsonObject.string("name")?.takeIf { it.isNotBlank() } else null
    }

    /** `/api/v1/offers?leafletId=…&pageIndex=…&as=mobiledetailed` yanıtı. */
    fun parsePageOffers(offersJson: String, pageIndex: Int): List<FlyerOffer> {
        val root = runCatching { JsonParser.parseString(offersJson) }.getOrNull() ?: return emptyList()
        if (!root.isJsonObject) return emptyList()
        val results = root.asJsonObject.get("results")
            ?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()

        return results.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val obj = item.asJsonObject
            val id = obj.long("id") ?: return@mapNotNull null
            val brand = obj.get("brand")?.takeIf { it.isJsonObject }?.asJsonObject?.string("name")
            val product = obj.get("product")?.takeIf { it.isJsonObject }?.asJsonObject?.string("name")
            val description = obj.string("description").orEmpty()
            val validity = obj.get("validityDates")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject

            FlyerOffer(
                id = id,
                pageIndex = obj.int("pageIndex") ?: pageIndex,
                name = displayName(brand, product, description),
                description = description,
                price = obj.double("price"),
                oldPrice = obj.double("oldPrice"),
                validFrom = validity?.string("from")?.let { parseInstant(it) },
                validTo = validity?.string("to")?.let { parseInstant(it) }
            )
        }
    }

    /** Teklifleri sayfadaki alanlarla eşleştirir; alanı olmayan teklifler yine döner. */
    fun attachHotspots(offers: List<FlyerOffer>, hotspots: List<FlyerHotspot>): List<FlyerOffer> {
        val byOfferId = hotspots.associateBy { it.offerId }
        return offers.map { it.withHotspot(byOfferId[it.id]) }
    }

    /**
     * Ürün adı: marka + ürün. Ürün alanı bazen "~twxau~" gibi yer tutucu olur;
     * o durumda marka, o da yoksa açıklamanın ilk kısa cümlesi kullanılır.
     */
    fun displayName(brand: String?, product: String?, description: String): String {
        val cleanBrand = brand?.trim()?.takeIf { it.isNotBlank() && !it.contains('~') }
        val cleanProduct = product?.trim()?.takeIf { it.isNotBlank() && !it.contains('~') }

        if (cleanBrand != null && cleanProduct != null) return "$cleanBrand $cleanProduct"
        if (cleanProduct != null) return cleanProduct
        if (cleanBrand != null) return cleanBrand

        val fromDescription = description
            // Yalnızca virgülden sonra boşluk varsa cümle sanılır; "0,10 EUR"
            // gibi ondalık virgüllü fiyatlar adın ortasında kesilmesin.
            .substringBefore(", ")
            .substringBefore(" je ")
            .trim()
            .take(48)
            .trim()
        return fromDescription.ifBlank { "Ürün" }
    }

    private fun parseInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

    private fun JsonObject.primitive(name: String): JsonElement? =
        get(name)?.takeIf { it.isJsonPrimitive }

    private fun JsonObject.string(name: String): String? =
        primitive(name)?.let { runCatching { it.asString }.getOrNull() }

    private fun JsonObject.long(name: String): Long? =
        primitive(name)?.let { runCatching { it.asLong }.getOrNull() }

    private fun JsonObject.int(name: String): Int? =
        primitive(name)?.let { runCatching { it.asInt }.getOrNull() }

    private fun JsonObject.double(name: String): Double? =
        primitive(name)?.let { runCatching { it.asDouble }.getOrNull() }

    private fun JsonObject.float(name: String): Float? =
        primitive(name)?.let { runCatching { it.asFloat }.getOrNull() }
}

/** Fiyatları seçili para biriminin yerel biçiminde gösterir: 1,19 € · 34,90 ₺ */
object FlyerPriceLabel {
    fun format(price: Double?): String? =
        price?.let { MoneyFormat.amount(it) }
}
