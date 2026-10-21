package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Test

class FlyerPageUrlsTest {

    @Test
    fun buildsCoverFromTheFirstPageAtPreviewSize() {
        assertEquals(
            "https://cdn.marktguru.de/api/v1/leaflets/6393372/images/pages/0/medium.webp",
            FlyerPageUrls.cover(6393372L)
        )
    }

    @Test
    fun buildsHighResolutionPageUrlsByIndex() {
        assertEquals(
            "https://cdn.marktguru.de/api/v1/leaflets/6393372/images/pages/0/xlarge.webp",
            FlyerPageUrls.page(6393372L, 0)
        )
        assertEquals(
            "https://cdn.marktguru.de/api/v1/leaflets/6393372/images/pages/43/xlarge.webp",
            FlyerPageUrls.page(6393372L, 43)
        )
    }

    @Test
    fun allowsChoosingAPreviewSizeForPreloading() {
        assertEquals(
            "https://cdn.marktguru.de/api/v1/leaflets/10/images/pages/2/large.webp",
            FlyerPageUrls.page(10L, 2, size = "large")
        )
    }
}
