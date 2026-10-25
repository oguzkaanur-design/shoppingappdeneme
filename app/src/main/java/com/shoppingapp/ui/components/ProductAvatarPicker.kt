package com.shoppingapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shoppingapp.R
import com.shoppingapp.data.catalog.ProductAvatars
import com.shoppingapp.data.model.Category

/**
 * Listede ürünün simgesine dokununca açılan seçim: ürünün hangi türde olduğunu
 * kullanıcı bir bakışta değiştirebilsin diye simgeler kategori başlıkları altında
 * gösterilir. "Otomatik" seçeneği seçimi siler, simge yine addan tahmin edilir.
 */
@Composable
fun ProductAvatarPickerDialog(
    productName: String,
    currentKey: String?,
    categories: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val grouped = ProductAvatars.all.groupBy { it.categoryId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.testTag("product_avatar_picker")
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.s_avatar_sec),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .testTag("product_avatar_grid"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "auto") {
                        Surface(
                            onClick = { onSelect(null) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentKey.isNullOrBlank()) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            contentColor = if (currentKey.isNullOrBlank()) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.fillMaxWidth().testTag("product_avatar_auto")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✨", fontSize = 20.sp)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text(
                                    text = stringResource(R.string.s_otomatik),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    grouped.forEach { (categoryId, avatars) ->
                        item(span = { GridItemSpan(maxLineSpan) }, key = "header-$categoryId") {
                            Text(
                                text = categoryName(categoryId, categories),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(avatars, key = { it.key }) { avatar ->
                            AvatarCell(
                                emoji = avatar.emoji,
                                label = avatar.name,
                                selected = avatar.key == currentKey,
                                onClick = { onSelect(avatar.key) },
                                modifier = Modifier.testTag("product_avatar_${avatar.key}")
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.s_kapat))
                }
            }
        }
    }
}

@Composable
private fun AvatarCell(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp, lineHeight = 28.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Kategori başlığı: veritabanındaki ad varsa o, yoksa sabit karşılığı. */
private fun categoryName(categoryId: Long, categories: List<Category>): String {
    categories.firstOrNull { it.id == categoryId }?.nameTr?.let { return it }
    return when (categoryId) {
        1L -> "Meyve & Sebze"
        2L -> "Süt Ürünleri"
        3L -> "İçecekler"
        4L -> "Atıştırmalık"
        else -> "Temizlik & Ev"
    }
}
