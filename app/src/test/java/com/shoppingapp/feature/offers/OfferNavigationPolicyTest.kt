package com.shoppingapp.feature.offers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferNavigationPolicyTest {
    @Test
    fun allowsSecureRetailerPagesAndTheirSubpages() {
        assertTrue(OfferNavigationPolicy.allowsInAppNavigation("https://www.rewe.de/angebote/"))
        assertTrue(OfferNavigationPolicy.allowsInAppNavigation("https://prospekt.lidl.de/view/123"))
    }

    @Test
    fun blocksInsecureOrAppLaunchingSchemes() {
        assertFalse(OfferNavigationPolicy.allowsInAppNavigation("http://www.rewe.de/angebote/"))
        assertFalse(OfferNavigationPolicy.allowsInAppNavigation("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(OfferNavigationPolicy.allowsInAppNavigation("javascript:alert(1)"))
        assertFalse(OfferNavigationPolicy.allowsInAppNavigation("not a url"))
    }
}
