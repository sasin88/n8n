package com.nutriai.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nutriai.domain.model.ThemeMode

/** true cuando la app se dibuja en modo oscuro (para el cristal y el fondo). */
val LocalDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Accent.copy(alpha = 0.16f),
    background = LightBase,
    onBackground = LightLabel,
    surface = Color.White,
    onSurface = LightLabel,
    surfaceVariant = Color(0x14000000),
    onSurfaceVariant = LightSecondaryLabel,
    outline = LightSeparator,
    error = ErrorLight,
    surfaceContainerHigh = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF7F7FA),
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF00210F),
    primaryContainer = AccentDark.copy(alpha = 0.18f),
    background = DarkBase,
    onBackground = DarkLabel,
    surface = Color(0xFF1C1C1E),
    onSurface = DarkLabel,
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = DarkSecondaryLabel,
    outline = DarkSeparator,
    error = ErrorDark,
    surfaceContainerHigh = Color(0xFF242428),
    surfaceContainer = Color(0xFF242428),
)

@Composable
fun NutriAiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = NutriAiTypography,
            shapes = NutriAiShapes,
            content = content,
        )
    }
}
