package com.shoppingapp.data.aisle

import com.google.gson.JsonParser

/**
 * Market bazlı reyon sıralaması.
 *
 * [com.shoppingapp.data.model.Store.aisleOrderJson] kategori kimliklerini marketin
 * gerçek reyon sırasına göre tutar (ör. Aldi Süd'de manav girişte, içecekler en
 * arkada). Liste markete göre filtrelendiğinde ürünler bu sırayla gruplanır;
 * böylece markette gereksiz gidiş-geliş olmaz.
 */
object AisleOrder {

    /**
     * Bozuk/eski JSON verisini sessizce yok sayar; sıra yoksa liste boş döner.
     * Ayrıştırma Gson ile yapılır: saf JVM testlerinde çalışır, Android'a bağlı değildir.
     */
    fun parse(json: String?): List<Long> {
        if (json.isNullOrBlank()) return emptyList()
        val array = runCatching { JsonParser.parseString(json).asJsonArray }.getOrNull()
            ?: return emptyList()
        val ids = mutableListOf<Long>()
        array.forEach { element ->
            val id = runCatching {
                if (element.isJsonPrimitive) element.asString.trim().toLongOrNull() else null
            }.getOrNull()
            if (id != null && id > 0 && !ids.contains(id)) ids.add(id)
        }
        return ids
    }

    /**
     * Reyon sırasını, o an listede bulunan kategorilerle sınırlar. Sırada olmayan
     * kategoriler sona eklenir: yeni bir kategori eklendiğinde ürün kaybolmaz.
     */
    fun forCategories(order: List<Long>, available: Collection<Long>): List<Long> {
        val known = order.filter { available.contains(it) }
        val rest = available.filter { !known.contains(it) }.sorted()
        return known + rest
    }

    /**
     * Ürünleri reyon sırasına göre gruplar. Sırada yazılmayan kategoriler kendi
     * grubuyla sona eklenir, kategorisi hiç olmayan ürünler en sonda tek bir
     * "Diğer" grubunda toplanır; hiçbir ürün düşürülmez.
     */
    fun <T> group(
        items: List<T>,
        order: List<Long>,
        categoryOf: (T) -> Long?
    ): List<AisleGroup<T>> {
        if (items.isEmpty()) return emptyList()
        val byCategory = items.groupBy { categoryOf(it) }
        val ordered = forCategories(order, byCategory.keys.filterNotNull())
        val groups = mutableListOf<AisleGroup<T>>()

        ordered.forEach { categoryId ->
            val groupItems = byCategory[categoryId].orEmpty()
            if (groupItems.isNotEmpty()) {
                groups.add(
                    AisleGroup(
                        // Numara, marketin reyon sırasındaki gerçek konumdur: listede
                        // tek kategori olsa bile "Reyon 2" doğru bilgi verir.
                        aisleNumber = ordered.indexOf(categoryId) + 1,
                        categoryId = categoryId,
                        items = groupItems
                    )
                )
            }
        }

        val rest = byCategory[null].orEmpty() + byCategory
            .filterKeys { it != null && !ordered.contains(it) }
            .values
            .flatten()
        if (rest.isNotEmpty()) {
            groups.add(
                AisleGroup(
                    aisleNumber = ordered.size + 1,
                    categoryId = null,
                    items = rest
                )
            )
        }
        return groups
    }
}

/** Listedeki tek bir reyon bölümü. */
data class AisleGroup<T>(
    val aisleNumber: Int,
    val categoryId: Long?,
    val items: List<T>
)
