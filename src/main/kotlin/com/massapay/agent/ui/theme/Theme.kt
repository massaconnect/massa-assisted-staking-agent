package com.massapay.agent.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// LIGHT THEME - MassaConnect Style
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFAFAFA)
val LightCardBackground = Color(0xFFF5F5F5)
val LightButtonBackground = Color(0xFF000000)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF666666)
val LightTextTertiary = Color(0xFF999999)
val LightBorder = Color(0xFFE0E0E0)
val LightDivider = Color(0xFFEEEEEE)

// DARK THEME COLORS (for Setup Wizard)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val CardBackground = Color(0xFF2A2A2A)
val MassaRed = Color(0xFFE53935)

// ACCENT COLORS
val AccentGreen = Color(0xFF00C853)
val AccentRed = Color(0xFFFF3B30)
val AccentOrange = Color(0xFFFF9500)
val AccentBlue = Color(0xFF007AFF)

private val LightColorScheme = lightColorScheme(
    primary = LightButtonBackground,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A1A1A),
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = AccentGreen,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCardBackground,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    error = AccentRed,
    outline = LightBorder
)

@Composable
fun MassaAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
