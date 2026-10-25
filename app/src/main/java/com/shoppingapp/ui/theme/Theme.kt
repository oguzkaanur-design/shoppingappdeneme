package com.shoppingapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Arayüz stili tüm ağaçtan okunabilir; liste ekranı buna göre farklı bileşenler seçer. */
val LocalAppUiStyle = staticCompositionLocalOf { AppUiStyle.MODERN }

private val Ink = Color(0xFF0F172A)

// ---------------------------------------------------------------- Sistem / zümrüt

private val SystemLightColors = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    inversePrimary = Color(0xFF6EE7B7),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF075985),
    background = Color(0xFFF5FAF7),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF6F1),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = Color(0xFF047857),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF1F5F9),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE8F0EB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FCFA),
    surfaceContainer = Color(0xFFF0F7F3),
    surfaceContainerHigh = Color(0xFFEAF3ED),
    surfaceContainerHighest = Color(0xFFE2EDE6)
)

private val SystemDarkColors = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    inversePrimary = Color(0xFF047857),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF075985),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1B263B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceTint = Color(0xFF34D399),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0B1220),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2B3A52),
    surfaceDim = Color(0xFF070D17),
    surfaceContainerLowest = Color(0xFF060B13),
    surfaceContainerLow = Color(0xFF0D1523),
    surfaceContainer = Color(0xFF121C2D),
    surfaceContainerHigh = Color(0xFF182338),
    surfaceContainerHighest = Color(0xFF1E2B42)
)

// ------------------------------------------------------------------------- Beyaz

private val WhiteColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    inversePrimary = Color(0xFF93C5FD),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0B1220),
    surface = Color.White,
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF4B5563),
    surfaceTint = Color(0xFF1D4ED8),
    inverseSurface = Color(0xFF111827),
    inverseOnSurface = Color(0xFFF9FAFB),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFDDE4EE),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE9EEF7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFD),
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerHigh = Color(0xFFEAEFF7),
    surfaceContainerHighest = Color(0xFFE3E9F2)
)

// ------------------------------------------------------------------------- Siyah

private val BlackColors = darkColorScheme(
    primary = Color(0xFFA3E635),
    onPrimary = Color(0xFF1A2E05),
    primaryContainer = Color(0xFF365314),
    onPrimaryContainer = Color(0xFFECFCCB),
    inversePrimary = Color(0xFF4D7C0F),
    secondary = Color(0xFFFB923C),
    onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Color.Black,
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF080808),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFB5B5B5),
    surfaceTint = Color(0xFFA3E635),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF0A0A0A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF525252),
    outlineVariant = Color(0xFF262626),
    scrim = Color.Black,
    surfaceBright = Color(0xFF262626),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1C1C1C)
)

// -------------------------------------------------------------------------- Gece

private val NightColors = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    inversePrimary = Color(0xFF7C3AED),
    secondary = Color(0xFFF0ABFC),
    onSecondary = Color(0xFF4A044E),
    secondaryContainer = Color(0xFF701A75),
    onSecondaryContainer = Color(0xFFFAE8FF),
    tertiary = Color(0xFF67E8F9),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF100818),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF1A1028),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF2A1740),
    onSurfaceVariant = Color(0xFFD8B4FE),
    surfaceTint = Color(0xFFC4B5FD),
    inverseSurface = Color(0xFFF5F3FF),
    inverseOnSurface = Color(0xFF1A1028),
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6),
    outline = Color(0xFF7E5A9E),
    outlineVariant = Color(0xFF3B2551),
    scrim = Color.Black,
    surfaceBright = Color(0xFF3A2350),
    surfaceDim = Color(0xFF0B0412),
    surfaceContainerLowest = Color(0xFF0B0412),
    surfaceContainerLow = Color(0xFF150B21),
    surfaceContainer = Color(0xFF20122F),
    surfaceContainerHigh = Color(0xFF29173C),
    surfaceContainerHighest = Color(0xFF331D49)
)

// ------------------------------------------------------------------------- Yeşil

private val GreenColors = lightColorScheme(
    primary = Color(0xFF166534),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    inversePrimary = Color(0xFF86EFAC),
    secondary = Color(0xFF4D7C0F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFCCB),
    onSecondaryContainer = Color(0xFF1A2E05),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),
    background = Color(0xFFF0F7F1),
    onBackground = Color(0xFF14261A),
    surface = Color.White,
    onSurface = Color(0xFF14261A),
    surfaceVariant = Color(0xFFE6F1E8),
    onSurfaceVariant = Color(0xFF4B6152),
    surfaceTint = Color(0xFF166534),
    inverseSurface = Color(0xFF14261A),
    inverseOnSurface = Color(0xFFF0F7F1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF8FAA97),
    outlineVariant = Color(0xFFD7E6DB),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE2EFE4),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FCF8),
    surfaceContainer = Color(0xFFEDF6EE),
    surfaceContainerHigh = Color(0xFFE5F1E7),
    surfaceContainerHighest = Color(0xFFDDEDE0)
)

fun isDarkColorTheme(theme: AppColorTheme, systemDark: Boolean): Boolean =
    theme.forcedDark ?: systemDark

fun colorSchemeFor(theme: AppColorTheme, systemDark: Boolean): ColorScheme = when (theme) {
    AppColorTheme.SYSTEM -> if (systemDark) SystemDarkColors else SystemLightColors
    AppColorTheme.WHITE -> WhiteColors
    AppColorTheme.BLACK -> BlackColors
    AppColorTheme.NIGHT -> NightColors
    AppColorTheme.GREEN -> GreenColors
}

private fun shapesFor(style: AppUiStyle): Shapes = when (style) {
    AppUiStyle.MODERN -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp)
    )

    AppUiStyle.CLASSIC -> Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(1.dp),
        medium = RoundedCornerShape(2.dp),
        large = RoundedCornerShape(3.dp),
        extraLarge = RoundedCornerShape(4.dp)
    )

    AppUiStyle.COMPACT -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(12.dp)
    )

    AppUiStyle.GALLERY -> Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp)
    )
}

private fun typographyFor(style: AppUiStyle): Typography {
    val base = Typography()
    return when (style) {
        AppUiStyle.MODERN -> Typography(
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        )

        AppUiStyle.CLASSIC -> base.copy(
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 36.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 32.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Serif, fontSize = 21.sp, lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold
            ),
            titleSmall = TextStyle(
                fontFamily = FontFamily.Serif, fontSize = 15.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
            ),
            bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 12.sp, lineHeight = 17.sp),
            labelLarge = TextStyle(
                fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            ),
            labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.9.sp),
            labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.sp)
        )

        AppUiStyle.COMPACT -> Typography(
            headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
            titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
            bodyMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
            bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
            labelLarge = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
            labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
            labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)
        )

        AppUiStyle.GALLERY -> Typography(
            displaySmall = TextStyle(fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold),
            headlineMedium = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold),
            headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
            labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun ShoppingAppTheme(
    colorTheme: AppColorTheme = ThemeStore.colorTheme,
    uiStyle: AppUiStyle = ThemeStore.uiStyle,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = isDarkColorTheme(colorTheme, systemDark)
    val colorScheme = colorSchemeFor(colorTheme, systemDark)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Renkler edge-to-edge ile sistem tarafından yönetilir; yalnızca ikon
            // kontrastı ayarlanır (statusBarColor/navigationBarColor API 35+ no-op).
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppUiStyle provides uiStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyFor(uiStyle),
            shapes = shapesFor(uiStyle),
            content = content
        )
    }
}
