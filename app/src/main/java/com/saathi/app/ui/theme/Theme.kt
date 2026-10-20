package com.saathi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SaathiLightColors = lightColorScheme(
    primary = SaathiTealPrimary,
    onPrimary = Color.White,
    primaryContainer = SaathiTealContainer,
    onPrimaryContainer = SaathiOnTealContainer,
    secondary = SaathiAmberAccent,
    onSecondary = Color(0xFF2C1600),
    secondaryContainer = SaathiAmberContainer,
    onSecondaryContainer = SaathiOnAmberContainer,
    error = RukoRed,
    onError = Color.White,
    errorContainer = RukoRedContainer,
    onErrorContainer = RukoOnRedContainer,
    background = SaathiCreamBg,
    onBackground = Color(0xFF191C1D),
    surface = SaathiSurface,
    onSurface = Color(0xFF191C1D),
    surfaceVariant = SaathiSurfaceVariant,
    onSurfaceVariant = Color(0xFF40484A),
)

private val SaathiDarkColors = darkColorScheme(
    primary = SaathiTealLight,
    onPrimary = Color(0xFF00363B),
    primaryContainer = SaathiTealDark,
    onPrimaryContainer = SaathiTealContainer,
    secondary = SaathiAmberAccent,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = SaathiAmberDark,
    onSecondaryContainer = SaathiAmberContainer,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF101415),
    onBackground = Color(0xFFE1E3E4),
    surface = Color(0xFF101415),
    onSurface = Color(0xFFE1E3E4),
)

@Composable
fun SaathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SaathiDarkColors else SaathiLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SaathiTypography,
        content = content
    )
}
