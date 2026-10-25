package com.shoppingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shoppingapp.R
import com.shoppingapp.data.catalog.ProductAvatars
import com.shoppingapp.data.model.Product

/**
 * Only a product image returned for its exact barcode is shown as a photo.
 * Manually entered items use the symbol that matches their product type
 * (milk, cheese, vegetables, cleaning …).
 *
 * @param onClick verilirse simge dokunulabilir olur ve avatar seçimini açar;
 * null olduğu yerlerde (fiş, ödeme diyaloğu, tarayıcı) yalnızca gösterilir.
 */
@Composable
fun ProductThumb(
    product: Product,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null
) {
    val hasPhoto = !product.imageUrl.isNullOrBlank()
    val editable = onClick != null && !hasPhoto
    val editLabel = stringResource(R.string.s_avatar_sec)

    Box(
        modifier = modifier
            .testTag("product_thumb_${product.id}")
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (editable) {
                    Modifier.clickable(onClickLabel = editLabel) { onClick?.invoke() }
                } else {
                    Modifier
                }
            )
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sabit boyutlu kutuda simge kutuyu taşmasın: punto kutuya göre seçilir.
                val side = minOf(maxWidth.value, maxHeight.value)
                Text(
                    text = ProductAvatars.emojiFor(product),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    fontSize = (side * 0.66f).sp,
                    lineHeight = (side * 0.8f).sp
                )
            }
        }
    }
}
