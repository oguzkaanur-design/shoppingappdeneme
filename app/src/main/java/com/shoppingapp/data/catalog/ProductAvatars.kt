package com.shoppingapp.data.catalog

import com.shoppingapp.data.model.Product
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Listenin solundaki küçük görsel: ürünün türünü anlatan tek bir emoji.
 *
 * Gruplar bilinçli olarak **kategori kimliğine** (1 Meyve & Sebze … 5 Temizlik & Ev)
 * bağlıdır; böylece avatar değişince kategori tahmini de aynı yerden beslenir.
 * [keywords] üç dilde yazılır (Türkçe, Almanca, İngilizce) çünkü ürün adları
 * kullanıcıdan, barkod servisinden veya paylaşılan listeden her dilde gelebilir.
 */
data class ProductAvatar(
    val key: String,
    val emoji: String,
    val categoryId: Long,
    /** Seçim ekranındaki kısa ad (kategori adları gibi Türkçe). */
    val name: String,
    val keywords: List<String>
)

/**
 * Ürün adı → avatar eşlemesi.
 *
 * Eşleşme sırası:
 *  1. Kullanıcının seçtiği avatar ([Product.avatarKey]) her zaman kazanır.
 *  2. Ad içindeki kelimelere bakılır; tam kelime > kelime başı > kelime ile başlayan.
 *  3. Hiçbiri yoksa ürünün kategorisi için genel simge, o da yoksa 🛒.
 *
 * Alt dize araması yerine kelime bazlı arama yapılır: "lei" gibi kısa parçalar
 * "Reiniger" içinde eşleşip yanlış simge vermesin.
 */
object ProductAvatars {

    const val FALLBACK_EMOJI = "🛒"

    /** Kategori kimliği → genel simge; ürün adı tanınmadığında kullanılır. */
    private val categoryEmoji: Map<Long, String> = mapOf(
        1L to "🥬",
        2L to "🥛",
        3L to "🥤",
        4L to "🍫",
        5L to "🧽"
    )

