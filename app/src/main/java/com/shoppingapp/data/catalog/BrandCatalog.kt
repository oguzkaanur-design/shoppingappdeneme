package com.shoppingapp.data.catalog

import java.util.Locale

/**
 * Almanya marketlerinin kendi markaları (Eigenmarken). Marka eşleşmeleri
 * marketlerin kendi ürün sayfalarından doğrulandı; doğrulanamayan kategoride
 * marka boş bırakılır ve öneri markasız üretilir (yanlış marka yazmamak için).
 *
 * Örn. Aldi Süd'ün süt ürünleri markası MILSANI, Lidl'inki Milbona'dır;
 * "ja!" Rewe'nin, GUT&GÜNSTIG Edeka'nın, MILPRIMA Penny'nin,
 * Gutes Land Netto'nun, K-Classic Kaufland'ın markasıdır.
 */
object BrandCatalog {

    enum class BrandSlot { DAIRY, WATER, JUICE, BEER, BAKERY, COFFEE, SNACKS, HOUSEHOLD, COSMETICS, EGGS }

    private val storeBrands: Map<Long, Map<BrandSlot, String>> = mapOf(
        1L to mapOf( // Rewe
            BrandSlot.DAIRY to "ja!",
            BrandSlot.BAKERY to "ja!",
            BrandSlot.EGGS to "ja!"
        ),
        2L to mapOf( // Edeka
            BrandSlot.DAIRY to "GUT&GÜNSTIG",
            BrandSlot.EGGS to "GUT&GÜNSTIG"
        ),
        3L to mapOf( // Aldi Süd
            BrandSlot.DAIRY to "MILSANI",
            BrandSlot.JUICE to "RIO D'ORO",
            BrandSlot.BEER to "KARLSKRONE",
            BrandSlot.BAKERY to "GOLDÄHREN",
            BrandSlot.COFFEE to "BARISSIMO",
            BrandSlot.SNACKS to "CHOCŒUR",
            BrandSlot.HOUSEHOLD to "TANDIL"
        ),
        4L to mapOf( // Lidl
            BrandSlot.DAIRY to "Milbona",
            BrandSlot.WATER to "Saskia"
        ),
        5L to mapOf( // Kaufland
            BrandSlot.DAIRY to "K-Classic",
            BrandSlot.HOUSEHOLD to "K-Classic"
        ),
        6L to mapOf( // Penny
            BrandSlot.DAIRY to "MILPRIMA"
        ),
        7L to mapOf( // Netto Marken-Discount
            BrandSlot.DAIRY to "Gutes Land"
        ),
        8L to mapOf( // dm
            BrandSlot.COSMETICS to "Balea",
            BrandSlot.HOUSEHOLD to "Denkmit"
        ),
        9L to mapOf( // Rossmann
            BrandSlot.COSMETICS to "ISANA",
            BrandSlot.HOUSEHOLD to "domol"
        )
    )

    /** Doğrulanmış, markete özel ek ürün adları. */
    private val storeSpecials: Map<Long, Map<String, List<String>>> = mapOf(
        1L to mapOf( // Rewe: Eigenmarken and quality-level variants
            "eggs" to listOf(
                "ja! Frische Eier aus Bodenhaltung 10 Stück",
                "REWE Beste Wahl Eier aus Freilandhaltung 6 Stück",
                "REWE Bio Eier 6 Stück",
                "ja! Bio Eier 6 Stück"
            )
        ),
        2L to mapOf( // Edeka
            "eggs" to listOf(
                "GUT&GÜNSTIG Eier aus Bodenhaltung 10 Stück",
                "GUT&GÜNSTIG Eier aus Freilandhaltung 10 Stück",
                "EDEKA Bio Eier 6 Stück"
            )
        ),
        3L to mapOf( // Aldi Süd
            "eggs" to listOf(
                "Frische Eier aus Bodenhaltung 10 Stück",
                "Frische Eier aus Freilandhaltung 10 Stück",
                "Bio Eier 6 Stück",
                "Eier aus Bodenhaltung 6 Stück"
            ),
            "yogurt" to listOf("MILSANI Joghurt nach türkischer Art 3,5% Fett")
        ),
        4L to mapOf( // Lidl
            "eggs" to listOf(
                "Frische Eier aus Bodenhaltung 10 Stück",
                "Frische Eier aus Freilandhaltung 10 Stück",
                "Bio Eier 6 Stück"
            )
        ),
        6L to mapOf( // Penny
            "eggs" to listOf(
                "Eier aus Bodenhaltung 10 Stück",
                "Eier aus Freilandhaltung 10 Stück",
                "Bio Eier 6 Stück"
            )
        )
    )

