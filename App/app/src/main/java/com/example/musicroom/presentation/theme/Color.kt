package com.example.musicroom.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Brand Colors - Orange/Yellow Theme (same names, different colors)
val PrimaryPurple = Color(0xFFFF9800)      // Vibrant orange instead of purple
val DeepPurple = Color(0xFFFF5722)         // Deep orange instead of deep purple
val DarkPurple = Color(0xFFE65100)         // Dark orange instead of dark purple
val AccentPurple = Color(0xFFFFEB3B)       // Bright yellow instead of accent purple

// UI Colors (unchanged)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1D1D1D)
val DarkError = Color(0xFFCF6679)

// Text Colors (unchanged)
val TextPrimary = Color.White
val TextSecondary = Color.White.copy(alpha = 0.7f)

// Gradient Colors - Orange/Yellow Theme (same names, different colors)
val purpleGradient = Brush.linearGradient(
    listOf(
        PrimaryPurple,  // Now orange
        DeepPurple      // Now deep orange
    )
)

// Make gradient public
public val onboardingGradient = Brush.verticalGradient(
    listOf(
        DarkBackground,
        PrimaryPurple.copy(alpha = 0.2f),  // Now orange
        DeepPurple.copy(alpha = 0.1f),     // Now deep orange
        DarkBackground
    )
)

// Add to existing gradients
val signUpGradient = Brush.verticalGradient(
    listOf(
        PrimaryPurple,  // Now orange
        DeepPurple      // Now deep orange
    )
)

// Dark Theme Colors (same names, different colors)
val DarkPrimary = PrimaryPurple        // Now orange
val DarkSecondary = DeepPurple         // Now deep orange
val DarkOnPrimary = Color.Black        // Better contrast with orange
val DarkOnSecondary = Color.Black      // Better contrast with orange
val DarkOnBackground = Color.White
val DarkOnSurface = Color.White

// Light Theme Colors (same names, different colors)
val LightPrimary = PrimaryPurple       // Now orange
val LightSecondary = DeepPurple        // Now deep orange
val LightBackground = Color.White
val LightSurface = Color.White
val LightError = Color(0xFFB00020)
val LightOnPrimary = Color.White
val LightOnSecondary = Color.Black
val LightOnBackground = Color.Black
val LightOnSurface = Color.Black