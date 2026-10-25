package com.shoppingapp.feature.reports

import com.shoppingapp.data.model.MoneyAccount
import com.shoppingapp.data.model.MoneyEntry
import com.shoppingapp.data.model.PantryRow
import com.shoppingapp.data.model.PfandEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRowsTest {

    private val labels = ReportLabels(
        cash = "Cash", bank = "Bank", date = "Date", description = "Description",
        account = "Account", amount = "Amount", type = "Type", units = "Units",
        unitPrice = "Unit", total = "Total", status = "Status", pending = "Pending",
        returned = "Returned", product = "Product", stock = "Stock",
        threshold = "Threshold", empty = "No records"
    )

    @Test
    fun `movements are newest first with signed amounts`() {
        val table = ReportRows.movements(
            listOf(
                MoneyEntry(account = MoneyAccount.CASH, amount = 50.0, note = "Pocket", createdAt = 1_000),
                MoneyEntry(account = MoneyAccount.BANK, amount = -12.5, note = "Bakery", createdAt = 2_000),
                MoneyEntry(account = MoneyAccount.CASH, amount = 0.0, note = "", createdAt = 3_000)
            ),
            labels
        )
        assertEquals(listOf("Date", "Description", "Account", "Amount"), table.headers)
        assertEquals(3, table.rows.size)
        assertEquals("—", table.rows[0][1])
        assertEquals("+€50,00", table.rows[2][3])
        assertEquals("−€12,50", table.rows[1][3])
        assertEquals("Bank", table.rows[1][2])
    }

    @Test
    fun `pfand lists pending and returned with totals`() {
        val table = ReportRows.pfand(
            pending = listOf(PfandEntry(label = "Can", unitAmount = 0.25, quantity = 4, createdAt = 2_000)),
            returned = listOf(PfandEntry(label = "Crate", unitAmount = 1.5, quantity = 2, createdAt = 1_000, isReturned = true)),
            labels = labels
        )
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Can", "4", "€0,25", "€1,00", "Pending"), table.rows[0])
        assertEquals(listOf("Crate", "2", "€1,50", "€3,00", "Returned"), table.rows[1])
    }

    @Test
    fun `empty tables still render a placeholder row`() {
        val movements = ReportRows.movements(emptyList(), labels)
        assertEquals("No records", movements.rows.single().first())
        val pantry = ReportRows.pantry(emptyList(), labels)
        assertEquals("No records", pantry.rows.single().first())
    }

    @Test
    fun `pantry shows quantity units and expiry`() {
        val table = ReportRows.pantry(
            listOf(
                PantryRow(
                    id = 1, productId = 1, name = "Milk", unit = "L", categoryId = 2,
                    currentQuantity = 1.5, minThreshold = 2.0, expiryDate = 1_700_000_000_000,
                    defaultStoreId = 1, priceEuro = 1.09
                )
            ),
            labels
        )
        assertEquals("Milk", table.rows[0][0])
        assertEquals("1,5 L", table.rows[0][1])
        assertEquals("2 L", table.rows[0][2])
        assertTrue(table.rows[0][3].matches(Regex("""\d{2}\.\d{2}\.\d{4}""")))
    }

    @Test
    fun `amounts use German formatting and whole numbers stay short`() {
        assertEquals("1", ReportRows.quantity(1.0))
        assertEquals("1,5", ReportRows.quantity(1.5))
        assertEquals("€1234,50", ReportRows.euro(1234.5))
    }
}
