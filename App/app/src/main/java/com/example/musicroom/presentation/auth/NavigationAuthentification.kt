package com.example.musicroom.presentation.auth

/**
 * ========================================================================
 * NAVIGATION AUTHENTIFICATION
 * ========================================================================
 * Définit les routes de navigation pour les écrans d'authentification
 */
sealed class NavigationAuthentification(val route: String) {
    object Connexion : NavigationAuthentification("connexion")
    object Inscription : NavigationAuthentification("inscription")
    object MotDePasseOublie : NavigationAuthentification("mot_de_passe_oublie")
}

