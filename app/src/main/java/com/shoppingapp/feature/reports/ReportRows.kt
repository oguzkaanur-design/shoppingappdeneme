package com.shoppingapp.feature.reports

import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.model.PfandEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rapor tablosu; ekran ve PDF aynı satırları kullanır, böylece ikisi ayrışmaz. */
data class ReportTable(
    val headers: List<String>,
    val weights: List<Float>,
    val endAligned: Set<Int>,
    val rows: List<List<String>>
)

/** Başlıklar çağıran taraftan gelir (dile göre); biçimlendirme burada sabittir. */
data class ReportLabels(
    val cash: String,
    val bank: String,
    val date: String,
    val description: String,
    val account: String,
    val amount: String,
    val type: String,
    val units: String,
    val unitPrice: String,
    val total: String,
    val status: String,
    val pending: String,
    val returned: String,
    val product: String,
    val stock: String,
    val threshold: String,
    val empty: String
)

object ReportRows {

    fun euro(value: Double): String = String.format(Locale.GERMANY, "€%.2f", value)

    fun signedEuro(value: Double): String = when {
        value > 0.0 -> "+" + euro(value)
        value < 0.0 -> "−" + euro(-value)
        else -> euro(0.0)
    }

    fun date(epochMs: Long): String =
        SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(epochMs))

    fun movements(entries: List<MoneyEntry>, labels: ReportLabels): ReportTable {
        val rows = entries
            .sortedByDescending { it.createdAt }
            .map { entry ->
                listOf(
                    date(entry.createdAt),
                    entry.note.ifBlank { "—" },
                    if (entry.account == MoneyAccount.BANK) labels.bank else labels.cash,
                    signedEuro(entry.amount)
                )
            }
            .ifEmpty { listOf(listOf(labels.empty, "", "", "")) }
        return ReportTable(
            headers = listOf(labels.date, labels.description, labels.account, labels.amount),
            weights = listOf(0.22f, 0.42f, 0.16f, 0.20f),
            endAligned = setOf(3),
            rows = rows
        )
    }

    fun pfand(
        pending: List<PfandEntry>,
        returned: List<PfandEntry>,
        labels: ReportLabels
    ): ReportTable {
        val rows = (pending.map { it to labels.pending } + returned.map { it to labels.returned })
            .sortedByDescending { it.first.createdAt }
            .map { (entry, status) ->
                listOf(
                    entry.label,
                    entry.quantity.toString(),
                    euro(entry.unitAmount),
                    euro(entry.unitAmount * entry.quantity),
                    status
                )
            }
            .ifEmpty { listOf(listOf(labels.empty, "", "", "", "")) }
        return ReportTable(
            headers = listOf(labels.type, labels.units, labels.unitPrice, labels.total, labels.status),
            weights = listOf(0.34f, 0.12f, 0.18f, 0.20f, 0.16f),
            endAligned = setOf(1, 2, 3),
            rows = rows
        )
    }

    fun pantry(rows: List<PantryRow>, labels: ReportLabels): ReportTable {
        val data = rows
            .sortedWith(compareBy({ it.currentQuantity > it.minThreshold }, { it.name }))
            .map { row ->
                listOf(
                    row.name,
                    "${quantity(row.currentQuantity)} ${row.unit}".trim(),
                    "${quantity(row.minThreshold)} ${row.unit}".trim(),
                    row.expiryDate?.let { date(it) } ?: "—"
                )
            }
            .ifEmpty { listOf(listOf(labels.empty, "", "", "")) }
        return ReportTable(
            headers = listOf(labels.product, labels.stock, labels.threshold, labels.date),
            weights = listOf(0.44f, 0.20f, 0.20f, 0.16f),
            endAligned = setOf(1, 2),
            rows = data
        )
    }

    /** "1" / "1,5": tam sayılar gereksiz ondalık göstermesin. */
    fun quantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.GERMANY, "%.1f", value)
}
