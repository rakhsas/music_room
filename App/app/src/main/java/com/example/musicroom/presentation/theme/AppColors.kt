package com.example.musicroom.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Modern Brand Colors - Teal/Cyan/Blue Theme
val PrimaryTeal = Color(0xFF00BCD4)          // Vibrant cyan/teal
val DeepCyan = Color(0xFF0097A7)             // Deep cyan
val DarkTeal = Color(0xFF006064)             // Dark teal
val AccentCyan = Color(0xFF18FFFF)           // Bright neon cyan
val SoftBlue = Color(0xFF4DD0E1)             // Soft light blue

// Secondary accent colors
val CoralPink = Color(0xFFFF6B9D)            // Coral pink for contrast
val PurpleAccent = Color(0xFF9C27B0)         // Purple accent
val ElectricBlue = Color(0xFF2196F3)         // Electric blue

// UI Background Colors - Modern dark theme
val DarkBackground = Color(0xFF0A0E27)       // Deep navy blue background
val DarkSurface = Color(0xFF1A1F3A)          // Elevated surface color
val DarkCard = Color(0xFF242B4D)             // Card background
val DarkError = Color(0xFFCF6679)            // Error color

// Glass effect colors
val GlassWhite = Color(0x1AFFFFFF)           // Semi-transparent white for glass effect
val GlassCyan = Color(0x1A00BCD4)            // Semi-transparent cyan for glass effect

// Text Colors
val TextPrimary = Color.White
val TextSecondary = Color.White.copy(alpha = 0.7f)
val TextTertiary = Color.White.copy(alpha = 0.5f)

// Gradients - Modern and vibrant
val modernGradient = Brush.linearGradient(
    listOf(
        PrimaryTeal,
        DeepCyan,
        SoftBlue
    )
)

val primaryGradient = Brush.linearGradient(
    listOf(
        PrimaryTeal,
        DeepCyan
    )
)

val accentGradient = Brush.linearGradient(
    listOf(
        AccentCyan,
        PrimaryTeal
    )
)

val backgroundGradient = Brush.verticalGradient(
    listOf(
        DarkBackground,
        PrimaryTeal.copy(alpha = 0.15f),
        DeepCyan.copy(alpha = 0.1f),
        DarkBackground
    )
)

val cardGradient = Brush.linearGradient(
    listOf(
        DarkCard,
        DarkSurface
    )
)

val glassGradient = Brush.linearGradient(
    listOf(
        GlassWhite,
        GlassCyan
    )
)

// Dark Theme Color Scheme
val DarkPrimary = PrimaryTeal
val DarkSecondary = DeepCyan
val DarkOnPrimary = Color.White
val DarkOnSecondary = Color.White
val DarkOnBackground = Color.White
val DarkOnSurface = Color.White

// Light Theme Color Scheme (for future use)
val LightPrimary = PrimaryTeal
val LightSecondary = DeepCyan
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color.White
val LightError = Color(0xFFB00020)
val LightOnPrimary = Color.White
val LightOnSecondary = Color.White
val LightOnBackground = Color.Black
val LightOnSurface = Color.Black

