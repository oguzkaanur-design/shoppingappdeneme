package com.shoppingapp.data.pfand

import com.shoppingapp.data.model.PfandEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PfandReturnPlannerTest {

    @Test
    fun partialAmountReturnsOnlyMatchingBottleCount() {
        val bottles = entry(id = 1, quantity = 95, unitAmount = 0.25)

        val plan = ready(PfandReturnPlanner.plan(listOf(bottles), 750))

        assertEquals(750, plan.amountCents)
        assertEquals(30, plan.returnedUnits)
        assertEquals(65, plan.remainingUnits)
        assertEquals(1_625, plan.remainingCents)
        assertEquals(1, plan.lines.size)
        assertEquals(30, plan.lines.single().quantity)
        assertEquals(1, plan.lines.single().entryId)
    }

    @Test
    fun mixedDepositsUseWholeItemsAndLeaveTheRest() {
        val bottles = entry(id = 1, quantity = 95, unitAmount = 0.25)
        val crates = entry(id = 2, quantity = 10, unitAmount = 0.15)

        val plan = ready(PfandReturnPlanner.plan(listOf(bottles, crates), 765))

        assertEquals(31, plan.returnedUnits)
        assertEquals(765, plan.amountCents)
        assertEquals(74, plan.remainingUnits)
        assertEquals(1_760, plan.remainingCents)
        assertEquals(30, plan.lines.first { it.entryId == 1L }.quantity)
        assertEquals(1, plan.lines.first { it.entryId == 2L }.quantity)
    }

    @Test
    fun fullAmountReturnsEveryEntry() {
        val bottles = entry(id = 1, quantity = 3, unitAmount = 0.25)
        val crates = entry(id = 2, quantity = 2, unitAmount = 1.50)

        val plan = ready(PfandReturnPlanner.plan(listOf(bottles, crates), 375))

        assertEquals(5, plan.returnedUnits)
        assertEquals(0, plan.remainingUnits)
        assertEquals(0, plan.remainingCents)
        assertEquals(3, plan.lines.first { it.entryId == 1L }.quantity)
        assertEquals(2, plan.lines.first { it.entryId == 2L }.quantity)
    }

    @Test
    fun amountAbovePendingIsRejected() {
        val entry = entry(id = 1, quantity = 2, unitAmount = 0.25)

        val result = PfandReturnPlanner.plan(listOf(entry), 51)

        assertEquals(PfandPlanError.EXCEEDS_PENDING, error(result))
    }

    @Test
    fun amountThatCannotMatchWholeItemsIsRejected() {
        val bottles = entry(id = 1, quantity = 2, unitAmount = 0.25)
        val crate = entry(id = 2, quantity = 1, unitAmount = 0.15)

        val result = PfandReturnPlanner.plan(listOf(bottles, crate), 30)

        assertEquals(PfandPlanError.NO_EXACT_MATCH, error(result))
    }

    @Test
    fun nonPositiveAmountIsInvalid() {
        val entry = entry(id = 1, quantity = 2, unitAmount = 0.25)

        val result = PfandReturnPlanner.plan(listOf(entry), 0)

        assertEquals(PfandPlanError.INVALID_AMOUNT, error(result))
    }

    @Test
    fun noPendingEntriesReturnsNoPending() {
        val result = PfandReturnPlanner.plan(emptyList(), 50)

        assertEquals(PfandPlanError.NO_PENDING, error(result))
    }

    @Test
    fun returnsFlaggedEntriesAreNotCounted() {
        val returned = entry(id = 1, quantity = 10, unitAmount = 0.25, isReturned = true)

        val result = PfandReturnPlanner.plan(listOf(returned), 50)

        assertEquals(PfandPlanError.NO_PENDING, error(result))
    }

    @Test
    fun largePendingAmountsAreSolvedWithoutBranchingExplosion() {
        val small = entry(id = 1, quantity = 9_999, unitAmount = 0.08)
        val large = entry(id = 2, quantity = 9_999, unitAmount = 0.25)

        val plan = ready(PfandReturnPlanner.plan(listOf(small, large), 300_000))

        assertEquals(300_000, plan.amountCents)
        assertEquals(18_783, plan.returnedUnits)
        assertEquals(9_975, plan.lines.first { it.entryId == 1L }.quantity)
        assertEquals(8_808, plan.lines.first { it.entryId == 2L }.quantity)
    }

    @Test
    fun zeroUnitDepositIsIgnored() {
        val free = entry(id = 1, quantity = 10, unitAmount = 0.0)
        val bottle = entry(id = 2, quantity = 1, unitAmount = 0.25)

        val plan = ready(PfandReturnPlanner.plan(listOf(free, bottle), 25))

        assertEquals(1, plan.returnedUnits)
        assertTrue(plan.lines.none { it.entryId == free.id })
    }

    private fun entry(
        id: Long,
        quantity: Int,
        unitAmount: Double,
        isReturned: Boolean = false
    ) = PfandEntry(
        id = id,
        label = "Plastik şişe",
        unitAmount = unitAmount,
        quantity = quantity,
        createdAt = id,
        isReturned = isReturned
    )

    private fun ready(result: PfandPlanResult): PfandReturnPlan =
        (result as PfandPlanResult.Ready).plan

    private fun error(result: PfandPlanResult): PfandPlanError =
        (result as PfandPlanResult.Error).reason
}
