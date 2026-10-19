package com.shoppingapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shoppingapp.R
import com.shoppingapp.data.catalog.BrandCatalog
import com.shoppingapp.data.model.Product

/**
 * Bundled category photography means every row has a real photo instantly and
 * offline. When a barcode lookup supplied an actual product photo, it is drawn
 * on top and replaces the category image.
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
        Image(
            painter = painterResource(categoryDrawableFor(product)),
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun categoryDrawable(categoryId: Long): Int = when (categoryId) {
    1L -> R.drawable.cat_produce
    2L -> R.drawable.cat_dairy
    3L -> R.drawable.cat_drinks
    4L -> R.drawable.cat_snacks
    5L -> R.drawable.cat_household
    else -> R.drawable.cat_default
}

/**
 * Görsel seçimi: kayıtlı kategori yoksa ürün adındaki kelimelere bakılır
 * ("yoğurt" → süt ürünleri fotoğrafı), böylece liste satırları doğru resmi alır.
 */
fun categoryDrawableFor(product: Product): Int {
    if (product.categoryId in 1L..5L) return categoryDrawable(product.categoryId)
    val guessed = BrandCatalog.detectCategory(product.name) ?: return R.drawable.cat_default
    return categoryDrawable(guessed)
}
