package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IosBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003975),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = IosGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E4F24),
    onSecondaryContainer = Color(0xFFB8F5C8),
    tertiary = IosOrange,
    background = IosSheetBgDark,
    onBackground = Color(0xFFF2F2F7),
    surface = IosCardDark,
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFFAEAEB2),
    error = IosRed
)

private val LightColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EBFF),
    onPrimaryContainer = Color(0xFF002B5E),
    secondary = IosGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F8DF),
    onSecondaryContainer = Color(0xFF073B18),
    tertiary = IosOrange,
    background = IosSystemGray6,
    onBackground = Color(0xFF1C1C1E),
    surface = IosCardLight,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = IosSystemGray5,
    onSurfaceVariant = Color(0xFF636366),
    error = IosRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
