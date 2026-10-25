package com.shoppingapp.feature.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportDocument(
    val brand: String,
    val title: String,
    val generatedLabel: String,
    val summaryTitle: String,
    val cashLabel: String,
    val bankLabel: String,
    val pfandLabel: String,
    val totalLabel: String,
    val movementsTitle: String,
    val pfandTitle: String,
    val pantryTitle: String,
    val footer: String,
    val labels: ReportLabels,
    val cash: Double,
    val bank: Double,
    val pfand: Double,
    val movements: ReportTable,
    val pfandTable: ReportTable,
    val pantryTable: ReportTable
)

/**
 * Cebim, Pfand ve kiler özetini tek bir A4 PDF'e yazar. Harici kütüphane yok;
 * Android'in PdfDocument'ı kullanılır, böylece APK büyümez ve rapor çevrimdışı üretilir.
 */
class ReportPdfWriter(private val context: Context) {

    private val ink = Color.rgb(15, 23, 42)
    private val muted = Color.rgb(71, 85, 105)
    private val green = Color.rgb(4, 120, 87)
    private val mint = Color.rgb(236, 253, 245)
    private val border = Color.rgb(209, 250, 229)
    private val danger = Color.rgb(185, 28, 28)

    private val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacing = 0.12f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 8.5f
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 9f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 8f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 8f
    }
    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 7.5f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = border
        strokeWidth = 0.7f
    }
    private val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green }
    private val zebraFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mint }

    fun write(document: ReportDocument): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY).format(Date())
        val file = File(dir, "baskit-rapor-$stamp.pdf")

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var y = 54f

        fun nextPage(): Canvas {
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(pageInfo(pageNumber))
            canvas = page.canvas
            y = 46f
            canvas.drawText(document.brand.uppercase(Locale.getDefault()), MARGIN, 30f, brandPaint)
            return canvas
        }

        fun ensure(space: Float) {
            if (y + space > PAGE_HEIGHT - 52f) nextPage()
        }

        fun section(title: String) {
            ensure(90f)
            canvas.drawText(title, MARGIN, y, sectionPaint)
            y += 8f
        }

        fun drawTable(table: ReportTable) {
            val width = PAGE_WIDTH - MARGIN * 2 - 10f
            fun header() {
                ensure(26f)
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, headerFill)
                var x = MARGIN + 5f
                table.headers.forEachIndexed { index, label ->
                    val column = width * table.weights[index]
                    canvas.drawText(fit(label, headerPaint, column - 8f), x, y + 12f, headerPaint)
                    x += column
                }
                y += 18f
            }

            header()
            table.rows.forEachIndexed { rowIndex, row ->
                if (y + 17f > PAGE_HEIGHT - 52f) {
                    nextPage()
                    header()
                }
                if (rowIndex % 2 == 1) {
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 17f, zebraFill)
                }
                var x = MARGIN + 5f
                row.forEachIndexed { column, value ->
                    val colWidth = width * table.weights[column]
                    cellPaint.color = if (column in table.endAligned && value.startsWith("−")) danger else ink
                    val text = fit(value, cellPaint, colWidth - 8f)
                    val textX = if (column in table.endAligned) {
                        x + colWidth - 8f - cellPaint.measureText(text)
                    } else {
                        x
                    }
                    canvas.drawText(text, textX, y + 11.5f, cellPaint)
                    x += colWidth
                }
                canvas.drawLine(MARGIN, y + 17f, PAGE_WIDTH - MARGIN, y + 17f, linePaint)
                y += 17f
            }
        }

        canvas.drawText(document.brand.uppercase(Locale.getDefault()), MARGIN, 34f, brandPaint)
        canvas.drawText(document.title, MARGIN, y + 2f, titlePaint)
        y += 20f
        canvas.drawText(document.generatedLabel, MARGIN, y, metaPaint)
        y += 16f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 18f

        canvas.drawText(document.summaryTitle, MARGIN, y, sectionPaint)
        y += 12f
        fun summaryRow(label: String, value: String, strong: Boolean = false) {
            canvas.drawText(label, MARGIN, y + 11f, if (strong) valuePaint else labelPaint)
            canvas.drawText(value, PAGE_WIDTH - MARGIN - valuePaint.measureText(value), y + 11f, valuePaint)
            y += 19f
        }
        summaryRow(document.cashLabel, ReportRows.euro(document.cash))
        summaryRow(document.bankLabel, ReportRows.euro(document.bank))
        summaryRow(document.pfandLabel, ReportRows.euro(document.pfand))
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        summaryRow(document.totalLabel, ReportRows.euro(document.cash + document.bank + document.pfand), strong = true)
        y += 10f

        section(document.movementsTitle)
        drawTable(document.movements)
        y += 14f

        section(document.pfandTitle)
        drawTable(document.pfandTable)
        y += 14f

        section(document.pantryTitle)
        drawTable(document.pantryTable)

        canvas.drawText(document.footer, MARGIN, PAGE_HEIGHT - 26f, footerPaint)
        canvas.drawText("$pageNumber", PAGE_WIDTH - MARGIN - 10f, PAGE_HEIGHT - 26f, footerPaint)

        pdf.finishPage(page)
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    private fun pageInfo(number: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var cut = text.length
        while (cut > 1 && paint.measureText(text.substring(0, cut) + "…") > maxWidth) cut--
        return text.substring(0, cut) + "…"
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
    }
}
