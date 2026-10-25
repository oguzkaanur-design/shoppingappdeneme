package com.shoppingapp.data.backup

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupValidatorTest {

    private fun validJson(extra: String = "") = """
        {"schemaVersion":1,"products":[{"id":1,"name":"Süt","barcode":"4001234567890","categoryId":1,"unit":"L","pfandAmount":0.0,"priceEuro":1.09}],"stores":[{"id":1,"name":"PENNY","colorHex":"#E2001A","aisleOrderJson":"[]"}]$extra}
    """.trimIndent()

    @Test
    fun `eksik tablolar bos diziye tamamlanir`() {
        val root = JsonParser.parseString(BackupValidator.normalize(validJson())).asJsonObject

        assertEquals(0, root.getAsJsonArray("listItems").size())
        assertEquals(0, root.getAsJsonArray("flyerListItems").size())
        assertEquals(1, root.getAsJsonArray("products").size())
    }

    @Test
    fun `urun ve market yoksa yedek reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize("""{"schemaVersion":1}""")
        }
        assertTrue(error.message!!.contains("ürün veya market"))
    }

    @Test
    fun `gelecek sema reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize("""{"schemaVersion":99,"products":[{}],"stores":[{}]}""")
        }
        assertTrue(error.message!!.contains("şema 99"))
    }

    @Test
    fun `liste olmayan alan reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize("""{"products":{},"stores":[{}]}""")
        }
        assertTrue(error.message!!.contains("liste değil"))
    }

    @Test
    fun `bozuk json reddedilir`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize("""{"products":[""")
        }
    }

    @Test
    fun `tablo basina kayit siniri asilirsa reddedilir`() {
        val items = (0..BackupValidator.MAX_ITEMS_PER_TABLE).joinToString(",") { "{}" }
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize("""{"schemaVersion":1,"products":[$items],"stores":[{}]}""")
        }
        assertTrue(error.message!!.contains("çok büyük"))
    }

    @Test
    fun `sinirli okuma fazla baytta durur`() {
        val input = ByteArrayInputStream(ByteArray(64))
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.readLimited(input, maxBytes = 32)
        }
    }

    @Test
    fun `sinirli okuma tam boyutu kabul eder`() {
        val input = ByteArrayInputStream(ByteArray(32))
        assertEquals(32, BackupValidator.readLimited(input, maxBytes = 32).size)
    }

    @Test
    fun `gecerli satirlar kabul edilir`() {
        val root = JsonParser.parseString(BackupValidator.normalize(validJson())).asJsonObject

        assertEquals(1, root.getAsJsonArray("products").size())
        assertEquals(1, root.getAsJsonArray("stores").size())
    }

    @Test
    fun `zorunlu alan eksikse yedek reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize(
                """{"schemaVersion":1,"products":[{"id":1,"barcode":"1","categoryId":1,"unit":"L","pfandAmount":0.0}],"stores":[{"id":1,"name":"PENNY","colorHex":"#E2001A","aisleOrderJson":"[]"}]}"""
            )
        }

        assertTrue(error.message!!.contains("'name' eksik"))
    }

    @Test
    fun `bos urun adi reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize(validJson().replace("\"Süt\"", "\"   \""))
        }

        assertTrue(error.message!!.contains("boş olamaz"))
    }

    @Test
    fun `yanlis alan tipi reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize(validJson().replace("\"priceEuro\":1.09", "\"priceEuro\":\"1,09\""))
        }

        assertTrue(error.message!!.contains("sayı olmalı"))
    }

    @Test
    fun `yinelenen kayit kimligi reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize(
                validJson().replace(
                    "\"stores\":[{\"id\":1,",
                    "\"stores\":[{\"id\":1,\"name\":\"PENNY\",\"colorHex\":\"#E2001A\",\"aisleOrderJson\":\"[]\"},{\"id\":1,"
                )
            )
        }

        assertTrue(error.message!!.contains("yinelenen kimlik"))
    }

    @Test
    fun `gecersiz para hesabi reddedilir`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupValidator.normalize(
                validJson(
                    ",\"moneyEntries\":[{\"id\":1,\"account\":\"WALLET\",\"amount\":10.0,\"note\":\"\",\"createdAt\":1}]"
                )
            )
        }

        assertTrue(error.message!!.contains("hesabı geçersiz"))
    }
}
