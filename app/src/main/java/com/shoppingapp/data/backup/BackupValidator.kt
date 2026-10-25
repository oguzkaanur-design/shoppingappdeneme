package com.shoppingapp.data.backup

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * İçe aktarılan yedek, uygulamanın kendi ürettiği dosya olmak zorunda değil:
 * kullanıcı WhatsApp'tan, indirilenler klasöründen veya başka bir uygulamadan
 * herhangi bir JSON seçebilir. Bu yüzden Gson'a dokunmadan önce boyut, şema ve
 * tablo sayıları doğrulanır; eksik tablolar boş listeye tamamlanır.
 *
 * Sınırlar aynı zamanda bir hizmet engelleme (DoS) korumasıdır: 8 MB'tan büyük
 * dosya okunmaz, tek tabloda 20 binden fazla kayıt kabul edilmez.
 */
internal object BackupValidator {

    const val MAX_BYTES = 8 * 1024 * 1024
    const val MAX_ITEMS_PER_TABLE = 20_000
    const val MAX_ITEMS_TOTAL = 60_000
    const val CURRENT_SCHEMA = 1

    private val tables = listOf(
        "products",
        "stores",
        "categories",
        "listItems",
        "pantryItems",
        "purchaseHistory",
        "purchaseHistoryItems",
        "pfandEntries",
        "moneyEntries",
        "flyerListItems"
    )

    private enum class FieldType { STRING, NUMBER, BOOLEAN }

    private data class Field(
        val name: String,
        val type: FieldType,
        val nullable: Boolean = false,
        val nonBlank: Boolean = false
    )

    /**
     * Gson, Kotlin'un non-null alanlarına JSON'dan `null` yazabildiği için satır
     * alanları Gson'a gitmeden doğrulanır. Bu kurallar uygulamanın kendi ürettiği
     * yedeğin tüm zorunlu alanlarını kapsar; nullable alanlar atlanabilir.
     */
    private val rowRules = mapOf(
        "products" to listOf(
            Field("id", FieldType.NUMBER),
            Field("name", FieldType.STRING, nonBlank = true),
            Field("barcode", FieldType.STRING, nonBlank = true),
            Field("categoryId", FieldType.NUMBER),
            Field("imageUrl", FieldType.STRING, nullable = true),
            Field("unit", FieldType.STRING, nonBlank = true),
            Field("defaultStoreId", FieldType.NUMBER, nullable = true),
            Field("pfandAmount", FieldType.NUMBER),
            // Eski yedeklerde bu alan yoksa Gson 0.0'a düşürür.
            Field("priceEuro", FieldType.NUMBER, nullable = true),
            // Kullanıcının seçtiği avatar; eski yedeklerde yoksa otomatik tahmin kullanılır.
            Field("avatarKey", FieldType.STRING, nullable = true)
        ),
        "stores" to listOf(
            Field("id", FieldType.NUMBER),
            Field("name", FieldType.STRING, nonBlank = true),
            Field("colorHex", FieldType.STRING, nonBlank = true),
            Field("aisleOrderJson", FieldType.STRING)
        ),
        "categories" to listOf(
            Field("id", FieldType.NUMBER),
            Field("nameDe", FieldType.STRING, nonBlank = true),
            Field("nameTr", FieldType.STRING, nonBlank = true),
            Field("iconName", FieldType.STRING),
            Field("colorHex", FieldType.STRING, nonBlank = true)
        ),
        "listItems" to listOf(
            Field("id", FieldType.NUMBER),
            Field("productId", FieldType.NUMBER),
            Field("storeId", FieldType.NUMBER),
            Field("quantity", FieldType.NUMBER),
            Field("isPurchased", FieldType.BOOLEAN),
            Field("addedAt", FieldType.NUMBER)
        ),
        "pantryItems" to listOf(
            Field("id", FieldType.NUMBER),
            Field("productId", FieldType.NUMBER),
            Field("currentQuantity", FieldType.NUMBER),
            Field("minThreshold", FieldType.NUMBER),
            Field("expiryDate", FieldType.NUMBER, nullable = true)
        ),
        "purchaseHistory" to listOf(
            Field("id", FieldType.NUMBER),
            Field("storeId", FieldType.NUMBER),
            Field("totalAmount", FieldType.NUMBER),
            Field("pfandTotal", FieldType.NUMBER),
            Field("purchaseDate", FieldType.NUMBER)
        ),
        "purchaseHistoryItems" to listOf(
            Field("id", FieldType.NUMBER),
            Field("historyId", FieldType.NUMBER),
            Field("productId", FieldType.NUMBER),
            Field("priceEuro", FieldType.NUMBER),
            Field("quantity", FieldType.NUMBER)
        ),
        "pfandEntries" to listOf(
            Field("id", FieldType.NUMBER),
            Field("label", FieldType.STRING, nonBlank = true),
            Field("unitAmount", FieldType.NUMBER),
            Field("quantity", FieldType.NUMBER),
            Field("createdAt", FieldType.NUMBER),
            Field("isReturned", FieldType.BOOLEAN)
        ),
        "moneyEntries" to listOf(
            Field("id", FieldType.NUMBER),
            Field("account", FieldType.STRING, nonBlank = true),
            Field("amount", FieldType.NUMBER),
            Field("note", FieldType.STRING),
            Field("createdAt", FieldType.NUMBER)
        ),
        "flyerListItems" to listOf(
            Field("id", FieldType.NUMBER),
            Field("offerId", FieldType.NUMBER),
            Field("storeName", FieldType.STRING, nonBlank = true),
            Field("storeColor", FieldType.NUMBER),
            Field("productName", FieldType.STRING, nonBlank = true),
            Field("description", FieldType.STRING),
            Field("unitPrice", FieldType.NUMBER),
            Field("quantity", FieldType.NUMBER),
            Field("imagePath", FieldType.STRING, nullable = true),
            Field("validFromEpochMs", FieldType.NUMBER),
            Field("validToEpochMs", FieldType.NUMBER),
            Field("addedAt", FieldType.NUMBER)
        )
    )

