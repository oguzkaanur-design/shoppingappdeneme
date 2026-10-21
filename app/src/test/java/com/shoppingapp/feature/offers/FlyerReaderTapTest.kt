package com.shoppingapp.feature.offers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.shoppingapp.data.offers.FlyerHotspot
import com.shoppingapp.data.offers.FlyerOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Okuyucudaki dokunuş → ürün eşleştirmesi. Koordinatlar sayfa görselinin
 * normalize edilmiş alanına çevrildiği için yakınlaştırma/kaydırma ile birlikte
 * test edilir.
 */
class FlyerReaderTapTest {

    private fun offer(id: Long, fromX: Float, fromY: Float, toX: Float, toY: Float): FlyerOffer =
        FlyerOffer(
            id = id,
            pageIndex = 0,
            name = "Ürün $id",
            description = "",
            price = 1.0,
            oldPrice = null,
            validFrom = null,
            validTo = null,
            hotspot = FlyerHotspot(id, 0, fromX, fromY, toX, toY)
        )

    private val offers = listOf(
        offer(1, 0.05f, 0.05f, 0.25f, 0.25f),
        offer(2, 0.60f, 0.60f, 0.90f, 0.90f)
    )

    @Test
    fun mapsTapIntoCenteredPage() {
        // Sayfa 500x500 olarak 1000x1000 alana ortalanmış: sol üst (250, 250).
        val hit = offerAt(
            position = Offset(325f, 325f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 1f,
            offset = Offset.Zero,
            offers = offers
        )

        assertEquals(1L, hit?.id)
    }

    @Test
    fun secondOfferIsFound() {
        val hit = offerAt(
            position = Offset(625f, 625f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 1f,
            offset = Offset.Zero,
            offers = offers
        )

        assertEquals(2L, hit?.id)
    }

    @Test
    fun accountsForZoomAndPan() {
        // 2x yakınlaştırma: görsel 1000x1000 olur, sol üst (0,0).
        val zoomed = offerAt(
            position = Offset(200f, 200f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 2f,
            offset = Offset.Zero,
            offers = offers
        )
        assertEquals(1L, zoomed?.id)

        // Sağa 200 px kaydırma: görselin sol üstü ekranda 200 px'te başlar, yani
        // 1. ürünün merkezi (0.15) 200 + 150 = 350 px'e denk gelir.
        val panned = offerAt(
            position = Offset(350f, 200f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 2f,
            offset = Offset(200f, 0f),
            offers = offers
        )
        assertEquals(1L, panned?.id)
    }

    @Test
    fun toleratesSmallMissNearMarker() {
        // Küçük alan (0.10–0.12) yüzünden işareti ıskalayan dokunuş, merkeze
        // yakın olduğu için yine ürüne sayılır.
        val tiny = listOf(offer(id = 7, fromX = 0.10f, fromY = 0.10f, toX = 0.12f, toY = 0.12f))
        val hit = offerAt(
            position = Offset(250f + 0.13f * 500f, 250f + 0.13f * 500f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 1f,
            offset = Offset.Zero,
            offers = tiny
        )

        assertEquals(7L, hit?.id)
    }

    @Test
    fun ignoresTapsOutsideThePage() {
        val hit = offerAt(
            position = Offset(300f, 900f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 1f,
            offset = Offset.Zero,
            offers = offers
        )

        assertNull(hit)
    }

    @Test
    fun ignoresOffersWithoutHotspot() {
        val withoutSpot = offers + FlyerOffer(
            id = 3,
            pageIndex = 0,
            name = "Alanı olmayan",
            description = "",
            price = 2.0,
            oldPrice = null,
            validFrom = null,
            validTo = null
        )

        val hit = offerAt(
            position = Offset(325f, 325f),
            area = IntSize(1000, 1000),
            display = 500f to 500f,
            scale = 1f,
            offset = Offset.Zero,
            offers = withoutSpot
        )

        assertEquals(1L, hit?.id)
    }
}
