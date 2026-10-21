package com.shoppingapp.data.offers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Katalog sayfasının kendi JavaScript yapılandırmasında yayınlanan istemci bilgileri.
 * Anahtarlar uygulamaya gömülmez; her açılışta sayfadan okunur, böylece döndürülürse
 * uygulama güncellemesi gerekmez.
 */
data class MarktguruConfig(
    val apiHost: String,
    val apiKey: String,
    val clientKey: String
)

object MarktguruConfigParser {

    private val jsonScript = Regex(
        "<script[^>]*type=\"application/json\"[^>]*>(.*?)</script>",
        RegexOption.DOT_MATCHES_ALL
    )

    fun parse(html: String): MarktguruConfig? {
        val roots = jsonScript.findAll(html)
            .map { it.groupValues[1] }
            .mapNotNull { block -> runCatching { JsonParser.parseString(block) }.getOrNull() }

        for (root in roots) {
            val config = findConfig(root) ?: continue
            val host = config.string("apiHostAddress")?.takeIf { it.isNotBlank() } ?: continue
            val apiKey = config.string("apiKey")?.takeIf { it.isNotBlank() } ?: continue
            val clientKey = config.string("clientKey")?.takeIf { it.isNotBlank() } ?: continue
            return MarktguruConfig(host.removePrefix("https://").removePrefix("http://"), apiKey, clientKey)
        }
        return null
    }

    private fun findConfig(element: JsonElement): JsonObject? = when {
        element.isJsonObject -> {
            val obj = element.asJsonObject
            val direct = obj.get("config")?.takeIf { it.isJsonObject }?.asJsonObject
            if (direct != null && direct.has("apiKey")) {
                direct
            } else {
                obj.entrySet().firstNotNullOfOrNull { findConfig(it.value) }
            }
        }

        element.isJsonArray -> element.asJsonArray.firstNotNullOfOrNull { findConfig(it) }
        else -> null
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asString }.getOrNull() }
}
