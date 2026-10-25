package com.shoppingapp.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.account.AccountState
import com.shoppingapp.account.AccountStore
import com.shoppingapp.account.AccountType
import com.shoppingapp.money.Currency
import com.shoppingapp.money.CurrencyStore
import com.shoppingapp.sync.SyncManager
import com.shoppingapp.sync.SyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Arayüzde gösterilecek senkronizasyon sonucu; metin çevirisi ekranda yapılır. */
enum class SyncMessage { UPLOADED, DOWNLOADED, FIRST_UPLOAD }

data class AccountUiState(
    val account: AccountState = AccountState(),
    val currency: Currency = Currency.EUR,
    val busy: Boolean = false,
    val message: SyncMessage? = null,
    val error: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state = _state.asStateFlow()

    private fun readState() = AccountUiState(
        account = AccountStore.current(context),
        currency = CurrencyStore.current(context)
    )

    fun refresh() {
        _state.value = readState()
    }

    fun setCurrency(currency: Currency) {
        CurrencyStore.set(context, currency)
        _state.value = readState()
    }

    fun onGoogleSignedIn(email: String?, displayName: String?) {
        AccountStore.signIn(context, AccountType.GOOGLE, email, displayName)
        refresh()
    }

    fun signOut() {
        AccountStore.signOut(context)
        refresh()
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    /** Drive belirtecini alıp senkronizasyonu çalıştırır. */
    fun sync(token: String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, message = null, error = null)
        viewModelScope.launch {
            val result = syncManager.syncNow(token)
            _state.value = _state.value.copy(
                busy = false,
                message = result.getOrNull()?.toMessage(),
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun tokenFailed(message: String) {
        _state.value = _state.value.copy(busy = false, message = null, error = message)
    }

    private fun SyncOutcome.toMessage(): SyncMessage = when (this) {
        SyncOutcome.DOWNLOADED -> SyncMessage.DOWNLOADED
        SyncOutcome.UPLOADED -> SyncMessage.UPLOADED
        SyncOutcome.FIRST_UPLOAD -> SyncMessage.FIRST_UPLOAD
    }
}
