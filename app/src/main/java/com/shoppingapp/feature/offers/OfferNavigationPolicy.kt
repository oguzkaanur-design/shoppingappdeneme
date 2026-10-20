package com.shoppingapp.feature.offers

import java.net.URI

internal object OfferNavigationPolicy {
    fun allowsInAppNavigation(rawUrl: String): Boolean {
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }
}
