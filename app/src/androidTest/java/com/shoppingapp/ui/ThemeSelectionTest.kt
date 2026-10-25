package com.shoppingapp.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppingapp.feature.theme.ThemeScreen
import com.shoppingapp.ui.theme.AppColorTheme
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.ShoppingAppTheme
import com.shoppingapp.ui.theme.ThemeStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Görünüm ekranındaki palet ve düzen seçimlerinin anında uygulandığını,
 * cihazda saklandığını ve varsayılana döndürülebildiğini doğrular.
 */
@RunWith(AndroidJUnit4::class)
class ThemeSelectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        ThemeStore.init(context)
        ThemeStore.reset(context)
    }

    @After
    fun tearDown() {
        ThemeStore.reset(context)
    }

    @Test
    fun selectionsApplyImmediatelyAndPersist() {
        composeRule.setContent {
            ShoppingAppTheme {
                ThemeScreen(onNavigateBack = {})
            }
        }

        composeRule.onNodeWithTag("theme_preview").assertExists()

        // Siyah, gece ve beyaz paletleri sırayla seç.
        composeRule.onNodeWithTag("theme_list").performScrollToIndex(5)
        composeRule.onNodeWithTag("theme_color_black").performClick()
        composeRule.runOnIdle { assertEquals(AppColorTheme.BLACK, ThemeStore.colorTheme) }

        composeRule.onNodeWithTag("theme_list").performScrollToIndex(6)
        composeRule.onNodeWithTag("theme_color_night").performClick()
        composeRule.runOnIdle { assertEquals(AppColorTheme.NIGHT, ThemeStore.colorTheme) }

        composeRule.onNodeWithTag("theme_list").performScrollToIndex(4)
        composeRule.onNodeWithTag("theme_color_white").performClick()
        composeRule.runOnIdle { assertEquals(AppColorTheme.WHITE, ThemeStore.colorTheme) }

        // Arayüz stilleri: vitrin seçimi kaydedilir.
        composeRule.onNodeWithTag("theme_list").performScrollToIndex(12)
        composeRule.onNodeWithTag("theme_style_gallery").performClick()
        composeRule.runOnIdle { assertEquals(AppUiStyle.GALLERY, ThemeStore.uiStyle) }

        composeRule.onNodeWithTag("theme_list").performScrollToIndex(10)
        composeRule.onNodeWithTag("theme_style_classic").performClick()
        composeRule.runOnIdle { assertEquals(AppUiStyle.CLASSIC, ThemeStore.uiStyle) }

        val prefs = context.getSharedPreferences("einkauf_theme", Context.MODE_PRIVATE)
        assertEquals("white", prefs.getString("color_theme", null))
        assertEquals("classic", prefs.getString("ui_style", null))

        // Varsayılana dön: sistem paleti + modern düzen.
        composeRule.onNodeWithTag("theme_list").performScrollToIndex(13)
        composeRule.onNodeWithTag("theme_reset").performClick()
        composeRule.runOnIdle {
            assertEquals(AppColorTheme.SYSTEM, ThemeStore.colorTheme)
            assertEquals(AppUiStyle.MODERN, ThemeStore.uiStyle)
        }
        assertEquals("system", prefs.getString("color_theme", null))
        assertEquals("modern", prefs.getString("ui_style", null))
    }
}
