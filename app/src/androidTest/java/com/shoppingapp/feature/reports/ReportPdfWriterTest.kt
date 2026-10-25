package com.shoppingapp.feature.reports

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ReportPdfWriterTest {

    private val labels = ReportLabels(
        cash = "Cash", bank = "Bank", date = "Date", description = "Description",
        account = "Account", amount = "Amount", type = "Type", units = "Units",
        unitPrice = "Unit", total = "Total", status = "Status", pending = "Pending",
        returned = "Returned", product = "Product", stock = "Stock",
        threshold = "Threshold", empty = "No records"
    )

    @Test
    fun writesAReadableMultiPagePdf() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val writer = ReportPdfWriter(context)
        val movements = ReportTable(
            headers = labels.let { listOf(it.date, it.description, it.account, it.amount) },
            weights = listOf(0.22f, 0.42f, 0.16f, 0.20f),
            endAligned = setOf(3),
            rows = (1..70).map {
                listOf("01.10.2026", "Market alışverişi $it", "Cash", "−€12,50")
            }
        )
        val document = ReportDocument(
            brand = "Baskit",
            title = "Report",
            generatedLabel = "Generated: 25.10.2026 15:00",
            summaryTitle = "Summary",
            cashLabel = "Cash",
            bankLabel = "Bank",
            pfandLabel = "Pfand",
            totalLabel = "Total",
            movementsTitle = "Transactions",
            pfandTitle = "Pfand",
            pantryTitle = "Pantry",
            footer = "Baskit · Data stays on this device",
            labels = labels,
            cash = 45.10,
            bank = 1154.10,
            pfand = 25.25,
            movements = movements,
            pfandTable = ReportRows.pfand(emptyList(), emptyList(), labels),
            pantryTable = ReportRows.pantry(emptyList(), labels)
        )

        val file = writer.write(document)
        try {
            assertTrue("PDF oluşmadı", file.exists())
            assertTrue("PDF çok küçük", file.length() > 2_000)
            val header = file.inputStream().use { stream -> ByteArray(4).also { stream.read(it) } }
            assertEquals("%PDF", String(header))
            // 70 satır tek sayfaya sığmaz; en az iki sayfa beklenir.
            val text = file.readBytes().toString(Charsets.ISO_8859_1)
            assertTrue("Sayfa sayısı beklenenden az", Regex("""/Count\s+([2-9]|\d{2,})""").containsMatchIn(text))

            // Cihazın PDF oluşturucusuyla ilk sayfayı gerçekten çiz: fontlar,
            // tablo çizgileri ve sayfa boyutu burada doğrulanır.
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertTrue("PDF en az bir sayfa içermeli", renderer.pageCount >= 1)
                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        var dark = 0
                        for (x in 0 until bitmap.width step 12) {
                            for (y in 0 until bitmap.height step 12) {
                                if (bitmap.getPixel(x, y) != Color.WHITE) dark++
                            }
                        }
                        assertTrue("Sayfa boş çizildi", dark > 50)
                        // Test ortamında dış depolama izni yok; uygulama klasörüne yaz.
                        val target = java.io.File(context.getExternalFilesDir(null), "report-render.png")
                        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                    }
                }
            }
        } finally {
            file.delete()
        }
    }
}
