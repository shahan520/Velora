package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VeloraPink,
    onPrimary = Color.White,
    primaryContainer = VeloraPlumMedium,
    onPrimaryContainer = VeloraLavender,
    secondary = VeloraViolet,
    onSecondary = Color.White,
    secondaryContainer = VeloraPlumSurface,
    onSecondaryContainer = VeloraLavenderLight,
    tertiary = VeloraGold,
    onTertiary = Color.Black,
    background = VeloraPlumDark,
    onBackground = Color.White,
    surface = VeloraPlumSurface,
    onSurface = Color.White,
    surfaceVariant = VeloraPlumMedium,
    onSurfaceVariant = VeloraLavender,
    outline = Color(0xFF45276B)
)

private val LightColorScheme = lightColorScheme(
    primary = VeloraPink,
    onPrimary = Color.White,
    primaryContainer = VeloraPinkSoft,
    onPrimaryContainer = VeloraMagenta,
    secondary = VeloraViolet,
    onSecondary = Color.White,
    secondaryContainer = VeloraLavenderLight,
    onSecondaryContainer = VeloraPlumMedium,
    tertiary = VeloraGold,
    onTertiary = Color.Black,
    background = VeloraIvory,
    onBackground = VeloraTextPrimary,
    surface = VeloraSurfaceLight,
    onSurface = VeloraTextPrimary,
    surfaceVariant = VeloraPinkSoft,
    onSurfaceVariant = VeloraTextSecondary,
    outline = VeloraCardBorder
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
