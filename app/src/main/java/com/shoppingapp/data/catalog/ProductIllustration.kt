package com.shoppingapp.data.catalog

import java.text.Normalizer
import java.util.Locale

/** Uses a recognizable product symbol instead of a misleading generic stock photo. */
object ProductIllustration {
    fun forProductName(name: String): String {
        val q = normalize(name)
        return when {
            has(q, "nudel", "pasta", "makarna", "spaghetti", "penne", "fusilli") -> "🍝"
            has(q, "eier", "egg", "yumurta") -> "🥚"
            has(q, "milch", "milk", "sut", "joghurt", "yogurt", "kase", "käse", "kaese", "cheese", "butter", "tereyag") -> "🥛"
            has(q, "wasser", "water", "saft", "juice", "getrank", "bira", "bier", "beer", "drink") -> "🥤"
            has(q, "brot", "bread", "toast", "ekmek") -> "🍞"
            has(q, "bunte eier", "colored egg", "gekocht", "boiled") -> "🥚"
            has(q, "apfel", "elma") -> "🍎"
            has(q, "birne", "pear", "armut") -> "🍐"
            has(q, "erdbeere", "strawberry", "cilek", "çilek") -> "🍓"
            has(q, "orange", "portakal") -> "🍊"
            has(q, "banane", "muz") -> "🍌"
            has(q, "kartoffel", "patates") -> "🥔"
            has(q, "tomate", "domates") -> "🍅"
            has(q, "karotte", "carrot", "havuç", "havuc") -> "🥕"
            has(q, "gurke", "salatalik", "salatalık") -> "🥒"
            has(q, "paprika", "biber") -> "🫑"
            has(q, "zwiebel", "onion", "sogan", "soğan") -> "🧅"
            has(q, "knoblauch", "garlic", "sarimsak", "sarımsak") -> "🧄"
            has(q, "brokkoli", "broccoli") -> "🥦"
            has(q, "obst", "gemuse", "meyve", "sebze") -> "🥬"
            has(q, "reiniger", "spulmittel", "deterjan", "haushalt", "temizleyici") -> "🧽"
            has(q, "schokolade", "chocolate", "snack", "chips", "cips") -> "🍫"
            else -> "🛒"
        }
    }

    private fun has(text: String, vararg keywords: String): Boolean = keywords.any { normalize(it) in text }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        .replace('ß', 's')
}
