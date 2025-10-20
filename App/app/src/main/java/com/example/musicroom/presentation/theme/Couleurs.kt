package com.example.musicroom.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Palette de Couleurs Moderne - Thème Vibrant Bleu/Violet
val CouleurPrincipale = Color(0xFF6366F1)       // Bleu indigo vibrant
val CouleurProfondeViolette = Color(0xFF8B5CF6) // Violet profond
val CouleurFonceeViolette = Color(0xFF7C3AED)   // Violet foncé
val CouleurAccent = Color(0xFF06B6D4)           // Cyan moderne

// Couleurs UI Sombres
val FondSombre = Color(0xFF0F172A)              // Ardoise très sombre
val SurfaceSombre = Color(0xFF1E293B)           // Ardoise sombre
val ErreurSombre = Color(0xFFEF4444)            // Rouge moderne

// Couleurs de Texte
val TextePrimaire = Color(0xFFF8FAFC)           // Blanc cassé
val TexteSecondaire = Color(0xFF94A3B8)         // Ardoise gris

// Couleurs Supplémentaires pour UI Moderne
val CouleurSucces = Color(0xFF10B981)           // Vert émeraude
val CouleurAvertissement = Color(0xFFF59E0B)    // Ambre
val CouleurInfo = Color(0xFF3B82F6)             // Bleu

// Dégradés Modernes
val degradePrincipal = Brush.linearGradient(
    listOf(
        CouleurPrincipale,
        CouleurProfondeViolette
    )
)

// Dégradé pour l'écran d'intégration
public val degrade Intégration = Brush.verticalGradient(
    listOf(
        FondSombre,
        CouleurPrincipale.copy(alpha = 0.15f),
        CouleurProfondeViolette.copy(alpha = 0.1f),
        FondSombre
    )
)

// Dégradé pour l'inscription
val degradeInscription = Brush.verticalGradient(
    listOf(
        CouleurPrincipale,
        CouleurProfondeViolette
    )
)

// Couleurs Thème Sombre
val PrimaireSombre = CouleurPrincipale
val SecondaireSombre = CouleurProfondeViolette
val SurPrimaireSombre = Color.White
val SurSecondaireSombre = Color.White
val SurFondSombre = TextePrimaire
val SurSurfaceSombre = TextePrimaire

// Couleurs Thème Clair
val PrimaireClair = CouleurPrincipale
val SecondaireClair = CouleurProfondeViolette
val FondClair = Color(0xFFF8FAFC)
val SurfaceClair = Color.White
val ErreurClair = Color(0xFFDC2626)
val SurPrimaireClair = Color.White
val SurSecondaireClair = Color.White
val SurFondClair = Color(0xFF1E293B)
val SurSurfaceClair = Color(0xFF1E293B)

// Couleurs de bordure et de division
val CouleurBordure = Color(0xFF334155)
val CouleurDivision = Color(0xFF475569)

