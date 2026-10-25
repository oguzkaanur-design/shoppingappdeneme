package com.shoppingapp.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Kilit durumunu yapılandırma değişimleri boyunca korur (döndürme, dil değişimi).
 *
 * ViewModel yalnızca süreç içinde yaşar: uygulama süreci öldüğünde yeniden
 * oluşturulur ve kilit, kayıtlı PIN varsa yeniden devreye girer. Böylece
 * döndürmede kilit ekranı gereksiz yere çıkmaz, süreç yeniden başlatıldığında
 * ise oturum açık kalmaz.
 */
@HiltViewModel
class AppLockStateViewModel @Inject constructor() : ViewModel() {

    private var initialized = false

    var locked by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun initialize(lockEnabled: Boolean) {
        if (initialized) return
        initialized = true
        locked = lockEnabled
    }

    fun lock() {
        locked = true
        error = null
    }

    fun unlock() {
        locked = false
        error = null
    }

    fun showError(message: String) {
        error = message
    }
}
