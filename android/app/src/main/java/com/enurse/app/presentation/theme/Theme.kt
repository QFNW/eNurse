package com.enurse.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════
//  易护 — 温润蓝白 品牌色板
// ═══════════════════════════════════════

// Primary blues
val Primary = Color(0xFF1565C0)
val PrimaryLight = Color(0xFF1E88E5)
val PrimaryDark = Color(0xFF0D47A1)
val PrimaryBg = Color(0xFFE3F2FD)
val PrimarySurface = Color(0xFFF0F7FF)

// Surfaces
val SurfaceWhite = Color(0xFFFFFFFF)
val Background = Color(0xFFF8FAFB)
val CardBorder = Color(0xFFE8EDF2)

// Text
val TextPrimary = Color(0xFF1A1D21)
val TextSecondary = Color(0xFF5F6B7A)
val TextHint = Color(0xFF9BA5B1)

// Semantic
val ErrorRed = Color(0xFFD32F2F)
val ErrorBg = Color(0xFFFFF5F5)
val ErrorBorder = Color(0xFFFFCDD2)
val SuccessGreen = Color(0xFF2E7D32)
val SuccessBg = Color(0xFFE8F5E9)
val Warning = Color(0xFFED6C02)

// Dark theme
private val DarkBackground = Color(0xFF0F1922)
private val DarkSurface = Color(0xFF1A2633)
private val DarkPrimary = Color(0xFF42A5F5)

// ═══════════════════════════════════════
//  Material 3 Light Color Scheme
// ═══════════════════════════════════════

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryBg,
    onPrimaryContainer = PrimaryDark,
    secondary = PrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = PrimarySurface,
    onSecondaryContainer = PrimaryDark,
    tertiary = SuccessGreen,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorBg,
    onErrorContainer = ErrorRed,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    background = Background,
    onBackground = TextPrimary,
    outline = CardBorder,
    outlineVariant = CardBorder,
    surfaceVariant = PrimarySurface,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = DarkPrimary,
    onSecondary = Color.Black,
    surface = DarkSurface,
    onSurface = Color(0xFFE0E6ED),
    background = DarkBackground,
    onBackground = Color(0xFFE0E6ED),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFF3E1515),
)

// ═══════════════════════════════════════
//  Shapes — 圆角体系
// ═══════════════════════════════════════

val LargeRadius = 20.dp
val MediumRadius = 16.dp
val ButtonRadius = 28.dp
val InputRadius = 12.dp
val SmallRadius = 8.dp

private val AppShapes = Shapes(
    extraLarge = RoundedCornerShape(LargeRadius),
    large = RoundedCornerShape(MediumRadius),
    medium = RoundedCornerShape(InputRadius),
    small = RoundedCornerShape(SmallRadius),
)

// ═══════════════════════════════════════
//  Typography — 字体体系
// ═══════════════════════════════════════

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, color = TextPrimary),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, color = TextPrimary),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp, color = TextPrimary),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, color = TextPrimary),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, color = TextSecondary),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, color = TextSecondary),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, color = TextHint),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
)

// ═══════════════════════════════════════
//  Theme composable
// ═══════════════════════════════════════

@Composable
fun eNurseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}

// ═══════════════════════════════════════
//  Backward-compatible color aliases (still in use)
// ═══════════════════════════════════════
val AlertRed = ErrorRed
val AlertRedBg = ErrorBg
// TODO: Migrate screens to MaterialTheme.colorScheme for dark mode support
// Currently screens use top-level Color constants, so dark theme has no effect.
