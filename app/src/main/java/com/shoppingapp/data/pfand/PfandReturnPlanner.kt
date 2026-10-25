package com.shoppingapp.data.pfand

import com.shoppingapp.data.model.PfandEntry
import java.util.BitSet
import kotlin.math.min

enum class PfandPlanError {
    NO_PENDING,
    INVALID_AMOUNT,
    EXCEEDS_PENDING,
    NO_EXACT_MATCH
}

/** Tek bir Pfand kaydından iade edilecek adet. */
data class PfandReturnLine(
    val entryId: Long,
    val label: String,
    val unitCents: Int,
    val quantity: Int
)

data class PfandReturnPlan(
    val lines: List<PfandReturnLine>,
    val amountCents: Int,
    val remainingCents: Int,
    val returnedUnits: Int,
    val remainingUnits: Int
)

sealed interface PfandPlanResult {
    data class Ready(val plan: PfandReturnPlan) : PfandPlanResult
    data class Error(val reason: PfandPlanError) : PfandPlanResult
}

/** DAO işleminin sonucu; [error] null ise para ve adetler yazılmıştır. */
data class PfandCashInResult(
    val amountCents: Int = 0,
    val returnedUnits: Int = 0,
    val remainingUnits: Int = 0,
    val error: PfandPlanError? = null
) {
    val success: Boolean get() = error == null
}

/**
 * Kasadan alınan toplam parayı, bekleyen Pfand kayıtlarından hangi adetlerin
 * iade edildiğine çevirir.
 *
 * İade tutarı mağazanın ödediği paradır; adetler bu tutardan türetilir. Farklı
 * birim Pfand'lar (0,08 / 0,15 / 0,25 / 1,50) karışıksa, en çok bekleyen türden
 * başlanır ve tutar tam eşleşene kadar tam sayı adetler denenir. Böylece
 * 95 × 0,25 € şişeden 7,50 € iade alındığında 30 şişe düşülür.
 *
 * Kuruş bazında çalışır; kayan nokta yuvarlaması bakiyeyi bozamaz.
 */
object PfandReturnPlanner {

    /** Tutar tam eşleşmezse hiçbir kaydı değiştirmez. */
    fun plan(entries: List<PfandEntry>, amountCents: Int): PfandPlanResult {
        val bundles = entries
            .filter { !it.isReturned && it.quantity > 0 && unitCents(it) > 0 }
            .groupBy { unitCents(it) }
            .map { (cents, group) ->
                Bundle(cents = cents, quantity = group.sumOf { it.quantity }, entries = group)
            }
            .sortedWith(
                // En sık kullanılan tür önce; eşitlikte küçük birim (şişe) önce.
                compareByDescending<Bundle> { it.quantity }.thenBy { it.cents }
            )

        if (bundles.isEmpty()) return PfandPlanResult.Error(PfandPlanError.NO_PENDING)
        if (amountCents <= 0) return PfandPlanResult.Error(PfandPlanError.INVALID_AMOUNT)

        val totalCents = bundles.sumOf { it.cents * it.quantity }
        if (amountCents > totalCents) return PfandPlanResult.Error(PfandPlanError.EXCEEDS_PENDING)

        // Tamamı isteniyorsa arama yapmadan bütün kayıtlar döner.
        if (amountCents == totalCents) {
            val lines = bundles.flatMap { bundle ->
                bundle.entries.sortedWith(entryOrder).mapNotNull { entry ->
                    if (entry.quantity <= 0) null else PfandReturnLine(
                        entryId = entry.id,
                        label = entry.label,
                        unitCents = bundle.cents,
                        quantity = entry.quantity
                    )
                }
            }
            return PfandPlanResult.Ready(
                PfandReturnPlan(
                    lines = lines,
                    amountCents = amountCents,
                    remainingCents = 0,
                    returnedUnits = lines.sumOf { it.quantity },
                    remainingUnits = 0
                )
            )
        }

        val divisor = bundles.map { it.cents }.reduce(::gcd)
        if (amountCents % divisor != 0) {
            return PfandPlanResult.Error(PfandPlanError.NO_EXACT_MATCH)
        }

        val scaled = bundles.map { it.copy(cents = it.cents / divisor) }
        val counts = solve(scaled, amountCents / divisor)
            ?: return PfandPlanResult.Error(PfandPlanError.NO_EXACT_MATCH)

        val lines = mutableListOf<PfandReturnLine>()
        bundles.forEachIndexed { index, bundle ->
            var left = counts[index]
            bundle.entries.sortedWith(entryOrder).forEach { entry ->
                if (left <= 0) return@forEach
                val take = min(left, entry.quantity)
                if (take > 0) {
                    lines += PfandReturnLine(
                        entryId = entry.id,
                        label = entry.label,
                        unitCents = bundle.cents,
                        quantity = take
                    )
                    left -= take
                }
            }
        }

        val returnedUnits = lines.sumOf { it.quantity }
        return PfandPlanResult.Ready(
            PfandReturnPlan(
                lines = lines,
                amountCents = amountCents,
                remainingCents = totalCents - amountCents,
                returnedUnits = returnedUnits,
                remainingUnits = bundles.sumOf { it.quantity } - returnedUnits
            )
        )
    }

    /** 0,25 € → 25 kuruş. Geçersiz/yuvarlanamayan tutarlar 0 döner. */
    fun unitCents(entry: PfandEntry): Int {
        val value = entry.unitAmount * 100.0
        if (!value.isFinite()) return 0
        return kotlin.math.round(value).toInt()
    }

    /**
     * Sınırlı bozuk para problemi: her türden en fazla bekleyen adet kadar seçerek
     * hedef kuruşu tam tutturan birleşimi bulur.
     *
     * Sondan başa "bu kalan tutar bu türlerle oluşabilir mi?" tablosu bir BitSet
     * olarak hesaplanır; ardından ilk türden başlanıp mümkün olan en yüksek adet
     * seçilir. Böylece 95 şişenin 30'u gibi doğal bir dağılım çıkar ve büyük
     * tutarlarda bile arama dallanıp UI'yı kilitlemez.
     */
    private fun solve(bundles: List<Bundle>, target: Int): IntArray? {
        val count = bundles.size
        val feasible = Array(count + 1) { BitSet(target + 1) }
        feasible[count].set(0)

        for (index in count - 1 downTo 0) {
            val value = bundles[index].cents
            val limit = bundles[index].quantity
            val previous = feasible[index + 1]
            val current = feasible[index]
            val window = (limit + 1L) * value

            for (residue in 0 until min(value, target + 1)) {
                var reachable = 0
                var amount = residue
                while (amount <= target) {
                    if (previous.get(amount)) reachable++
                    val expired = amount - window
                    if (expired >= 0 && previous.get(expired.toInt())) reachable--
                    if (reachable > 0) current.set(amount)
                    amount += value
                }
            }
        }

        if (!feasible[0].get(target)) return null

        val counts = IntArray(count)
        var remaining = target
        for (index in 0 until count) {
            val value = bundles[index].cents
            val maxCount = min(bundles[index].quantity, remaining / value)
            var chosen = -1
            for (candidate in maxCount downTo 0) {
                val next = remaining - candidate * value
                if (feasible[index + 1].get(next)) {
                    chosen = candidate
                    remaining = next
                    break
                }
            }
            if (chosen < 0) return null
            counts[index] = chosen
        }
        return counts
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    private data class Bundle(
        val cents: Int,
        val quantity: Int,
        val entries: List<PfandEntry>
    )

    private val entryOrder = compareBy<PfandEntry>({ it.createdAt }, { it.id })
}
