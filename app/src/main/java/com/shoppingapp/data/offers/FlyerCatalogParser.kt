package com.shoppingapp.data.offers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant

/**
 * Market prospektüs listesinin sunucu tarafında gömülü JSON verisinden okur.
 * Sayfa kazımak yerine yalnızca tek bir JSON bloğu ayrıştırılır; ağ erişimi
 * [FlyerRepository] içindedir, bu sınıf saf ve birim testine uygundur.
 */
object FlyerCatalogParser {

    private val jsonScript = Regex(
        "<script[^>]*type=\"application/json\"[^>]*>(.*?)</script>",
        RegexOption.DOT_MATCHES_ALL
    )

    fun parse(html: String, store: FlyerStore, now: Instant = Instant.now()): List<Flyer> {
        val leaflets = jsonScript.findAll(html)
            .map { it.groupValues[1] }
            .mapNotNull { block -> runCatching { JsonParser.parseString(block) }.getOrNull() }
            .mapNotNull { findLeaflets(it) }
            .firstOrNull() ?: return emptyList()

        return order(leaflets.mapNotNull { toFlyer(it, store) }, now)
    }

    /** Tekrarları atar ve bu hafta → gelecek hafta → geçmiş sırasına dizer. */
    fun order(flyers: List<Flyer>, now: Instant = Instant.now()): List<Flyer> = flyers
        .distinctBy { it.leafletId }
        .sortedWith(compareBy<Flyer> { it.phase(now).ordinal }.thenByDescending { it.validFrom })

    private fun findLeaflets(element: JsonElement): JsonArray? = when {
        element.isJsonObject -> {
            val obj = element.asJsonObject
            val direct = obj.get("leaflets")
            if (direct != null && direct.isJsonArray) {
                direct.asJsonArray
            } else {
                obj.entrySet().firstNotNullOfOrNull { findLeaflets(it.value) }
            }
        }

        element.isJsonArray -> element.asJsonArray.firstNotNullOfOrNull { findLeaflets(it) }
        else -> null
    }

    private fun toFlyer(element: JsonElement, store: FlyerStore): Flyer? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject

        val leafletId = obj.long("mainLeafletId") ?: obj.long("id") ?: return null
        val pageCount = obj.int("pageCount") ?: return null
        if (pageCount <= 0) return null

        val validFrom = obj.instant("validFrom") ?: return null
        val validTo = obj.instant("validTo") ?: return null

        return Flyer(
            storeId = store.id,
            storeName = store.name,
            leafletId = leafletId,
            pageCount = pageCount,
            validFrom = validFrom,
            validTo = validTo,
            offerCount = obj.int("offerCount") ?: 0
        )
    }

    private fun JsonObject.primitive(name: String): JsonElement? =
        get(name)?.takeIf { it.isJsonPrimitive }

    private fun JsonObject.long(name: String): Long? =
        primitive(name)?.let { runCatching { it.asLong }.getOrNull() }

    private fun JsonObject.int(name: String): Int? =
        primitive(name)?.let { runCatching { it.asInt }.getOrNull() }

    private fun JsonObject.instant(name: String): Instant? =
        primitive(name)?.let { runCatching { Instant.parse(it.asString) }.getOrNull() }
}
