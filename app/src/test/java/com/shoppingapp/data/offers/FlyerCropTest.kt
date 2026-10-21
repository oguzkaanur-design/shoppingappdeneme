package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlyerCropTest {

    private fun spot(fromX: Float, fromY: Float, toX: Float, toY: Float) =
        FlyerHotspot(offerId = 1L, pageIndex = 0, fromX = fromX, fromY = fromY, toX = toX, toY = toY)

    @Test
    fun mapsNormalizedAreaToPagePixels() {
        val rect = FlyerCrop.rectFor(spot(0.178f, 0.253f, 0.671f, 0.443f), 1080, 1909)

        assertEquals(192, rect.left)
        assertEquals(483, rect.top)
        assertEquals(725, rect.right)
        assertEquals(846, rect.bottom)
    }

    @Test
    fun clampsAreasThatReachThePageEdge() {
        val rect = FlyerCrop.rectFor(spot(0.99f, 0.99f, 1.2f, 1.2f), 1080, 1909)

        assertEquals(1069, rect.left)
        assertTrue(rect.right <= 1080)
        assertTrue(rect.bottom <= 1909)
        assertTrue(rect.right > rect.left)
        assertTrue(rect.bottom > rect.top)
    }

    @Test
    fun keepsAtLeastOnePixelForTinyAreas() {
        val rect = FlyerCrop.rectFor(spot(0.5f, 0.5f, 0.5f, 0.5f), 1080, 1909)

        assertTrue(rect.right > rect.left)
        assertTrue(rect.bottom > rect.top)
    }
}