    val all: List<ProductAvatar> = listOf(
        // ---- Meyve & sebze (1)
        avatar("fruit_apple", "🍎", 1L, "Elma", "apfel", "äpfel", "apple", "elma"),
        avatar("fruit_pear", "🍐", 1L, "Armut", "birne", "birnen", "pear", "armut"),
        avatar("fruit_banana", "🍌", 1L, "Muz", "banane", "bananen", "banana", "muz"),
        avatar(
            "fruit_strawberry", "🍓", 1L, "Çilek",
            "erdbeere", "erdbeeren", "strawberry", "cilek", "çilek"
        ),
        avatar(
            "fruit_grapes", "🍇", 1L, "Üzüm",
            "weintrauben", "trauben", "traube", "grapes", "grape", "uzum", "üzüm"
        ),
        avatar("fruit_lemon", "🍋", 1L, "Limon", "zitrone", "zitronen", "lemon", "limon"),
        avatar(
            "fruit_orange", "🍊", 1L, "Portakal",
            "orange", "orangen", "mandarine", "mandalina", "portakal"
        ),
        avatar("fruit_melon", "🍉", 1L, "Karpuz", "melone", "wassermelone", "melon", "karpuz"),
        avatar(
            "fruit_peach", "🍑", 1L, "Şeftali",
            "pfirsich", "nektarine", "peach", "seftali", "şeftali"
        ),
        avatar("fruit_pineapple", "🍍", 1L, "Ananas", "ananas", "pineapple"),
        avatar(
            "fruit_cherry", "🍒", 1L, "Kiraz",
            "kirsche", "kirschen", "cherry", "kiraz", "visne", "vişne"
        ),
        avatar("fruit_avocado", "🥑", 1L, "Avokado", "avocado", "avokado"),
        avatar(
            "produce_generic", "🧺", 1L, "Meyve & sebze",
            "obst", "gemuse", "gemüse", "meyve", "sebze", "fruit", "vegetable", "vegetables"
        ),
        avatar("veg_tomato", "🍅", 1L, "Domates", "tomate", "tomaten", "tomato", "domates"),
        avatar("veg_potato", "🥔", 1L, "Patates", "kartoffel", "kartoffeln", "potato", "patates"),
        avatar(
            "veg_carrot", "🥕", 1L, "Havuç",
            "karotte", "karotten", "möhre", "mohre", "carrot", "havuc", "havuç"
        ),
        avatar(
            "veg_cucumber", "🥒", 1L, "Salatalık",
            "gurke", "gurken", "cucumber", "salatalik", "salatalık"
        ),
        avatar("veg_onion", "🧅", 1L, "Soğan", "zwiebel", "zwiebeln", "onion", "sogan", "soğan"),
        avatar("veg_garlic", "🧄", 1L, "Sarımsak", "knoblauch", "garlic", "sarimsak", "sarımsak"),
        avatar("veg_pepper", "🫑", 1L, "Biber", "paprika", "paprikas", "biber"),
        avatar("veg_broccoli", "🥦", 1L, "Brokoli", "brokkoli", "broccoli"),
        avatar(
            "veg_leafy", "🥬", 1L, "Yeşillik",
            "salat", "kopfsalat", "lettuce", "marul", "yesillik", "yeşillik",
            "ispanak", "ıspanak", "spinach", "spinat", "roka", "rucola"
        ),
        avatar("veg_mushroom", "🍄", 1L, "Mantar", "pilz", "pilze", "champignon", "mushroom", "mantar"),
        avatar("veg_corn", "🌽", 1L, "Mısır", "mais", "corn", "misir", "mısır"),
        avatar(
            "veg_eggplant", "🍆", 1L, "Patlıcan",
            "aubergine", "eggplant", "patlican", "patlıcan"
        ),
        avatar(
            "herbs", "🌿", 1L, "Yeşil ot",
            "petersilie", "parsley", "maydanoz", "basilikum", "feslegen", "fesleğen",
            "nane", "minze", "mint", "dereotu", "dill", "kekik", "thyme"
        ),
        avatar(
            "nuts", "🥜", 1L, "Kuruyemiş",
            "nusse", "nüsse", "erdnuss", "ernusse", "nuts", "peanut", "findik", "fındık",
            "ceviz", "walnuss", "badem", "mandel", "almond"
        ),
        avatar(
            "olive_oil", "🫒", 1L, "Zeytin & yağ",
            "zeytin", "oliven", "olive", "zeytinyagi", "zeytinyağı", "olivenol"
        ),

        // ---- Fırın (1)
        avatar("bread", "🍞", 1L, "Ekmek", "brot", "brote", "bread", "ekmek", "toast"),
        avatar(
            "bread_rolls", "🥖", 1L, "Baget & sandviç",
            "baguette", "ciabatta", "brotchen", "brötchen", "semmel", "roll"
        ),
        avatar(
            "croissant", "🥐", 1L, "Kruvasan & börek",
            "croissant", "hornchen", "hörnchen", "borek", "börek", "pogaca", "poğaça"
        ),
        avatar("pretzel", "🥨", 1L, "Simit & pretzel", "brezel", "pretzel", "brezn", "simit"),

        // ---- Et & balık (1)
        avatar(
            "meat", "🥩", 1L, "Et",
            "fleisch", "rindfleisch", "hackfleisch", "steak", "schnitzel", "et",
            "kiyma", "kıyma", "kusbasi", "kuşbaşı"
        ),
        avatar(
            "chicken", "🍗", 1L, "Tavuk",
            "hahnchen", "hähnchen", "huhn", "chicken", "tavuk", "puten", "hindi"
        ),
        avatar(
            "fish", "🐟", 1L, "Balık",
            "fisch", "lachs", "thunfisch", "fish", "salmon", "balik", "balık", "somon"
        ),
        avatar(
            "deli", "🥓", 1L, "Şarküteri",
            "wurst", "salami", "schinken", "sausage", "sosis", "sucuk", "salam",
            "pastirma", "pastırma", "bacon", "speck"
        ),
        avatar(
            "seafood", "🍤", 1L, "Deniz ürünü",
            "garnele", "garnelen", "shrimp", "karides", "muschel", "midye"
        ),

        // ---- Süt ürünleri (2)
        avatar(
            "milk", "🥛", 2L, "Süt",
            "milch", "milk", "vollmilch", "hmilch", "buttermilch", "sut", "süt"
        ),
        avatar(
            "cheese", "🧀", 2L, "Peynir",
            "kase", "käse", "kaese", "cheese", "peynir", "gouda", "emmentaler",
            "mozzarella", "feta", "parmesan", "kasar", "kaşar", "tulum", "frischkase"
        ),
        avatar(
            "butter", "🧈", 2L, "Tereyağı",
            "butter", "margarine", "tereyagi", "tereyağı"
        ),
        avatar(
            "yogurt", "🥣", 2L, "Yoğurt",
            "joghurt", "yoghurt", "yogurt", "quark", "sahne", "kefir", "yogurd", "yoğurt"
        ),
        avatar(
            "eggs", "🥚", 2L, "Yumurta",
            "ei", "eier", "freilandeier", "bodenhaltungseier", "egg", "eggs", "yumurta"
        ),

        // ---- İçecekler (3)
        avatar(
            "water", "💧", 3L, "Su",
            "wasser", "water", "mineralwasser", "su", "suyu", "maden suyu",
            "icme suyu", "sodawasser"
        ),
        avatar(
            "juice", "🧃", 3L, "Meyve suyu",
            "saft", "juice", "smoothie", "meyve suyu", "portakal suyu", "elma suyu",
            "orangensaft", "apfelsaft"
        ),
        avatar(
            "softdrink", "🥤", 3L, "Gazlı içecek",
            "cola", "kola", "limonade", "soda", "fanta", "sprite", "gazoz", "energy"
        ),
        avatar("coffee", "☕", 3L, "Kahve", "kaffee", "coffee", "espresso", "kahve", "cappuccino"),
        avatar("tea", "🍵", 3L, "Çay", "tee", "tea", "cay", "çay", "eistee", "bitki cayi"),
        avatar("beer", "🍺", 3L, "Bira", "bier", "beer", "bira"),
        avatar("wine", "🍷", 3L, "Şarap", "wein", "wine", "sekt", "prosecco", "sarap", "şarap"),

        // ---- Atıştırmalık & temel gıda (4)
        avatar(
            "chocolate", "🍫", 4L, "Çikolata",
            "schokolade", "schoko", "chocolate", "praline", "cikolata", "çikolata", "nutella"
        ),
        avatar(
            "candy", "🍬", 4L, "Şeker",
            "bonbon", "bonbons", "candy", "sweets", "seker", "şeker", "lokum", "gummi"
        ),
        avatar(
            "icecream", "🍦", 4L, "Dondurma",
            "eis", "eiscreme", "icecream", "dondurma", "magnum"
        ),
        avatar(
            "sweets_cake", "🎂", 4L, "Pasta & kek",
            "kuchen", "torte", "cake", "kek", "tatli", "tatlı", "dessert"
        ),
        avatar(
            "cookies", "🍪", 4L, "Bisküvi",
            "keks", "platzchen", "plätzchen", "cookie", "cookies", "biscuit",
            "biskuvi", "bisküvi", "kurabiye"
        ),
        avatar(
            "honey_jam", "🍯", 4L, "Bal & reçel",
            "honig", "honey", "marmelade", "konfiture", "konfitüre", "jam", "bal",
            "recel", "reçel"
        ),
        avatar(
            "cereal", "🌾", 4L, "Müsli & yulaf",
            "musli", "müsli", "cornflakes", "cereal", "granola", "haferflocken",
            "oats", "gevrek", "yulaf"
        ),
        avatar("rice", "🍚", 4L, "Pirinç", "reis", "rice", "basmati", "pilav", "pirinc", "pirinç"),
        avatar(
            "pasta", "🍝", 4L, "Makarna",
            "nudeln", "nudel", "pasta", "spaghetti", "penne", "fusilli",
            "makarna", "eriste", "erişte"
        ),
        avatar(
            "sauce_can", "🥫", 4L, "Sos & konserve",
            "ketchup", "senf", "mustard", "hardal", "mayonez", "mayonnaise", "sauce",
            "sosse", "soße", "salca", "salça", "dose", "konserve", "canned"
        ),
        avatar(
            "spices", "🧂", 4L, "Baharat",
            "salz", "salt", "tuz", "pfeffer", "gewurz", "gewürz", "spices",
            "baharat", "curry", "zimt", "tarcin", "tarçın", "kimyon"
        ),
        avatar(
            "chips", "🍿", 4L, "Cips",
            "chips", "kartoffelchips", "cips", "popcorn"
        ),
        avatar("fries", "🍟", 4L, "Kızarmış patates", "pommes", "fries"),
        avatar("pizza", "🍕", 4L, "Pizza", "pizza"),
        avatar("burger", "🍔", 4L, "Hamburger", "burger", "hamburger"),
        avatar("sushi", "🍣", 4L, "Sushi", "sushi"),
        avatar(
            "noodles", "🍜", 4L, "Hazır erişte",
            "ramen", "instantnudeln", "noodles", "hazir eriste", "hazır erişte"
        ),
        avatar("soup", "🍲", 4L, "Çorba", "suppe", "soup", "corba", "çorba"),

        // ---- Temizlik & ev (5)
        avatar(
            "cleaning", "🧽", 5L, "Temizlik",
            "reiniger", "putzmittel", "spulmittel", "spülmittel", "detergent",
            "allzweck", "deterjan", "temizleyici", "domestos", "cif"
        ),
        avatar(
            "laundry", "🧺", 5L, "Çamaşır",
            "waschmittel", "weichspuler", "weichspüler", "laundry", "persil",
            "camasir", "çamaşır", "yumusatici", "yumuşatıcı"
        ),
        avatar(
            "paper", "🧻", 5L, "Kağıt ürünleri",
            "toilettenpapier", "klopapier", "toiletpaper", "kuchenrolle", "küchenrolle",
            "kitchenroll", "pecete", "peçete", "servietten", "tuvalet kagidi",
            "tuvalet kağıdı", "tissue"
        ),
        avatar(
            "trash_bags", "🗑️", 5L, "Çöp poşeti",
            "mullbeutel", "müllbeutel", "mullsack", "trashbag", "garbage",
            "cop poseti", "çöp poşeti"
        ),
        avatar(
            "cleaning_tools", "🧹", 5L, "Temizlik aleti",
            "besen", "broom", "mop", "eimer", "bucket", "schwamm", "sunger", "sünger",
            "supurge", "süpürge", "paspas", "kova"
        ),
        avatar(
            "soap", "🧼", 5L, "Sabun & duş",
            "seife", "soap", "deodorant", "deo", "duschgel", "shower", "sabun",
            "dus jeli", "duş jeli"
        ),
        avatar(
            "haircare", "🧴", 5L, "Şampuan & krem",
            "shampoo", "spulung", "creme", "crème", "lotion", "bodylotion",
            "sampuan", "şampuan", "krem", "sac kremi", "saç kremi"
        ),
        avatar(
            "toothcare", "🪥", 5L, "Diş bakımı",
            "zahnpasta", "zahnburste", "zahnbürste", "toothpaste", "toothbrush",
            "dis macunu", "diş macunu", "dis fircasi", "diş fırçası"
        ),
        avatar("makeup", "💄", 5L, "Makyaj", "lippenstift", "lipstick", "makeup", "makyaj", "rouge"),
        avatar(
            "shaving", "🪒", 5L, "Tıraş",
            "rasierer", "rasierklingen", "razor", "shaving", "tras", "tıraş", "jilet"
        ),
        avatar(
            "medicine", "💊", 5L, "İlaç & sağlık",
            "tabletten", "medikament", "medicine", "vitamin", "pflaster",
            "schmerzmittel", "ilac", "ilaç", "yara bandi", "yara bandı"
        ),
        avatar(
            "pet", "🐾", 5L, "Evcil hayvan",
            "hundefutter", "katzenfutter", "petfood", "leckerli", "kopek mamasi",
            "köpek maması", "kedi mamasi", "kedi maması"
        ),
        avatar(
            "baby", "🍼", 5L, "Bebek",
            "windel", "windeln", "diaper", "babybrei", "hipp", "milupa",
            "bebek bezi", "bebek mamasi", "bebek maması"
        ),
        avatar(
            "batteries", "🔋", 5L, "Pil",
            "batterie", "batterien", "battery", "batteries", "akku", "pil"
        ),
        avatar(
            "lightbulb", "💡", 5L, "Ampul",
            "gluhbirne", "glühbirne", "leuchtmittel", "lampe", "bulb", "led",
            "ampul", "ampül"
        ),
        avatar(
            "tools", "🔧", 5L, "Araç gereç",
            "werkzeug", "tools", "tool", "hammer", "schraubendreher", "bohrmaschine",
            "alet", "tornavida", "cekic", "çekiç", "matkap", "vidalama", "tamir", "civi"
        ),
        avatar(
            "stationery", "📚", 5L, "Kırtasiye",
            "kirtasiye", "kırtasiye", "stationery", "defter", "notizbuch", "notebook",
            "schulhefte", "kitap", "book", "buch", "kalemlik"
        ),
        avatar("flowers", "💐", 5L, "Çiçek", "blumen", "blume", "flowers", "flower", "cicek", "çiçek", "buket"),
        avatar("plant", "🪴", 5L, "Bitki", "pflanze", "pflanzen", "plant", "bitki", "saksi", "saksı"),
        avatar("gift", "🎁", 5L, "Hediye", "geschenk", "geschenke", "gift", "hediye"),
        avatar(
            "clothing", "👕", 5L, "Giyim",
            "socken", "socks", "tshirt", "unterwasche", "unterwäsche", "kleidung",
            "corap", "çorap", "ic camasiri", "iç çamaşırı", "giysi"
        )
    )

