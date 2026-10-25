package com.shoppingapp.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shoppingapp.MainActivity
import com.shoppingapp.money.Currency
import com.shoppingapp.money.CurrencyStore
import com.shoppingapp.money.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * İlk açılış akışı: dil → ülke/para birimi → hesap → tanıtım turu.
 *
 * Test runner onboarding'i normalde tamamlanmış işaretlediği için burada
 * bilinçli olarak sıfırlanır; ekranın gerçekten açıldığı ve seçilen ülkenin
 * para birimini uyguladığı doğrulanır.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun startFromFirstRun() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            OnboardingStore.reset(context)
            CurrencyStore.set(context, Currency.EUR)
        }
    }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun selectingTurkiyeSetsLiraAndFinishesOnboarding() {
        // 1. adım: dil — varsayılan seçimle devam.
        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).performClick()

        // 2. adım: ülke — Türkiye'yi ara ve seç.
        compose.onNodeWithTag(OnboardingTestTags.COUNTRY_SEARCH).performTextInput("Türkiye")
        compose.onNodeWithTag(OnboardingTestTags.country("TR")).performClick()
        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).performClick()

        // 3. adım: hesap — Google yapılandırılmadığı için hesapsız devam.
        compose.onNodeWithTag(OnboardingTestTags.SKIP_ACCOUNT).performClick()

        // 4. adım: tanıtım turu — atla.
        compose.onNodeWithTag(OnboardingTestTags.TOUR_SKIP).performClick()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("Onboarding tamamlanmalı", OnboardingStore.isCompleted(context))
        assertEquals(Currency.TRY, CurrencyStore.current(context))
        assertEquals("TR", OnboardingStore.country(context)?.code)
        assertEquals("34,90 ₺", MoneyFormat.amount(34.9))
    }
}
