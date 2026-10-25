package com.shoppingapp.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoppingapp.R

/**
 * Premium durum kartı ve lisans etkinleştirme diyaloğu.
 *
 * Kart yalnızca bilgi gösterir; premium kararını [PremiumGate] verir. Token
 * doğrulanmadan kaydedilmez, bu yüzden buradaki metin alanına ne yazılırsa
 * yazılsın premium açılamaz.
 */
@Composable
fun PremiumCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(PremiumGate.state(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val (subtitle, icon, tint) = when (state) {
        PremiumGate.State.ACTIVE -> Triple(
            stringResource(
                R.string.premium_subtitle_active,
                PremiumGate.activeLicense(context)?.plan.orEmpty()
            ),
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary
        )
        PremiumGate.State.EXPIRED -> Triple(
            stringResource(R.string.premium_subtitle_expired),
            Icons.Default.ErrorOutline,
            MaterialTheme.colorScheme.error
        )
        PremiumGate.State.TAMPERED -> Triple(
            stringResource(R.string.premium_subtitle_tampered),
            Icons.Default.ErrorOutline,
            MaterialTheme.colorScheme.error
        )
        PremiumGate.State.LOCKED -> Triple(
            stringResource(R.string.premium_subtitle_locked),
            Icons.Default.WorkspacePremium,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("premium_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(10.dp)
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        stringResource(R.string.premium_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state != PremiumGate.State.ACTIVE) {
                TextButton(
                    onClick = {
                        token = ""
                        error = false
                        showDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("premium_activate")
                ) {
                    Text(stringResource(R.string.premium_activate))
                }
            } else {
                Text(
                    stringResource(R.string.premium_offline_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        PremiumGate.deactivate(context)
                        state = PremiumGate.state(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.premium_deactivate))
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.premium_activate_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.premium_activate_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it
                            error = false
                        },
                        singleLine = false,
                        maxLines = 3,
                        isError = error,
                        modifier = Modifier.fillMaxWidth().testTag("premium_token")
                    )
                    if (error) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.premium_activate_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("premium_error")
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (PremiumGate.activate(context, token)) {
                            state = PremiumGate.state(context)
                            showDialog = false
                        } else {
                            error = true
                        }
                    },
                    modifier = Modifier.testTag("premium_confirm")
                ) {
                    Text(stringResource(R.string.premium_activate))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
