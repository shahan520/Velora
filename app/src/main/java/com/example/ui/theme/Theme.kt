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
    onBackground = Color(0xFFFAF7FD),
    surface = VeloraPlumSurface,
    onSurface = Color(0xFFFAF7FD),
    surfaceVariant = VeloraPlumMedium,
    onSurfaceVariant = Color(0xFFD8CAE3),
    outline = Color(0xFF3B205B)
)

private val LightColorScheme = lightColorScheme(
    primary = VeloraPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF0F5),
    onPrimaryContainer = VeloraMagenta,
    secondary = VeloraViolet,
    onSecondary = Color.White,
    secondaryContainer = VeloraLavenderLight,
    onSecondaryContainer = VeloraPlumMedium,
    tertiary = VeloraGold,
    onTertiary = Color.Black,
    background = Color(0xFFFCF9FA),
    onBackground = Color(0xFF1E1424),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1424),
    surfaceVariant = Color(0xFFFFF0F5),
    onSurfaceVariant = Color(0xFF75677E),
    outline = Color(0xFFF0E6EB)
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
