package com.shoppingapp.account

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAccountTest {

    @Test
    fun emailValidationAcceptsRealisticAddresses() {
        assertTrue(LocalAccount.isValidEmail("ayse@example.com"))
        assertTrue(LocalAccount.isValidEmail("a.b+liste@sub.example.co.uk"))
    }

    @Test
    fun emailValidationRejectsBrokenAddresses() {
        assertFalse(LocalAccount.isValidEmail(""))
        assertFalse(LocalAccount.isValidEmail("ayse"))
        assertFalse(LocalAccount.isValidEmail("ayse@example"))
        assertFalse(LocalAccount.isValidEmail("ayse @example.com"))
        assertFalse(LocalAccount.isValidEmail("@example.com"))
    }

    @Test
    fun samePasswordAndSaltProduceTheSameHash() {
        val salt = ByteArray(16) { it.toByte() }
        val first = LocalAccount.derive("korrekt-pferd", salt, iterations = 1_000)
        val second = LocalAccount.derive("korrekt-pferd", salt, iterations = 1_000)
        assertArrayEquals(first, second)
        assertEquals(32, first.size)
    }

    @Test
    fun differentSaltOrPasswordProducesADifferentHash() {
        val salt = ByteArray(16) { 1 }
        val otherSalt = ByteArray(16) { 2 }
        val base = LocalAccount.derive("korrekt-pferd", salt, iterations = 1_000)
        val otherPassword = LocalAccount.derive("anderes-pferd", salt, iterations = 1_000)
        val otherSaltHash = LocalAccount.derive("korrekt-pferd", otherSalt, iterations = 1_000)

        assertFalse(base.contentEquals(otherPassword))
        assertFalse(base.contentEquals(otherSaltHash))
    }

    @Test
    fun productionIterationCountIsHighEnough() {
        assertTrue(LocalAccount.ITERATIONS >= 100_000)
        assertTrue(LocalAccount.MIN_PASSWORD_LENGTH >= 8)
    }
}
