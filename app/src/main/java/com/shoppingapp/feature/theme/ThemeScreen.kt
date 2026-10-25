package com.shoppingapp.feature.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoppingapp.R
import com.shoppingapp.ui.theme.AppColorTheme
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.ThemeStore
import com.shoppingapp.ui.theme.colorSchemeFor

private const val TAG_COLOR_PREFIX = "theme_color_"
private const val TAG_STYLE_PREFIX = "theme_style_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val selectedColor = ThemeStore.colorTheme
    val selectedStyle = ThemeStore.uiStyle

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.theme_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
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
            modifier = Modifier.fillMaxSize().padding(padding).testTag("theme_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.theme_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item { LivePreview(selectedColor, selectedStyle) }

            item {
                SectionHeading(
                    title = stringResource(R.string.theme_color_section),
                    description = stringResource(R.string.theme_color_section_desc)
                )
            }

            items(AppColorTheme.entries, key = { "color-${it.key}" }) { theme ->
                ColorThemeRow(
                    theme = theme,
                    selected = theme == selectedColor,
                    onClick = { ThemeStore.setColorTheme(context, theme) }
                )
            }

            item {
                SectionHeading(
                    title = stringResource(R.string.theme_ui_section),
                    description = stringResource(R.string.theme_ui_section_desc)
                )
            }

            items(AppUiStyle.entries, key = { "style-${it.key}" }) { style ->
                UiStyleRow(
                    style = style,
                    selected = style == selectedStyle,
                    onClick = { ThemeStore.setUiStyle(context, style) }
                )
            }

            item {
                OutlinedButton(
                    onClick = { ThemeStore.reset(context) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("theme_reset")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.theme_reset))
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, description: String) {
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LivePreview(colorTheme: AppColorTheme, uiStyle: AppUiStyle) {
    val preview = colorSchemeFor(colorTheme, isSystemInDarkTheme())

    Card(
        modifier = Modifier.fillMaxWidth().testTag("theme_preview"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.theme_preview),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Text(
                    text = stringResource(
                        R.string.theme_selection_format,
                        stringResource(colorTheme.labelRes),
                        stringResource(uiStyle.labelRes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = when (uiStyle) {
                    AppUiStyle.CLASSIC -> RoundedCornerShape(0.dp)
                    AppUiStyle.COMPACT -> RoundedCornerShape(6.dp)
                    AppUiStyle.GALLERY -> RoundedCornerShape(18.dp)
                    AppUiStyle.MODERN -> RoundedCornerShape(12.dp)
                },
                color = preview.primaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.s_bugunku_liste),
                            style = MaterialTheme.typography.labelSmall,
                            color = preview.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                        Text(
                            stringResource(R.string.p_x_alinacak_x_alindi, 3, 1),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = preview.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.s_sepet),
                            style = MaterialTheme.typography.labelSmall,
                            color = preview.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                        Text(
                            "€12,40",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = preview.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            PreviewItemRow(preview.surface, preview.onSurface, preview.outlineVariant, checked = false)
            PreviewItemRow(preview.surface, preview.onSurface, preview.outlineVariant, checked = true)

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = when (uiStyle) {
                    AppUiStyle.CLASSIC -> RoundedCornerShape(0.dp)
                    AppUiStyle.COMPACT -> RoundedCornerShape(4.dp)
                    else -> RoundedCornerShape(10.dp)
                },
                color = preview.primary
            ) {
                Text(
                    stringResource(R.string.s_alisverisi_tamamla),
                    style = MaterialTheme.typography.labelLarge,
                    color = preview.onPrimary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PreviewItemRow(surface: Color, onSurface: Color, outline: Color, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else outline, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = surface, modifier = Modifier.size(10.dp))
        }
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(outline.copy(alpha = 0.35f))
        )
        Text(
            stringResource(R.string.s_urun_adi),
            style = MaterialTheme.typography.bodySmall,
            color = onSurface,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        Text("€2,49", style = MaterialTheme.typography.labelSmall, color = onSurface)
    }
}

@Composable
private fun ColorThemeRow(
    theme: AppColorTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val preview = colorSchemeFor(theme, isSystemInDarkTheme())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(TAG_COLOR_PREFIX + theme.key),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaletteSwatch(preview)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    stringResource(theme.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(theme.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(preview: androidx.compose.material3.ColorScheme) {
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(preview.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(preview.primary))
            Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(3.dp)).background(preview.secondary))
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(3.dp)).background(preview.surface))
            }
            Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(preview.primaryContainer))
        }
    }
}

@Composable
private fun UiStyleRow(
    style: AppUiStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(TAG_STYLE_PREFIX + style.key),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StylePreview(style)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    stringResource(style.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(style.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** Kart yerleşimini küçük bir tel kafesle gösterir; her stil gerçekten farklı görünür. */
@Composable
private fun StylePreview(style: AppUiStyle) {
    val surface = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(5.dp)
    ) {
        when (style) {
            AppUiStyle.MODERN -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PreviewBar(accent.copy(alpha = 0.85f), height = 10, rounded = 4)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    PreviewBar(surface, height = 11, rounded = 4, modifier = Modifier.weight(1f))
                    PreviewBar(surface, height = 11, rounded = 4, modifier = Modifier.weight(1f))
                }
                PreviewBar(surface, height = 8, rounded = 4)
            }

            AppUiStyle.CLASSIC -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PreviewBar(line, height = 2, rounded = 0)
                PreviewBar(accent, height = 3, rounded = 0)
                PreviewBar(line.copy(alpha = 0.35f), height = 1, rounded = 0)
                PreviewBar(line, height = 2, rounded = 0)
                PreviewBar(line.copy(alpha = 0.35f), height = 1, rounded = 0)
                PreviewBar(line, height = 2, rounded = 0)
                PreviewBar(line.copy(alpha = 0.35f), height = 1, rounded = 0)
                PreviewBar(line, height = 2, rounded = 0)
            }

            AppUiStyle.COMPACT -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PreviewBar(accent.copy(alpha = 0.8f), height = 4, rounded = 1)
                repeat(5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(5.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(accent.copy(alpha = 0.7f)))
                        PreviewBar(line, height = 3, rounded = 1, modifier = Modifier.weight(1f))
                    }
                }
            }

            AppUiStyle.GALLERY -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    PreviewBar(accent.copy(alpha = 0.75f), height = 16, rounded = 4, modifier = Modifier.weight(1f))
                    PreviewBar(surface, height = 16, rounded = 4, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    PreviewBar(surface, height = 14, rounded = 4, modifier = Modifier.weight(1f))
                    PreviewBar(accent.copy(alpha = 0.45f), height = 14, rounded = 4, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PreviewBar(
    color: Color,
    height: Int,
    rounded: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(rounded.dp))
            .background(color)
    )
}
