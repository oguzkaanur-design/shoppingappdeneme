package com.shoppingapp.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    @After
    fun clearLock() {
        AppLock.clear(context)
    }

    @Test
    fun setPinEnablesLockAndNeverStoresPlainPin() {
        AppLock.setPin(context, "1234")

        assertTrue(AppLock.isEnabled(context))
        assertTrue(AppLock.hasPin(context))

        val prefs = context.getSharedPreferences("einkauf_app_lock", Context.MODE_PRIVATE)
        assertFalse(prefs.all.values.any { it.toString().contains("1234") })
    }

    @Test
    fun verifyAcceptsCorrectPinAndRejectsWrongPin() {
        AppLock.setPin(context, "1234")

        assertTrue(AppLock.verify(context, "1234").success)
        assertFalse(AppLock.verify(context, "0000").success)
    }

    @Test
    fun fiveWrongAttemptsLockVerification() {
        AppLock.setPin(context, "1234")

        repeat(4) {
            assertFalse(AppLock.verify(context, "0000").success)
        }
        val locked = AppLock.verify(context, "0000")

        assertFalse(locked.success)
        assertTrue(locked.lockoutSeconds > 0)
        assertFalse(AppLock.verify(context, "1234").success)
    }

    @Test
    fun changingPinInvalidatesOldPin() {
        AppLock.setPin(context, "1234")
        AppLock.setPin(context, "5678")

        assertFalse(AppLock.verify(context, "1234").success)
        assertTrue(AppLock.verify(context, "5678").success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setPinRejectsInvalidLength() {
        AppLock.setPin(context, "12")
    }

    @Test(expected = IllegalArgumentException::class)
    fun setPinRejectsNonDigits() {
        AppLock.setPin(context, "12ab")
    }

    @Test
    fun clearRemovesLock() {
        AppLock.setPin(context, "1234")
        AppLock.clear(context)

        assertFalse(AppLock.isEnabled(context))
        assertEquals(0, AppLock.verify(context, "1234").lockoutSeconds)
        assertFalse(AppLock.verify(context, "1234").success)
    }

    @Test
    fun lockoutCountdownExpiresAndCorrectPinWorksAgain() {
        AppLock.setPin(context, "1234")
        val prefs = context.getSharedPreferences("einkauf_app_lock", Context.MODE_PRIVATE)

        prefs.edit()
            .putLong("locked_until", android.os.SystemClock.elapsedRealtime() + 5_000L)
            .commit()

        val locked = AppLock.verify(context, "1234")
        assertFalse("Kilit süresi dolmadan doğru PIN kabul edilmemeli", locked.success)
        assertTrue("Kalan süre pozitif olmalı", locked.lockoutSeconds in 1..5)

        prefs.edit()
            .putLong("locked_until", android.os.SystemClock.elapsedRealtime() - 1L)
            .commit()

        assertTrue("Süre dolunca doğru PIN çalışmalı", AppLock.verify(context, "1234").success)
    }

    @Test
    fun corruptedHashNeverUnlocksAndDoesNotCrash() {
        AppLock.setPin(context, "1234")
        context.getSharedPreferences("einkauf_app_lock", Context.MODE_PRIVATE)
            .edit()
            .putString("hash", "bozuk-base64!!!")
            .commit()

        val result = AppLock.verify(context, "1234")

        assertFalse("Bozuk özetle doğru PIN bile kabul edilmemeli", result.success)
        assertTrue("Kilit yapılandırması hâlâ etkin görünmeli", AppLock.isEnabled(context))
    }
}
