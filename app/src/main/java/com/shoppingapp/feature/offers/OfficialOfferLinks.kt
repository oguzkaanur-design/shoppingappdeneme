package com.shoppingapp.feature.offers

internal object OfficialOfferLinks {
    data class StoreLink(val name: String, val description: String, val url: String)

    val stores = listOf(
        StoreLink("REWE", "Haftanın teklifleri ve prospektüs", "https://www.rewe.de/angebote/nationale-angebote/"),
        StoreLink("EDEKA", "Marketine göre teklifler ve prospektüs", "https://www.edeka.de/angebote/"),
        StoreLink("ALDI SÜD", "Güncel ve gelecek hafta prospektüsleri", "https://www.aldi-sued.de/prospekte"),
        StoreLink("Lidl", "Online prospektüsler ve aksiyonlar", "https://www.lidl.de/c/online-prospekte/s10005610"),
        StoreLink("PENNY", "Güncel haftalık teklifler", "https://www.penny.de/angebote")
    )
}
