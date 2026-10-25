package com.shoppingapp.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppingapp.MainActivity
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.model.ListItem
import com.shoppingapp.data.model.Product
import com.shoppingapp.data.repository.ShoppingRepository
import com.shoppingapp.di.BackgroundEntryPoint
import com.shoppingapp.security.AppLock
import com.shoppingapp.ui.theme.ThemeStore
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kısa yatay ekranda başlık kaydırılıp ekrandan çıkınca üst çubukta görünen
 * "Alışverişi tamamla" eylemini ve döndürme sonrası listenin başa dönmesini
 * gerçek MainActivity üzerinde doğrular.
 */
@RunWith(AndroidJUnit4::class)
class ShoppingCompactCompleteTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var dao: ShoppingDao
    private lateinit var repository: ShoppingRepository
    private val addedItemIds = mutableListOf<Long>()
    private var createdTestProducts = false

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        // Kilit ekranı testin ana ekrana ulaşmasını engellemesin.
        AppLock.clear(context)
        // Önceki bir denemeden kalan Vitrin/Kompakt seçimi testin varsaydığı
        // yerleşimi değiştirmesin.
        ThemeStore.init(context)
        ThemeStore.reset(context)

        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            BackgroundEntryPoint::class.java
        )
        dao = entryPoint.shoppingDao()
        repository = entryPoint.shoppingRepository()

        runBlocking(Dispatchers.IO) {
            repository.seedConfig()
            repository.initDefaultData()

            // Listede kaydırma yapacak kadar satır olduğundan emin ol.
            var products = dao.getAllProducts().first()
            if (products.isEmpty()) {
                createdTestProducts = true
                products = (1..3).map { index ->
                    Product(
                        name = "UI test product $index",
                        barcode = "UITEST_${System.currentTimeMillis()}_$index",
                        categoryId = 1,
                        unit = "Adet",
                        defaultStoreId = 1,
                        priceEuro = 0.99
                    ).let { product -> product.copy(id = dao.insertProduct(product)) }
                }
            }

            val now = System.currentTimeMillis()
            repeat(EXTRA_ITEMS) { index ->
                addedItemIds += dao.insertListItem(
                    ListItem(
                        productId = products[index % products.size].id,
                        storeId = 1,
                        quantity = 1.0,
                        isPurchased = false,
                        addedAt = now + index
                    )
                )
            }
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            runCatching {
                scenario.onActivity {
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            scenario.close()
        }

        if (::dao.isInitialized) {
            runBlocking(Dispatchers.IO) {
                dao.getAllListItemsOnce()
                    .filter { it.id in addedItemIds }
                    .forEach { dao.deleteListItem(it) }
                if (createdTestProducts) dao.deleteAllProducts()
            }
        }

        AppLock.clear(context)
    }

    @Test
    fun compactCompleteAppearsAfterHeaderScrollAndResetsOnRotation() {
        rotateTo(Configuration.ORIENTATION_LANDSCAPE)
        requireCompactLandscape()

        waitForTag(ShoppingScreenTestTags.LIST)
        waitForTag(ShoppingScreenTestTags.SUMMARY_COMPLETE)
        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPACT_COMPLETE).assertDoesNotExist()

        // Başlık bloğu listeden çıkana kadar kaydır.
        composeRule.onNodeWithTag(ShoppingScreenTestTags.LIST).performScrollToIndex(6)

        waitForTag(ShoppingScreenTestTags.COMPACT_COMPLETE)
        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPACT_COMPLETE)
            .assertIsDisplayed()
            .assertHasClickAction()

        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPACT_COMPLETE).performClick()
        waitForTag(ShoppingScreenTestTags.COMPLETE_DIALOG)
        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPLETE_DIALOG).assertIsDisplayed()

        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPLETE_DIALOG_DISMISS).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingScreenTestTags.COMPLETE_DIALOG)
                .fetchSemanticsNodes().isEmpty()
        }

        // Dikeyde başlık sabittir; kompakt eylem görünmez ve liste başa döner.
        rotateTo(Configuration.ORIENTATION_PORTRAIT)
        waitForTag(ShoppingScreenTestTags.SUMMARY_COMPLETE)
        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPACT_COMPLETE).assertDoesNotExist()

        // Yataya geri dönünce liste baştan başlar; eylem yine gizlidir.
        rotateTo(Configuration.ORIENTATION_LANDSCAPE)
        waitForTag(ShoppingScreenTestTags.SUMMARY_COMPLETE)
        composeRule.onNodeWithTag(ShoppingScreenTestTags.COMPACT_COMPLETE).assertDoesNotExist()
    }

    private fun rotateTo(orientation: Int) {
        scenario.onActivity { activity ->
            activity.requestedOrientation =
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
        }
        composeRule.waitUntil(15_000) {
            var current = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { current = it.resources.configuration.orientation }
            current == orientation
        }
        composeRule.waitForIdle()
    }

    private fun requireCompactLandscape() {
        var screenHeightDp = Int.MAX_VALUE
        scenario.onActivity { screenHeightDp = it.resources.configuration.screenHeightDp }
        assumeTrue(
            "Kompakt yatay düzen yalnızca 480 dp altı ekran yüksekliğinde geçerli.",
            screenHeightDp < 480
        )
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val EXTRA_ITEMS = 14
    }
}
