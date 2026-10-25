package com.shoppingapp.feature.money

import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.money.MoneyFormat
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

    fun parse(raw: String): Double? = MoneyFormat.parse(raw)

    fun roundToCents(value: Double): Double = round(value * 100) / 100
}

/** Tutarı seçili para biriminde gösterir; biçim tek yerden yönetilir. */
internal fun money(value: Double): String = MoneyFormat.amount(value)

internal fun signedMoney(value: Double): String = when {
    value > 0.0 -> "+" + money(value)
    value < 0.0 -> "−" + money(abs(value))
    else -> money(0.0)
}
