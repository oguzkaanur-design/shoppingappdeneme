package com.shoppingapp.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.shoppingapp.R

/**
 * Cihazda hesap oluşturma. Doğrulama [LocalAccount] içinde; burada yalnızca
 * alanlar ve hata mesajı yönetilir. Şifre hiçbir yerde saklanmaz.
 */
@Composable
fun LocalAccountDialog(
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun submit() {
        val result = LocalAccount.create(context, email, password, repeated)
        val failure = result.exceptionOrNull()
        if (failure is LocalAccountException) {
            errorRes = when (failure.error) {
                LocalAccountError.EMAIL_INVALID -> R.string.onboarding_error_email
                LocalAccountError.PASSWORD_TOO_SHORT -> R.string.onboarding_error_password
                LocalAccountError.PASSWORD_MISMATCH -> R.string.onboarding_error_mismatch
                else -> R.string.onboarding_error_exists
            }
        } else if (result.isSuccess) {
            onCreated()
        } else {
            errorRes = R.string.onboarding_error_email
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.onboarding_local_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.onboarding_local_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text(stringResource(R.string.onboarding_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.onboarding_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repeated,
                    onValueChange = { repeated = it },
                    label = { Text(stringResource(R.string.onboarding_password_repeat)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                errorRes?.let {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text(stringResource(R.string.onboarding_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
