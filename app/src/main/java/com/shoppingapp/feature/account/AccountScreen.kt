package com.shoppingapp.feature.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppingapp.R
import com.shoppingapp.account.AccountType
import com.shoppingapp.account.GoogleSignIn
import com.shoppingapp.account.GoogleSignInResult
import com.shoppingapp.money.Currency
import kotlinx.coroutines.launch

/**
 * Hesap ve senkronizasyon ayarları.
 *
 * Senkronizasyon Google Drive'ın uygulamaya ait gizli klasöründe tutulur; başka
 * bir sunucu yoktur. Yerel hesapta veri cihazda kalır, yalnızca Google hesabı
 * cihazlar arasında taşır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val configured = GoogleSignIn.isConfigured()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.account_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AccountCard(
                    title = when (state.account.type) {
                        AccountType.GOOGLE -> stringResource(R.string.account_google)
                        AccountType.LOCAL -> stringResource(R.string.account_local)
                        AccountType.GUEST -> stringResource(R.string.account_guest)
                    },
                    subtitle = state.account.email
                        ?: stringResource(R.string.account_signed_out)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.account.type != AccountType.GOOGLE) {
                        Button(
                            onClick = {
                                scope.launch {
                                    when (val result = GoogleSignIn.signIn(context)) {
                                        is GoogleSignInResult.Success ->
                                            viewModel.onGoogleSignedIn(result.email, result.displayName)
                                        GoogleSignInResult.NotConfigured ->
                                            viewModel.tokenFailed(
                                                context.getString(R.string.onboarding_google_unavailable)
                                            )
                                        GoogleSignInResult.Cancelled -> Unit
                                        is GoogleSignInResult.Failed ->
                                            viewModel.tokenFailed(result.message)
                                    }
                                }
                            },
                            enabled = configured && !state.busy,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(stringResource(R.string.account_sign_in_google))
                        }
                    }

                    if (state.account.type == AccountType.GOOGLE) {
                        Button(
                            onClick = {
                                if (activity == null) return@Button
                                scope.launch {
                                    val token = GoogleSignIn.driveAccessToken(activity)
                                    token.onSuccess { viewModel.sync(it) }
                                        .onFailure {
                                            viewModel.tokenFailed(
                                                GoogleSignIn.describe(it)
                                            )
                                        }
                                }
                            },
                            enabled = !state.busy && configured,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (state.busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null)
                                Text(
                                    stringResource(R.string.account_sync_now),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.account_sync_requires_google),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    state.message?.let { message ->
                        val text = when (message) {
                            SyncMessage.UPLOADED -> R.string.sync_uploaded
                            SyncMessage.DOWNLOADED -> R.string.sync_downloaded
                            SyncMessage.FIRST_UPLOAD -> R.string.sync_first_upload
                        }
                        Text(
                            stringResource(text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    state.error?.let { error ->
                        Text(
                            stringResource(R.string.sync_failed, error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (state.account.type != AccountType.GUEST) {
                        TextButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.account_sign_out))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        stringResource(R.string.onboarding_local_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(Currency.entries, key = { it.code }) { currency ->
                CurrencyRow(
                    currency = currency,
                    selected = currency == state.currency,
                    onClick = { viewModel.setCurrency(currency) }
                )
            }
        }
    }
}

@Composable
private fun CurrencyRow(currency: Currency, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                currency.symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.size(44.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(currency.code, style = MaterialTheme.typography.titleSmall)
                Text(
                    currency.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun AccountCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
