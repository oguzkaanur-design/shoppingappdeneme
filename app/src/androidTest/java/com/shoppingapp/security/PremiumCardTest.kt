package com.shoppingapp.security

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shoppingapp.ui.theme.ShoppingAppTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Premium kartı: durum doğru gösteriliyor mu ve geçersiz anahtar premium açmıyor mu?
 */
@RunWith(AndroidJUnit4::class)
class PremiumCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cardShowsStateAndRejectsForgedToken() {
        PremiumGate.deactivate(context)
        composeRule.setContent {
            ShoppingAppTheme { PremiumCard() }
        }

        composeRule.onNodeWithTag("premium_card").assertExists()
        composeRule.onNodeWithTag("premium_activate").performClick()
        composeRule.onNodeWithTag("premium_token").performTextInput("BASKIT1.e30.e30")
        composeRule.onNodeWithTag("premium_confirm").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("premium_error").assertExists()
        assertFalse(PremiumGate.isPremium(context))
    }
}
