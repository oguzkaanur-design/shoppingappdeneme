package com.shoppingapp.feature.stats

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shoppingapp.data.model.PfandEntry
import com.shoppingapp.ui.theme.ShoppingAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * İade diyaloğunda tutar girildikçe adet hesabının canlı güncellendiğini ve
 * yalnızca geçerli bir tutarda iadeyi onaylamanın açıldığını doğrular.
 */
@RunWith(AndroidJUnit4::class)
class PfandReturnDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun amountShowsMatchingUnitsAndConfirmsCents() {
        var confirmedCents: Int? = null
        showDialog { confirmedCents = it }

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM).assertIsNotEnabled()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_AMOUNT).performTextInput("7.50")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CALC)
            .assertTextContains("30", substring = true)
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CALC)
            .assertTextContains("7,50", substring = true)
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM)
            .assertIsEnabled()
            .performClick()

        assertEquals(750, confirmedCents)
    }

    @Test
    fun amountBelowSmallestDepositIsRejected() {
        showDialog()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_AMOUNT).performTextInput("0.10")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_ERROR).assertExists()
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun amountThatCannotMatchWholeItemsIsRejected() {
        showDialog()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_AMOUNT).performTextInput("7.51")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_ERROR).assertExists()
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun amountAbovePendingTotalIsRejected() {
        showDialog()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_AMOUNT).performTextInput("30")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_ERROR).assertExists()
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CALC).assertDoesNotExist()
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun returnAllFillsPendingTotalInOneTap() {
        var confirmedCents: Int? = null
        showDialog { confirmedCents = it }

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_ALL)
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()

        // Ayırıcı cihaz yereline göre "25,00" veya "25.00" olabilir.
        val separator = PfandAmount.separatorFor(
            InstrumentationRegistry.getInstrumentation()
                .targetContext.resources.configuration.locales[0]
        )
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_AMOUNT)
            .assertTextContains("25${separator}00", substring = true)
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CALC)
            .assertTextContains("100", substring = true)
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM)
            .assertIsEnabled()
            .performClick()

        assertEquals(2_500, confirmedCents)
    }

    @Test
    fun returnAllIsDisabledWithoutPendingDeposit() {
        composeRule.setContent {
            ShoppingAppTheme {
                ReturnPfandDialog(
                    entries = emptyList(),
                    pendingTotal = 0.0,
                    pendingUnits = 0,
                    onDismiss = {},
                    onConfirm = {}
                )
            }
        }

        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_ALL).assertIsNotEnabled()
        composeRule.onNodeWithTag(PfandScreenTestTags.RETURN_CONFIRM).assertIsNotEnabled()
    }

    private fun showDialog(onConfirm: (Int) -> Unit = {}) {
        composeRule.setContent {
            ShoppingAppTheme {
                ReturnPfandDialog(
                    entries = listOf(
                        PfandEntry(
                            id = 1,
                            label = "Plastik şişe",
                            unitAmount = 0.25,
                            quantity = 100
                        )
                    ),
                    pendingTotal = 25.0,
                    pendingUnits = 100,
                    onDismiss = {},
                    onConfirm = onConfirm
                )
            }
        }
    }
}
