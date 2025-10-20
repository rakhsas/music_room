package com.example.musicroom.presentation.auth

import android.util.Log
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

sealed class EtatEcranAuth {
    object Connexion : EtatEcranAuth()
    object Inscription : EtatEcranAuth()
    object MotDePasseOublie : EtatEcranAuth()
}

/**
 * ========================================================================
 * CONTENEUR D'AUTHENTIFICATION
 * ========================================================================
 * Gère la navigation entre les différents écrans d'authentification
 */
@Composable
fun ConteneurAuthentification(surSuccesConnexion: () -> Unit) {
    val modeleVueAuth: AuthViewModel = hiltViewModel()
    val etatAuth by modeleVueAuth.authState.collectAsState()
    var ecranActuel by remember { mutableStateOf<EtatEcranAuth>(EtatEcranAuth.Connexion) }
    var etapeMotDePasseOublie by remember { mutableStateOf(EtapeReinitialisationMotDePasse.SAISIE_EMAIL) }
    
    // Gestion de l'authentification réussie
    LaunchedEffect(etatAuth) {
        when (etatAuth) {
            is AuthState.LoginSuccess -> {
                Log.d("ConteneurAuth", "✅ Connexion réussie - Navigation vers l'accueil")
                surSuccesConnexion()
                modeleVueAuth.clearState()
            }
            is AuthState.SignUpSuccess -> {
                Log.d("ConteneurAuth", "✅ Inscription réussie - Retour à la connexion")
                ecranActuel = EtatEcranAuth.Connexion
                modeleVueAuth.clearState()
            }
            is AuthState.GoogleSignInSuccess -> {
                Log.d("ConteneurAuth", "✅ Connexion Google réussie - Navigation vers l'accueil")
                surSuccesConnexion()
                modeleVueAuth.clearState()
            }
            else -> {}
        }
    }
    
    // Réinitialiser l'étape du mot de passe oublié lors du changement d'écran
    LaunchedEffect(ecranActuel) {
        if (ecranActuel == EtatEcranAuth.MotDePasseOublie) {
            etapeMotDePasseOublie = EtapeReinitialisationMotDePasse.SAISIE_EMAIL
            Log.d("ConteneurAuth", "Réinitialisation de l'étape du mot de passe oublié")
        }
    }
    
    // Navigation entre les écrans
    when (ecranActuel) {
        EtatEcranAuth.Connexion -> {
            EcranConnexion(
                surSuccesConnexion = surSuccesConnexion,
                surClicInscription = {
                    ecranActuel = EtatEcranAuth.Inscription
                },
                surClicMotDePasseOublie = {
                    ecranActuel = EtatEcranAuth.MotDePasseOublie
                },
                modeleVue = modeleVueAuth
            )
        }
        
        EtatEcranAuth.Inscription -> {
            EcranInscription(
                surRetourConnexion = {
                    ecranActuel = EtatEcranAuth.Connexion
                },
                modeleVue = modeleVueAuth
            )
        }
        
        EtatEcranAuth.MotDePasseOublie -> {
            EcranMotDePasseOublie(
                surRetourConnexion = {
                    ecranActuel = EtatEcranAuth.Connexion
                },
                surReinitialisationComplete = {
                    ecranActuel = EtatEcranAuth.Connexion
                },
                etapeInitiale = etapeMotDePasseOublie,
                surChangementEtape = { nouvelleEtape ->
                    etapeMotDePasseOublie = nouvelleEtape
                },
                modeleVue = modeleVueAuth
            )
        }
    }
}