    private val byKeyMap: Map<String, ProductAvatar> = all.associateBy { it.key }

    /** Anahtar listesi bir kez normalize edilir; her ürün eklemede tekrar işlenmez. */
    private val searchIndex: List<Pair<ProductAvatar, List<String>>> = all.map { avatar ->
        avatar to avatar.keywords.map(::normalize).filter { it.isNotEmpty() }
    }

    fun byKey(key: String?): ProductAvatar? =
        key?.trim()?.takeIf { it.isNotEmpty() }?.let { byKeyMap[it] }

    /** Ürün adına en iyi uyan avatar; hiçbir kelime tanınmazsa null. */
    fun detect(name: String): ProductAvatar? {
        val text = normalize(name)
        if (text.isEmpty()) return null
        val tokens = text.split(' ').filter { it.isNotEmpty() }

        var best: ProductAvatar? = null
        var bestScore = 0
        searchIndex.forEach { (avatar, keywords) ->
            keywords.forEach { keyword ->
                val value = score(keyword, text, tokens)
                if (value > bestScore) {
                    bestScore = value
                    best = avatar
                }
            }
        }
        return best
    }

    /** Ürün adından kategori kimliği; tanınmazsa null. */
    fun categoryFor(name: String): Long? = detect(name)?.categoryId