    data class Suggestion(
        val name: String,
        val categoryId: Long,
        val brand: String?
    )

    private data class Group(
        val key: String,
        val categoryId: Long,
        val slot: BrandSlot?,
        val keywords: List<String>,
        val templates: List<String>
    )

    private val groups: List<Group> = listOf(
        Group(
            key = "yogurt", categoryId = 2L, slot = BrandSlot.DAIRY,
            keywords = listOf("yogurt", "yoghurt", "joghurt", "yogurt", "yoğurt", "sahne", "sahnejoghurt"),
            templates = listOf(
                "{b} Joghurt 3,5% Fett",
                "{b} Joghurt mild 0,1% Fett",
                "{b} Sahnejoghurt 1,5% Fett",
                "{b} Griechischer Joghurt 10% Fett",
                "{b} Fruchtjoghurt Erdbeere 150 g"
            )
        ),
        Group(
            key = "milk", categoryId = 2L, slot = BrandSlot.DAIRY,
            keywords = listOf("sut", "süt", "milch", "milk", "vollmilch", "hmilch", "h-milch"),
            templates = listOf(
                "{b} H-Milch 1,5% Fett",
                "{b} Vollmilch 3,5% Fett",
                "{b} Frische fettarme Milch 1,5%",
                "{b} Bio H-Milch 3,8%",
                "{b} Laktosefreie Milch 1,5%"
            )
        ),
        Group(
            key = "butter", categoryId = 2L, slot = BrandSlot.DAIRY,
            keywords = listOf("tereyagi", "tereyağı", "butter"),
            templates = listOf(
                "{b} Deutsche Markenbutter 250 g",
                "{b} Süßrahmbutter 250 g",
                "{b} Streichzarte Butter 250 g"
            )
        ),
        Group(
            key = "cheese", categoryId = 2L, slot = BrandSlot.DAIRY,
            keywords = listOf("peynir", "kase", "käse", "kaese", "cheese", "gouda", "emmentaler"),
            templates = listOf(
                "{b} Gouda Scheiben 300 g",
                "{b} Emmentaler am Stück 400 g",
                "{b} Mozzarella 125 g",
                "{b} Feta 200 g"
            )
        ),
        Group(
            key = "eggs", categoryId = 1L, slot = BrandSlot.EGGS,
            keywords = listOf("yumurta", "yumurtalar", "ei", "eier", "freilandeier", "bodenhaltungseier", "egg", "eggs"),
            templates = listOf(
                "{b} Eier aus Bodenhaltung 10 Stück",
                "{b} Eier aus Freilandhaltung 10 Stück",
                "{b} Bio Eier 6 Stück",
                "{b} Eier aus Freilandhaltung 6 Stück",
                "{b} Bunte Eier gekocht und gefärbt 6 Stück"
            )
        ),
        Group(
            key = "water", categoryId = 3L, slot = BrandSlot.WATER,
            keywords = listOf("su", "wasser", "water", "mineralwasser", "maden suyu"),
            templates = listOf("{b} Mineralwasser 6×1,5 l Kasten", "{b} Mineralwasser still 1,5 l")
        ),
        Group(
            key = "juice", categoryId = 3L, slot = BrandSlot.JUICE,
            keywords = listOf("meyve suyu", "saft", "suyu", "orangensaft", "apfelsaft", "juice"),
            templates = listOf("{b} Orangensaft 1 l", "{b} Apfelsaft 1 l")
        ),
        Group(
            key = "beer", categoryId = 3L, slot = BrandSlot.BEER,
            keywords = listOf("bira", "bier", "beer"),
            templates = listOf("{b} Bier 6×0,5 l")
        ),
        Group(
            key = "bread", categoryId = 1L, slot = BrandSlot.BAKERY,
            keywords = listOf("ekmek", "brot", "bread", "toast", "sandwich"),
            templates = listOf(
                "{b} Toast 500 g",
                "{b} Vollkornbrot 500 g",
                "{b} Roggenmischbrot 750 g",
                "{b} Mehrkornbrötchen 6 Stück",
                "{b} Bauernbrot geschnitten 500 g"
            )
        ),
        Group(
            key = "coffee", categoryId = 4L, slot = BrandSlot.COFFEE,
            keywords = listOf("kahve", "kaffee", "coffee", "espresso"),
            templates = listOf("{b} Kaffee gemahlen 500 g", "{b} Löslicher Kaffee Gold")
        ),
        Group(
            key = "pasta", categoryId = 4L, slot = null,
            keywords = listOf("makarna", "nudeln", "pasta", "spaghetti", "penne", "fusilli"),
            templates = listOf(
                "Spaghetti 500 g",
                "Penne Rigate 500 g",
                "Fusilli 500 g",
                "Vollkornnudeln 500 g",
                "Bandnudeln 500 g"
            )
        ),
        Group(
            key = "chocolate", categoryId = 4L, slot = BrandSlot.SNACKS,
            keywords = listOf("cikolata", "çikolata", "schokolade", "chocolate", "schoko"),
            templates = listOf("{b} Schokolade Vollmilch 100 g", "{b} Nussschokolade 100 g")
        ),
        Group(
            key = "chips", categoryId = 4L, slot = BrandSlot.SNACKS,
            keywords = listOf("cips", "chips", "kartoffelchips"),
            templates = listOf("{b} Kartoffelchips Paprika 175 g")
        ),
        Group(
            key = "cleaning", categoryId = 5L, slot = BrandSlot.HOUSEHOLD,
            keywords = listOf("deterjan", "spulmittel", "spülmittel", "bulasik", "bulaşık", "reiniger", "allzweck", "temizleyici"),
            templates = listOf("{b} Spülmittel 500 ml", "{b} Allzweckreiniger 1 l")
        ),
        Group(
            key = "toiletpaper", categoryId = 5L, slot = BrandSlot.HOUSEHOLD,
            keywords = listOf("tuvalet kagidi", "tuvalet kağıdı", "toilettenpapier", "klopapier", "papier"),
            templates = listOf("{b} Toilettenpapier 3-lagig 10 Rollen")
        ),
        Group(
            key = "cosmetics", categoryId = 5L, slot = BrandSlot.COSMETICS,
            keywords = listOf("sampuan", "şampuan", "shampoo", "duschgel", "dus jeli", "creme", "krem"),
            templates = listOf("{b} Shampoo 300 ml", "{b} Duschgel 250 ml")
        )
    )

