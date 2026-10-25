package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Velora Signature Palette (Static Brand Accents)
val VeloraPink = Color(0xFFFF2D78)
val VeloraRose = Color(0xFFE91E63)
val VeloraMagenta = Color(0xFFD81B60)
val VeloraPinkLight = Color(0xFFFF6492)

// Secondary Plum & Violet
val VeloraPlumDark = Color(0xFF1A0B2E)
val VeloraPlumMedium = Color(0xFF28114B)
val VeloraPlumSurface = Color(0xFF220E3E)
val VeloraViolet = Color(0xFF7C4DFF)
val VeloraLavender = Color(0xFFE1BEE7)
val VeloraLavenderLight = Color(0xFFF3E5F5)

// Premium Accents
val VeloraGold = Color(0xFFFFB300)
val VeloraGoldLight = Color(0xFFFFE082)
val VeloraGoldDark = Color(0xFFC68400)

// Status & Verification
val VeloraVerifiedBlue = Color(0xFF29B6F6)
val VeloraOnlineGreen = Color(0xFF00E676)
val VeloraErrorRed = Color(0xFFFF3D57)
val VeloraPassRed = Color(0xFFFF4D6D)
val VeloraSuperLikeStar = Color(0xFF8E24AA)

// Theme-Aware Dynamic Color Tokens
val VeloraTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Color(0xFFFAF7FD) else Color(0xFF1E1424)

val VeloraTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Color(0xFFD8CAE3) else Color(0xFF6B5878)

val VeloraTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Color(0xFFB5A4C3) else Color(0xFF8C7B96)

val VeloraCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Color(0xFF3B205B) else Color(0xFFF0E6EB)

val VeloraPinkSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Color(0xFF321350) else Color(0xFFFFF0F5)

val VeloraIvory: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) VeloraPlumDark else Color(0xFFFCF9FA)

val VeloraSurfaceLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) VeloraPlumSurface else Color(0xFFFFFFFF)