    /** Akışı en fazla [maxBytes] bayt okur; sınır aşılırsa okumayı keser. */
    fun readLimited(input: InputStream, maxBytes: Int = MAX_BYTES): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= maxBytes) { "Yedek dosyası çok büyük (en fazla 8 MB)." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    /**
     * Yedeği doğrular ve eksik tabloları boş diziyle tamamlayıp yeniden yazar.
     * Gson, eksik alanları Kotlin varsayılanlarıyla değil `null` ile bırakabildiği
     * için bu adım geri yükleme sırasında yarım kalan işlemi de engeller.
     */
    fun normalize(json: String): String {
        require(json.isNotBlank()) { "Dosya boş." }
        require(json.length <= MAX_BYTES) { "Yedek dosyası çok büyük (en fazla 8 MB)." }

        val parsed = try {
            JsonParser.parseString(json)
        } catch (error: RuntimeException) {
            throw IllegalArgumentException("Dosya geçerli bir JSON değil.", error)
        }
        require(parsed.isJsonObject) { "Dosya bir Korb yedeği değil." }
        val root = parsed.asJsonObject

        val version = root.get("schemaVersion")?.let { element ->
            require(element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
                "Yedek sürümü okunamadı."
            }
            element.asInt
        } ?: CURRENT_SCHEMA
        require(version in 1..CURRENT_SCHEMA) {
            "Bu yedek daha yeni bir uygulama sürümünden geliyor (şema $version)."
        }

        var total = 0
        tables.forEach { name ->
            val existing = root.get(name)
            if (existing == null || existing.isJsonNull) {
                root.add(name, JsonArray())
            } else {
                require(existing.isJsonArray) { "Yedekteki '$name' alanı liste değil." }
                val size = existing.asJsonArray.size()
                require(size <= MAX_ITEMS_PER_TABLE) { "Yedekteki '$name' listesi çok büyük." }
                total += size
            }
        }
        require(total <= MAX_ITEMS_TOTAL) { "Yedekte toplam çok fazla kayıt var." }

        val products = root.getAsJsonArray("products")?.size() ?: 0
        val stores = root.getAsJsonArray("stores")?.size() ?: 0
        require(products > 0 || stores > 0) { "Yedekte ürün veya market verisi yok." }

        validateRows(root)
        return root.toString()
    }

    private fun validateRows(root: JsonObject) {
        tables.forEach { table ->
            val rows = root.getAsJsonArray(table)
            val rules = rowRules.getValue(table)
            val ids = HashSet<Long>(rows.size())

            rows.forEachIndexed { index, element ->
                val position = index + 1
                require(element.isJsonObject) {
                    "Yedekteki '$table' $position. kayıt nesne değil."
                }
                val row = element.asJsonObject

                rules.forEach { field ->
                    val value = row.get(field.name)
                    if (value == null || value.isJsonNull) {
                        require(field.nullable) {
                            "Yedekteki '$table' $position. kayıtta '${field.name}' eksik."
                        }
                    } else {
                        validateField(table, position, field, value)
                    }
                }

                val idElement = row.get("id")
                val id = idElement
                    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                    ?.let { runCatching { it.asLong }.getOrNull() }
                if (id == null || id <= 0L) {
                    throw IllegalArgumentException(
                        "Yedekteki '$table' $position. kaydın kimliği geçersiz."
                    )
                }
                require(ids.add(id)) {
                    "Yedekteki '$table' tablosunda yinelenen kimlik: $id."
                }

                if (table == "moneyEntries") {
                    val account = row.get("account").asString
                    require(account == "CASH" || account == "BANK") {
                        "Yedekteki 'moneyEntries' $position. kaydın hesabı geçersiz: $account."
                    }
                }
            }
        }
    }

    private fun validateField(
        table: String,
        position: Int,
        field: Field,
        element: JsonElement
    ) {
        require(element.isJsonPrimitive) {
            "Yedekteki '$table' $position. kayıtta '${field.name}' yanlış tipte."
        }
        val primitive = element.asJsonPrimitive
        when (field.type) {
            FieldType.STRING -> {
                require(primitive.isString) {
                    "Yedekteki '$table' $position. kayıtta '${field.name}' metin olmalı."
                }
                if (field.nonBlank) {
                    require(primitive.asString.isNotBlank()) {
                        "Yedekteki '$table' $position. kayıtta '${field.name}' boş olamaz."
                    }
                }
            }

            FieldType.NUMBER -> {
                require(primitive.isNumber) {
                    "Yedekteki '$table' $position. kayıtta '${field.name}' sayı olmalı."
                }
                require(primitive.asDouble.isFinite()) {
                    "Yedekteki '$table' $position. kayıtta '${field.name}' geçersiz sayı."
                }
            }

            FieldType.BOOLEAN -> {
                require(primitive.isBoolean) {
                    "Yedekteki '$table' $position. kayıtta '${field.name}' true/false olmalı."
                }
            }
        }
    }
}
