package com.shoppingapp.data.offers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarktguruConfigParserTest {

    private fun page(json: String) =
        """<html><head><script type="application/json">$json</script></head><body></body></html>"""

    @Test
    fun readsApiHostAndClientKeysFromEmbeddedConfig() {
        val html = page(
            """{"config":{"apiHostAddress":"https://api.marktguru.de",
               "apiKey":"api-key-value","clientKey":"client-key-value"},"data":{}}"""
        )

        val config = MarktguruConfigParser.parse(html)

        assertEquals("api.marktguru.de", config?.apiHost)
        assertEquals("api-key-value", config?.apiKey)
        assertEquals("client-key-value", config?.clientKey)
    }

    @Test
    fun findsConfigNestedDeeperInTheDocument() {
        val html = page("""{"content":{"layout":{"config":{"apiHostAddress":"api.example.de","apiKey":"a","clientKey":"b"}}}}""")

        assertEquals("api.example.de", MarktguruConfigParser.parse(html)?.apiHost)
    }

    @Test
    fun ignoresBlocksWithoutKeys() {
        assertNull(MarktguruConfigParser.parse(page("""{"config":{"apiHostAddress":"api.marktguru.de"}}""")))
        assertNull(MarktguruConfigParser.parse("<html></html>"))
    }
}