    /** Listede gösterilecek simge: seçim > ad tahmini > kategori simgesi > 🛒. */
    fun emojiFor(product: Product): String =
        emojiFor(
            name = product.name,
            avatarKey = product.avatarKey,
            categoryId = product.categoryId
        )

    fun emojiFor(name: String, avatarKey: String? = null, categoryId: Long = 0L): String =
        byKey(avatarKey)?.emoji
            ?: detect(name)?.emoji
            ?: categoryEmoji[categoryId]
            ?: FALLBACK_EMOJI

    /**
     * Puanlama: iki kelimelik kalıp ("maden suyu") tek kelimeden daha güçlü,
     * sonra tam kelime, sonra kelime başı ("Tomaten" ≈ "tomate"), en zayıf
     * olarak da Türkçe ek almış hâl ("Ekmeği" ≈ "ekmek") ve kısaltma
     * ("schoko" → Schokolade).
     */
    private fun score(keyword: String, text: String, tokens: List<String>): Int {
        if (keyword.isEmpty()) return 0
        if (keyword.contains(' ')) {
            return if (" $text ".contains(" $keyword ")) 140 + keyword.length else 0
        }
        var best = 0
        tokens.forEach { token ->
            val current = when {
                token == keyword -> 120 + keyword.length
                keyword.length >= 4 && token.startsWith(keyword) -> 90 + keyword.length
                // Almanca bileşik adlar: "Vollkorn-brot", "Sonnenblumen-öl".
                keyword.length >= 4 && token.endsWith(keyword) -> 80 + keyword.length
                token.length >= 4 && keyword.startsWith(token) -> 70 + token.length
                else -> nearMatch(keyword, token)
            }
            if (current > best) best = current
        }
        return best
    }

