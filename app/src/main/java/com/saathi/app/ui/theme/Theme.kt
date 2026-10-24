package com.saathi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ---------- 1. Colour Tokens (Section 8.1) ----------
data class SevaColors(
    val canvas: Color = Color(0xFFFFF8EC),
    val surface1: Color = Color(0xFFFFFFFF),
    val surface2: Color = Color(0xFFFFF1DA),
    val surfaceMint: Color = Color(0xFFD6EFE8),
    val ink: Color = Color(0xFF1E1E1E),
    val inkDeep: Color = Color(0xFF0B4F44),
    val onSurfaceVariant: Color = Color(0xFF4F5E5A),
    val teal: Color = Color(0xFF0F6B5C),
    val onTeal: Color = Color(0xFFFFFFFF),
    val saffron: Color = Color(0xFFF5A623),
    val onSaffron: Color = Color(0xFF2B1B00),
    val danger: Color = Color(0xFFC62828),
    val onDanger: Color = Color(0xFFFFFFFF),
    val success: Color = Color(0xFF2E7D32),
    val warning: Color = Color(0xFF8A4B08),
    val outline: Color = Color(0xFFD7DEDA),
    val secondary: Color = Color(0xFF5D65A6),
) {
    val background: Color get() = canvas
    val onSurface: Color get() = ink
    val onPrimary: Color get() = onTeal
    val mint: Color get() = surfaceMint
    val peach: Color get() = Color(0xFFFFE0B2)
    val safeGreen: Color get() = success
}

// Tuned dark scheme (not a simple inversion)
val SevaColorsDark = SevaColors(
    canvas = Color(0xFF141210),
    surface1 = Color(0xFF1F1D1A),
    surface2 = Color(0xFF2A251F),
    surfaceMint = Color(0xFF1E2E29),
    ink = Color(0xFFF2EDE4),
    inkDeep = Color(0xFF9DC9C0),
    onSurfaceVariant = Color(0xFFB9B2A6),
    teal = Color(0xFF7FBFB2),
    onTeal = Color(0xFF06251F),
    saffron = Color(0xFFD9A24A),
    onSaffron = Color(0xFF241703),
    danger = Color(0xFFE57368),
    onDanger = Color(0xFF2B0805),
    success = Color(0xFF8CC79A),
    warning = Color(0xFFD9A24A),
    outline = Color(0xFF3A362F),
)

val LocalSevaColors = staticCompositionLocalOf { SevaColors() }
val LocalSimpleMode = compositionLocalOf { false }

// ---------- 2. Typography (Section 8.2) ----------
data class SevaTypography(
    val display: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 56.sp
    ),
    val headline: TextStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 40.sp
    ),
    val title: TextStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 34.sp
    ),
    val body: TextStyle = TextStyle( // Minimum 18sp anywhere in the app
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 29.sp
    ),
    val label: TextStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    val caption: TextStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

val LocalSevaTypography = staticCompositionLocalOf { SevaTypography() }

// ---------- 3. Shape Tokens (Section 8.3) ----------
data class SevaShapes(
    val card: RoundedCornerShape = RoundedCornerShape(28.dp),
    val tile: RoundedCornerShape = RoundedCornerShape(24.dp),
    val button: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val sheetTop: RoundedCornerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
)

val LocalSevaShapes = staticCompositionLocalOf { SevaShapes() }

// ---------- 4. Spacing Scale (Section 8.3) ----------
data class SevaSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalSevaSpacing = staticCompositionLocalOf { SevaSpacing() }

// ---------- 5. Master Theme Wrapper ----------

/**
 * SevaCare Master Theme.
 * Single source of design truth for all screens, tokens, and gestures.
 */
@Composable
fun SevaTheme(
    darkThemeEnabled: Boolean = false,
    simpleMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkThemeEnabled && isSystemInDarkTheme()) SevaColorsDark else SevaColors()
    val motion = resolveMotion()
    val haptics = rememberSevaHaptics()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.canvas.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkThemeEnabled
        }
    }

    CompositionLocalProvider(
        LocalSevaColors provides colors,
        LocalSevaTypography provides SevaTypography(),
        LocalSevaShapes provides SevaShapes(),
        LocalSevaSpacing provides SevaSpacing(),
        LocalMotion provides motion,
        LocalReducedMotion provides (motion == SevaMotion.Reduced),
        LocalSevaHaptics provides haptics,
        LocalSimpleMode provides simpleMode
    ) {
        MaterialTheme(
            colorScheme = materialSchemeFrom(colors),
            typography = SaathiTypography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp)
            ),
            content = content
        )
    }
}

private fun materialSchemeFrom(c: SevaColors) =
    lightColorScheme(
        primary = c.teal,
        onPrimary = c.onTeal,
        secondary = c.secondary,
        onSecondary = c.onTeal,
        tertiary = c.saffron,
        onTertiary = c.onSaffron,
        background = c.canvas,
        onBackground = c.ink,
        surface = c.surface1,
        onSurface = c.ink,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.onSurfaceVariant,
        error = c.danger,
        onError = c.onDanger,
        outline = c.outline
    )

/**
 * Backward compatibility wrapper for existing call sites.
 */
@Composable
fun SaathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = SevaTheme(darkThemeEnabled = darkTheme, content = content)
