package com.shoppingapp.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.shoppingapp.R

enum class LockDialogMode { ENABLE, DISABLE, CHANGE }

/**
 * PIN belirleme, kaldırma ve değiştirme. Kaydetme işlemi burada yapılır;
 * çağıran ekran yalnızca sonucu yansıtır.
 */
@Composable
fun AppLockSettingsDialog(
    mode: LockDialogMode,
    onDismiss: () -> Unit,
    onComplete: (enabled: Boolean, biometricEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (mode) {
        LockDialogMode.ENABLE -> stringResource(R.string.lock_settings_title)
        LockDialogMode.DISABLE -> stringResource(R.string.lock_disable)
        LockDialogMode.CHANGE -> stringResource(R.string.lock_change_pin)
    }
    val confirmLabel = when (mode) {
        LockDialogMode.ENABLE -> stringResource(R.string.lock_enable)
        LockDialogMode.DISABLE -> stringResource(R.string.lock_disable)
        LockDialogMode.CHANGE -> stringResource(R.string.lock_change_pin)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mode != LockDialogMode.ENABLE) {
                    PinField(
                        value = currentPin,
                        onValueChange = { currentPin = it; error = null },
                        label = stringResource(R.string.lock_pin_current)
                    )
                }
                if (mode != LockDialogMode.DISABLE) {
                    PinField(
                        value = newPin,
                        onValueChange = { newPin = it; error = null },
                        label = stringResource(R.string.lock_pin_new)
                    )
                    PinField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it; error = null },
                        label = stringResource(R.string.lock_pin_confirm)
                    )
                }
                error?.let {
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (mode) {
                        LockDialogMode.ENABLE -> {
                            if (newPin.length != AppLock.PIN_LENGTH) {
                                error = context.getString(R.string.lock_wrong_pin)
                            } else if (newPin != confirmPin) {
                                error = context.getString(R.string.lock_pin_mismatch)
                            } else {
                                AppLock.setPin(context, newPin)
                                onComplete(true, AppLock.isBiometricEnabled(context))
                            }
                        }

                        LockDialogMode.DISABLE -> {
                            val result = AppLock.verify(context, currentPin)
                            if (!result.success) {
                                error = result.lockoutSeconds.takeIf { it > 0 }
                                    ?.let { context.getString(R.string.lock_locked_out, it) }
                                    ?: context.getString(R.string.lock_wrong_pin)
                            } else {
                                AppLock.clear(context)
                                onComplete(false, false)
                            }
                        }

                        LockDialogMode.CHANGE -> {
                            val result = AppLock.verify(context, currentPin)
                            if (!result.success) {
                                error = result.lockoutSeconds.takeIf { it > 0 }
                                    ?.let { context.getString(R.string.lock_locked_out, it) }
                                    ?: context.getString(R.string.lock_wrong_pin)
                            } else if (newPin.length != AppLock.PIN_LENGTH) {
                                error = context.getString(R.string.lock_wrong_pin)
                            } else if (newPin != confirmPin) {
                                error = context.getString(R.string.lock_pin_mismatch)
                            } else {
                                AppLock.setPin(context, newPin)
                                onComplete(true, AppLock.isBiometricEnabled(context))
                            }
                        }
                    }
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= AppLock.PIN_LENGTH && input.all(Char::isDigit)) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}
