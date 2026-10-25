package com.shoppingapp.feature.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shoppingapp.R
import com.shoppingapp.data.offers.FlyerOffer
import com.shoppingapp.data.offers.FlyerPriceLabel
import com.shoppingapp.data.offers.FlyerStore
import java.time.Instant
import java.util.Locale

/**
 * Broşürde bir ürün alanına dokunulduğunda açılan onay sayfası: ürün adı, fiyatı,
 * geçerlilik tarihi ve adet. Onaylanınca ürün fırsat listesine eklenir.
 *
 * Okuyucu zaten tam ekran bir [Dialog] içinde olduğu için [androidx.compose.material3.ModalBottomSheet]
 * kullanılmıyor: alt sayfa kendi penceresine çizilip okuyucunun arkasında kalıyordu.
 */
@Composable
fun FlyerOfferSheet(
    offer: FlyerOffer,
    store: FlyerStore,
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit
) {
    var quantity by remember(offer.id) { mutableIntStateOf(1) }
    val brandColor = Color(store.brandColor)
    val priceText = FlyerPriceLabel.format(offer.price)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Karartma: sayfanın dışına dokununca kapanır.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )

            val cardInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Kartın boş alanına dokunmak sayfayı kapatmasın.
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(brandColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                offer.name.take(1).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = brandColor
                            )
                        }
                        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(
                                offer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                stringResource(R.string.p_x_prospektusten, store.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (offer.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            offer.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            priceText ?: stringResource(R.string.offer_no_price),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        offer.oldPrice?.let { old ->
                            Spacer(Modifier.width(8.dp))
                            Text(
                                FlyerPriceLabel.format(old).orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                    }

                    offer.validTo?.let { validTo ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.p_x_tarihine_kadar_gecerli, FlyerDateLabel.day(validTo)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.s_adet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledIconStep(
                                icon = Icons.Default.Remove,
                                enabled = quantity > 1,
                                description = stringResource(R.string.s_azalt)
                            ) { quantity = (quantity - 1).coerceAtLeast(1) }
                            Text(
                                stringResource(R.string.p_x, quantity),
                                modifier = Modifier.padding(horizontal = 18.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FilledIconStep(
                                icon = Icons.Default.Add,
                                enabled = quantity < 99,
                                description = stringResource(R.string.s_artir)
                            ) { quantity = (quantity + 1).coerceAtMost(99) }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onAdd(quantity) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBasket, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.s_firsat_listesine_ekle), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.s_vazgec))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilledIconStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(icon, contentDescription = description)
    }
}

/** Fırsatın bitiş gününü "24 Eki" biçiminde gösterir. */
internal fun validUntilLabel(validToEpochMs: Long): String =
    FlyerDateLabel.day(Instant.ofEpochMilli(validToEpochMs))

internal fun isExpired(validToEpochMs: Long, now: Long = System.currentTimeMillis()): Boolean =
    validToEpochMs < now
