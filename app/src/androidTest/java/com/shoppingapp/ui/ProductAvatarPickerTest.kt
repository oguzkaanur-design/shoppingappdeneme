package com.shoppingapp.ui

import android.content.Context
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ürünün simgesi gerçekten türüne göre mi seçiliyor, simgeye dokununca başka bir
 * simge seçilebiliyor mu ve seçim veritabanına yazılıyor mu? Bunu liste ekranında
 * gerçek MainActivity üzerinden doğrular.
 */
@RunWith(AndroidJUnit4::class)
class ProductAvatarPickerTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var dao: ShoppingDao
    private lateinit var repository: ShoppingRepository
    private var productId = 0L
    private var listItemId = 0L

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val thumbTag: String get() = "product_thumb_$productId"

    @Before
    fun setUp() {
        AppLock.clear(context)
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

            // Domates adı "meyve & sebze" olarak tanınmalı; test bunu doğrular.
            val product = Product(
                name = "TEST Domates ${System.currentTimeMillis()}",
                barcode = "AVATAR_${System.currentTimeMillis()}",
                categoryId = 1L,
                unit = "Adet",
                defaultStoreId = 1L
            )
            productId = dao.insertProduct(product)
            listItemId = dao.insertListItem(
                ListItem(
                    productId = productId,
                    storeId = 1L,
                    quantity = 1.0,
                    isPurchased = false,
                    addedAt = System.currentTimeMillis()
                )
            )
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()

        if (::dao.isInitialized && listItemId > 0L) {
            runBlocking(Dispatchers.IO) {
                dao.getAllListItemsOnce()
                    .filter { it.id == listItemId }
                    .forEach { dao.deleteListItem(it) }
            }
        }

        AppLock.clear(context)
    }

    @Test
    fun tappingTheThumbChangesTheProductsAvatar() {
        scrollToThumb()
        waitForThumb("🍅")

        // 1) Simgeye dokun → seçim açılır.
        composeRule.onNodeWithTag(thumbTag, useUnmergedTree = true).performClick()
        waitForTag("product_avatar_picker")

        // 2) Peynir seç → listedeki simge ve veritabanı değişir.
        // Izgara tembel; Süt Ürünleri bölümüne kaydırıp peyniri görünür yap.
        composeRule.onNodeWithTag("product_avatar_grid", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("product_avatar_cheese"))
        composeRule.onNodeWithTag("product_avatar_cheese", useUnmergedTree = true).performClick()
        waitUntilGone("product_avatar_picker")
        waitForAvatarKey("cheese")
        assertEquals("Seçim veritabanına yazılmalı", "cheese", avatarKey())
        waitForThumb("🧀")

        // 3) "Otomatik" seç → simge yine addan tahmin edilir.
        composeRule.onNodeWithTag(thumbTag, useUnmergedTree = true).performClick()
        waitForTag("product_avatar_picker")
        composeRule.onNodeWithTag("product_avatar_auto", useUnmergedTree = true).performClick()
        waitUntilGone("product_avatar_picker")
        waitForAvatarKey(null)
        assertEquals("Otomatik seçim null olarak saklanmalı", null, avatarKey())
        waitForThumb("🍅")
    }

    /** Seçim arka planda yazıldığı için veritabanı değişene kadar bekle. */
    private fun waitForAvatarKey(expected: String?) {
        composeRule.waitUntil(10_000) { avatarKey() == expected }
        composeRule.waitForIdle()
    }

    private fun avatarKey(): String? = runBlocking(Dispatchers.IO) {
        dao.getProductById(productId)?.avatarKey
    }

    /** Liste uzunsa ürün satırı görünene kadar kaydır (lazy liste yoksa sessizce geç). */
    private fun scrollToThumb() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(ShoppingScreenTestTags.LIST))
                .fetchSemanticsNodes().isNotEmpty()
        }
        runCatching {
            composeRule.onNodeWithTag(ShoppingScreenTestTags.LIST)
                .performScrollToNode(hasTestTag(thumbTag))
        }
        composeRule.waitForIdle()
    }

    private fun waitForThumb(emoji: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(
                hasTestTag(thumbTag) and hasAnyDescendant(hasText(emoji)),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun waitUntilGone(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()
    }
}
