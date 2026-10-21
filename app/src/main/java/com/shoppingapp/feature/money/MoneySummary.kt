package com.shoppingapp.feature.money

import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** Nakit, banka ve Pfand bakiyeleri; ekranın tek doğruluk kaynağı. */
data class MoneyBalances(
    val cash: Double = 0.0,
    val bank: Double = 0.0,
    val pfand: Double = 0.0
) {
    /**
     * Cebimdeki toplam. Pfand henüz bozdurulmadığı için "bekleyen para" olarak
     * eklenir; böylece kasada duran şişeler de birikimin parçası görünür.
     */
    val total: Double get() = cash + bank + pfand

    fun balanceOf(account: String): Double = when (account) {
        MoneyAccount.BANK -> bank
        else -> cash
    }
}

/**
 * Bakiye ayrı tutulmaz: her hesabın bakiyesi o hesabın hareketlerinin toplamıdır.
 * Saf fonksiyonlar olduğu için cihaz olmadan test edilebilir.
 */
object MoneySummary {

    fun of(entries: List<MoneyEntry>, pendingPfand: Double): MoneyBalances {
        var cash = 0.0
        var bank = 0.0
        entries.forEach { entry ->
            when (entry.account) {
                MoneyAccount.CASH -> cash += entry.amount
                MoneyAccount.BANK -> bank += entry.amount
            }
        }
        return MoneyBalances(
            cash = cash,
            bank = bank,
            pfand = if (pendingPfand.isFinite() && pendingPfand > 0.0) pendingPfand else 0.0
        )
    }

    /** Diyaloğdaki "işlemden sonra bakiye" önizlemesi. */
    fun preview(current: Double, delta: Double): Double = current + delta
}

/** Kullanıcının yazdığı tutarı okur: "12,50", "12.50", " 7 € " hepsi geçerli. */
internal object MoneyInput {

    fun parse(raw: String): Double? {
        val cleaned = raw.trim()
            .replace("€", "")
            .replace(" ", "")
            .replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value <= 0.0) return null
        return roundToCents(value)
    }

    fun roundToCents(value: Double): Double = round(value * 100) / 100
}

internal fun euro(value: Double): String = String.format(Locale.GERMANY, "€%.2f", value)

internal fun signedEuro(value: Double): String = when {
    value > 0.0 -> "+" + euro(value)
    value < 0.0 -> "−" + euro(abs(value))
    else -> euro(0.0)
}