    /**
     * "Ekmek" ↔ "Ekmeği", "Süt" ↔ "Sütü" gibi tek heceli ekleri tolere eder:
     * ilk dört harf aynı ve uzunluk farkı en fazla üçse eşleşme sayılır.
     */
    private fun nearMatch(keyword: String, token: String): Int {
        if (keyword.length < 3 || token.length < 3) return 0
        if (abs(keyword.length - token.length) > 2) return 0
        var common = 0
        while (common < keyword.length && common < token.length &&
            keyword[common] == token[common]
        ) {
            common++
        }
        return if (common >= 3) 50 + common else 0
    }

    private fun avatar(
        key: String,
        emoji: String,
        categoryId: Long,
        name: String,
        vararg keywords: String
    ): ProductAvatar = ProductAvatar(
        key = key,
        emoji = emoji,
        categoryId = categoryId,
        name = name,
        keywords = keywords.toList()
    )

    /**
     * Türkçe/Almanca harfleri ASCII'ye indirger ve noktalama işaretlerini boşluğa
     * çevirir; "Yoğurt", "yogurt" ve "YOĞURT" aynı metne düşer.
     */
    private fun normalize(value: String): String {
        val ascii = buildString(value.length) {
            value.lowercase(Locale.ROOT).forEach { ch ->
                append(
                    when (ch) {
                        'ı', 'î', 'i' -> 'i'
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
        }
        val stripped = Normalizer
            .normalize(ascii, Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        return stripped.replace(Regex("[^a-z0-9]+"), " ").trim()
    }
}
