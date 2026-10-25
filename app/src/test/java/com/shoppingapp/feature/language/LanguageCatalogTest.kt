package com.shoppingapp.feature.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCatalogTest {

    @Test
    fun `search matches native and English names`() {
        assertEquals(listOf("de"), LanguageCatalog.filter("deutsch").map { it.tag })
        assertEquals(listOf("de"), LanguageCatalog.filter("german").map { it.tag })
        assertEquals(listOf("zh-CN"), LanguageCatalog.filter("简体").map { it.tag })
    }

    @Test
    fun `search is case insensitive and empty query returns all languages`() {
        assertEquals(listOf("nl"), LanguageCatalog.filter("DUTCH").map { it.tag })
        assertTrue(LanguageCatalog.filter("   ").size == LanguageCatalog.supported.size)
    }

    @Test
    fun `unknown query returns no languages`() {
        assertTrue(LanguageCatalog.filter("klingon").isEmpty())
    }

    @Test
    fun `tag lookup is case insensitive and falls back to language code`() {
        assertEquals("en-GB", LanguageCatalog.byTag("en-gb")?.tag)
        assertEquals("zh-CN", LanguageCatalog.byTag("zh")?.tag)
        assertNull(LanguageCatalog.byTag(null))
        assertNull(LanguageCatalog.byTag("xx-XX"))
    }
}
