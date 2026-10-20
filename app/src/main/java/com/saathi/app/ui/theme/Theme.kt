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
    primary = SaathiPurple40,
    onPrimary = Color.White,
    primaryContainer = SaathiPurple80,
    onPrimaryContainer = Color(0xFF21005D),
    secondary = SaathiOrange40,
    onSecondary = Color.White,
    secondaryContainer = SaathiOrange80,
    onSecondaryContainer = SaathiOrange40,
    error = RukoRed,
    onError = Color.White,
    errorContainer = RukoRedContainer,
    onErrorContainer = RukoOnRedContainer,
    background = WarmWhite,
    onBackground = Color(0xFF1C1B1F),
    surface = WarmSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
)

private val SaathiDarkColors = darkColorScheme(
    primary = SaathiPurple80,
    onPrimary = Color(0xFF371E73),
    primaryContainer = SaathiPurple40,
    onPrimaryContainer = SaathiPurple80,
    secondary = SaathiOrange80,
    onSecondary = SaathiOrange40,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SaathiTypography,
        content = content
    )
}
