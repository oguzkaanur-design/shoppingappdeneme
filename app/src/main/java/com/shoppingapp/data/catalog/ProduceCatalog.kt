package com.shoppingapp.data.catalog

import java.text.Normalizer
import java.util.Locale

/** Common German supermarket produce, displayed with Turkish and German names. */
object ProduceCatalog {
    data class Item(val turkish: String, val german: String)

    val items = listOf(
        Item("Elma", "Apfel"), Item("Armut", "Birne"), Item("Muz", "Banane"),
        Item("Portakal", "Orange"), Item("Mandalina", "Mandarine"), Item("Limon", "Zitrone"),
        Item("Misket limonu", "Limette"), Item("Greyfurt", "Grapefruit"), Item("Üzüm", "Weintrauben"),
        Item("Çilek", "Erdbeere"), Item("Yaban mersini", "Heidelbeere"), Item("Ahududu", "Himbeere"),
        Item("Böğürtlen", "Brombeere"), Item("Kiraz", "Kirsche"), Item("Vişne", "Sauerkirsche"),
        Item("Şeftali", "Pfirsich"), Item("Nektarin", "Nektarine"), Item("Kayısı", "Aprikose"),
        Item("Erik", "Pflaume"), Item("Kivi", "Kiwi"), Item("Ananas", "Ananas"),
        Item("Mango", "Mango"), Item("Avokado", "Avocado"), Item("Karpuz", "Wassermelone"),
        Item("Kavun", "Honigmelone"), Item("Nar", "Granatapfel"), Item("Trabzon hurması", "Kaki"),
        Item("Hindistan cevizi", "Kokosnuss"), Item("Patates", "Kartoffel"), Item("Tatlı patates", "Süßkartoffel"),
        Item("Soğan", "Zwiebel"), Item("Kırmızı soğan", "Rote Zwiebel"), Item("Sarımsak", "Knoblauch"),
        Item("Havuç", "Karotte"), Item("Salatalık", "Gurke"), Item("Domates", "Tomate"),
        Item("Cherry domates", "Cherrytomaten"), Item("Biber", "Paprika"), Item("Acı biber", "Chili"),
        Item("Kabak", "Zucchini"), Item("Patlıcan", "Aubergine"), Item("Brokoli", "Brokkoli"),
        Item("Karnabahar", "Blumenkohl"), Item("Lahana", "Weißkohl"), Item("Kırmızı lahana", "Rotkohl"),
        Item("Savoy lahanası", "Wirsing"), Item("Brüksel lahanası", "Rosenkohl"), Item("Kırmızı turp", "Radieschen"),
        Item("Turp", "Rettich"), Item("Marul", "Kopfsalat"), Item("Iceberg marul", "Eisbergsalat"),
        Item("Roka", "Rucola"), Item("Ispanak", "Spinat"), Item("Mantar", "Champignon"),
        Item("İstiridye mantarı", "Austernpilz"), Item("Pırasa", "Lauch"), Item("Kereviz", "Sellerie"),
        Item("Kereviz kökü", "Knollensellerie"), Item("Rezene", "Fenchel"), Item("Kuşkonmaz", "Spargel"),
        Item("Yeşil kuşkonmaz", "Grüner Spargel"), Item("Taze fasulye", "Grüne Bohnen"), Item("Bezelye", "Erbsen"),
        Item("Mısır", "Mais"), Item("Balkabağı", "Kürbis"), Item("Pancar", "Rote Bete"),
        Item("Şeker bezelyesi", "Zuckerschoten"), Item("Taze soğan", "Frühlingszwiebeln"),
        Item("Yer lahanası", "Kohlrabi"), Item("Maydanoz", "Petersilie"), Item("Dereotu", "Dill"),
        Item("Fesleğen", "Basilikum"), Item("Frenk soğanı", "Schnittlauch"), Item("Nane", "Minze"),
        Item("Kişniş", "Koriander")
    )

    fun search(query: String): List<Item> {
        val q = normalize(query)
        if (q.isBlank()) return items
        return items.filter { normalize(it.turkish).contains(q) || normalize(it.german).contains(q) }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        .replace("ß", "ss")
        .trim()
}
