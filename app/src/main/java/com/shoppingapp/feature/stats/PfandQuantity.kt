package com.shoppingapp.feature.stats

internal object PfandQuantity {
    const val MAX = 9_999

    fun parse(raw: String): Int? = raw.toIntOrNull()?.takeIf { it in 1..MAX }
}
