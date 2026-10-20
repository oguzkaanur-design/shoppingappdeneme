package com.shoppingapp.feature.offers

internal object OfficialOfferLinks {
    data class StoreLink(
        val name: String,
        val shortName: String,
        val description: String,
        val url: String,
        val brandColor: Long
    )

    val stores = listOf(
        StoreLink("REWE", "REWE", "Haftanın teklifleri ve prospektüs", "https://www.rewe.de/angebote/nationale-angebote/", 0xFFD71920),
        StoreLink("EDEKA", "EDEKA", "Marketine göre teklifler ve prospektüs", "https://www.edeka.de/angebote/", 0xFF0050AA),
        StoreLink("ALDI SÜD", "ALDI", "Güncel ve gelecek hafta prospektüsleri", "https://www.aldi-sued.de/prospekte", 0xFF0060A8),
        StoreLink("Lidl", "Lidl", "Online prospektüsler ve aksiyonlar", "https://www.lidl.de/c/online-prospekte/s10005610", 0xFFCF202F),
        StoreLink("PENNY", "PENNY", "Güncel haftalık teklifler", "https://www.penny.de/angebote", 0xFFE30613)
    )
}
