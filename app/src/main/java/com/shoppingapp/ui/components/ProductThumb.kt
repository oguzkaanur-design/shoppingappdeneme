package com.shoppingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shoppingapp.data.catalog.ProductIllustration
import com.shoppingapp.data.model.Product

/**
 * Only a product image returned for its exact barcode is shown as a photo.
 * Manually entered items use a matching symbol instead of unrelated stock art.
 */
@Composable
fun ProductThumb(
    product: Product,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = ProductIllustration.forProductName(product.name),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize().padding(4.dp).wrapContentSize(Alignment.Center),
                fontSize = 30.sp,
                lineHeight = 36.sp
            )
        }
    }
}