    /**
     * @param storeId seçilen market; null ise marka önerisi üretilmez.
     * @param query kullanıcının yazdığı ürün adı (Türkçe veya Almanca olabilir).
     */
    fun suggest(storeId: Long?, query: String, limit: Int = 4): List<Suggestion> {
        if (storeId == null || storeId <= 0L) return emptyList()
        val q = normalize(query)
        if (q.length < 2) return emptyList()

        val brands = storeBrands[storeId].orEmpty()
        // Yazılan kelime en iyi hangi ürün grubuna uyuyorsa sadece o grubun
        // önerileri gösterilir; böylece "süt" yazınca su önerisi gelmez.
        val group = matchGroups(q).firstOrNull() ?: return emptyList()
        val brand = group.slot?.let { brands[it] }
        val specials = storeSpecials[storeId]?.get(group.key).orEmpty()

        return (specials + group.templates)
            .map { render(it, brand) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(limit)
            .map { Suggestion(name = it, categoryId = group.categoryId, brand = brand) }
    }

    /** Ürün adından kategori tahmini; eşleşme yoksa null (genel görsel kullanılır). */
    fun detectCategory(query: String): Long? {
        val q = normalize(query)
        if (q.length < 2) return null
        return matchGroups(q).firstOrNull()?.categoryId
    }

    private fun matchGroups(q: String): List<Group> {
        val exact = groups.filter { group -> group.keywords.any { normalize(it) == q } }
        val prefix = groups.filter { group ->
            group !in exact && group.keywords.any { kw ->
                val k = normalize(kw)
                k.startsWith(q) || q.startsWith(k)
            }
        }
        val contains = groups.filter { group ->
            group !in exact && group !in prefix && group.keywords.any { kw ->
                q.contains(normalize(kw))
            }
        }
        return exact + prefix + contains
    }

    private fun render(template: String, brand: String?): String =
        if (brand.isNullOrBlank()) {
            template.replace("{b} ", "").replace("{b}", "").trim()
        } else {
            template.replace("{b}", brand)
        }

    private fun normalize(input: String): String {
        val replaced = input
            .replace('İ', 'i')
            .replace('I', 'i')
            .lowercase(Locale.ROOT)
        val sb = StringBuilder(replaced.length)
        replaced.forEach { ch ->
            sb.append(
                when (ch) {
                    'ı', 'î' -> 'i'
                    'ş' -> 's'
                    'ğ' -> 'g'
                    'ç' -> 'c'
                    'ö' -> 'o'
                    'ü', 'û' -> 'u'
                    'ä' -> 'a'
                    'ß' -> 's'
                    'é', 'è' -> 'e'
                    'â' -> 'a'
                    else -> ch
                }
            )
        }
        return sb.toString().trim().replace("ß", "ss")
    }
}
