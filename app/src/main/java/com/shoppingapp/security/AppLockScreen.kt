package com.shoppingapp.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoppingapp.R

/**
 * Kilit açılana kadar uygulama içeriğinin yerine çizilir. Geri tuşu içeriği
 * göstermez; PIN veya biyometrik doğrulama gerekir.
 */
@Composable
fun AppLockScreen(
    errorMessage: String?,
    biometricEnabled: Boolean,
    onSubmit: (String) -> Unit,
    onBiometric: () -> Unit
) {
    BackHandler(enabled = true) { /* kilit ekranı geri tuşuyla kapanmaz */ }

    var pin by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.8f))

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                stringResource(R.string.lock_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp),
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.lock_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(AppLock.PIN_LENGTH) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < pin.length) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(top = 12.dp)
            )

            Spacer(Modifier.weight(0.5f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { digit ->
                            DigitButton(digit) {
                                if (pin.length < AppLock.PIN_LENGTH) {
                                    val next = pin + digit
                                    if (next.length == AppLock.PIN_LENGTH) {
                                        onSubmit(next)
                                        pin = ""
                                    } else {
                                        pin = next
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (biometricEnabled) {
                        IconButton(onClick = onBiometric, modifier = Modifier.size(64.dp)) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = stringResource(R.string.lock_biometric_unlock),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(64.dp))
                    }

                    DigitButton("0") {
                        if (pin.length < AppLock.PIN_LENGTH) {
                            val next = pin + "0"
                            if (next.length == AppLock.PIN_LENGTH) {
                                onSubmit(next)
                                pin = ""
                            } else {
                                pin = next
                            }
                        }
                    }

                    if (pin.isNotEmpty()) {
                        IconButton(
                            onClick = { pin = pin.dropLast(1) },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(Modifier.size(64.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DigitButton(digit: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            fontSize = 26.sp
        )
    }
}
