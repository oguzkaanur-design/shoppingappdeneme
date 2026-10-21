package com.shoppingapp.data.offers

/** Uygulamada prospektüsü gösterilen marketler ve resmî yedek adresleri. */
object FlyerStores {

    val all: List<FlyerStore> = listOf(
        FlyerStore(
            id = "rewe",
            name = "REWE",
            shortName = "REWE",
            brandColor = 0xFFD71920,
            catalogUrl = "https://www.marktguru.de/rp/rewe-prospekte",
            pageUrl = "https://www.rewe.de/angebote/nationale-angebote/"
        ),
        FlyerStore(
            id = "edeka",
            name = "EDEKA",
            shortName = "EDEKA",
            brandColor = 0xFF0050AA,
            catalogUrl = "https://www.marktguru.de/rp/edeka-prospekte",
            pageUrl = "https://www.edeka.de/angebote/"
        ),
        FlyerStore(
            id = "aldi-sued",
            name = "ALDI SÜD",
            shortName = "ALDI",
            brandColor = 0xFF0060A8,
            catalogUrl = "https://www.marktguru.de/rp/aldi-sued-prospekte",
            pageUrl = "https://www.aldi-sued.de/prospekte"
        ),
        FlyerStore(
            id = "lidl",
            name = "Lidl",
            shortName = "Lidl",
            brandColor = 0xFFCF202F,
            catalogUrl = "https://www.marktguru.de/rp/lidl-prospekte",
            pageUrl = "https://www.lidl.de/c/online-prospekte/s10005610"
        ),
        FlyerStore(
            id = "penny",
            name = "PENNY",
            shortName = "PENNY",
            brandColor = 0xFFE30613,
            catalogUrl = "https://www.marktguru.de/rp/penny-prospekte",
            pageUrl = "https://www.penny.de/angebote"
        )
    )

    fun byId(id: String): FlyerStore = all.firstOrNull { it.id == id } ?: all.first()
}
