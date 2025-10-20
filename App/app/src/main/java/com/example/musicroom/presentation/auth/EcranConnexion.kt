package com.example.musicroom.presentation.auth

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicroom.components.GoogleButton
import com.example.musicroom.presentation.theme.*

/**
 * ========================================================================
 * ÉCRAN DE CONNEXION - Design Moderne
 * ========================================================================
 * Interface de connexion repensée avec une esthétique contemporaine
 * et une expérience utilisateur fluide
 */
@Composable
fun EcranConnexion(
    surSuccesConnexion: () -> Unit,
    surClicInscription: () -> Unit,
    surClicMotDePasseOublie: () -> Unit,
    modeleVue: AuthViewModel = hiltViewModel()
) {
    // Gestion des états
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var motDePasseVisible by remember { mutableStateOf(false) }
    
    val etatAuth by modeleVue.authState.collectAsState()
    
    // Gestion des effets secondaires
    LaunchedEffect(etatAuth) {
        when (etatAuth) {
            is AuthState.LoginSuccess -> {
                Log.d("EcranConnexion", "✅ Connexion réussie")
                surSuccesConnexion()
                modeleVue.clearState()
            }
            is AuthState.GoogleSignInSuccess -> {
                Log.d("EcranConnexion", "✅ Connexion Google réussie")
                surSuccesConnexion()
                modeleVue.clearState()
            }
            else -> {}
        }
    }
    
    // Interface utilisateur
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(degradeIntégration)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // En-tête moderne
            EnTeteConnexion()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Carte de formulaire
            CarteFormulaireConnexion(
                email = email,
                surChangementEmail = { email = it },
                motDePasse = motDePasse,
                surChangementMotDePasse = { motDePasse = it },
                motDePasseVisible = motDePasseVisible,
                surBasculementVisibilite = { motDePasseVisible = !motDePasseVisible },
                surConnexion = {
                    Log.d("EcranConnexion", "🔐 Tentative de connexion")
                    modeleVue.login(email.trim(), motDePasse)
                },
                surMotDePasseOublie = surClicMotDePasseOublie,
                surConnexionGoogle = {
                    Log.d("EcranConnexion", "🔗 Connexion Google")
                    modeleVue.signInWithGoogle("mock_google_id_token_${System.currentTimeMillis()}")
                },
                enChargement = etatAuth is AuthState.Loading,
                formulaireValide = email.isNotBlank() && motDePasse.isNotBlank()
            )
            
            // Message d'erreur animé
            AnimatedVisibility(
                visible = etatAuth is AuthState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CarteErreur(
                    message = (etatAuth as? AuthState.Error)?.message ?: "",
                    email = email
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Section d'inscription
            SectionInscription(surClicInscription = surClicInscription)
        }
    }
}

@Composable
private fun EnTeteConnexion() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icône moderne avec dégradé
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = CouleurPrincipale.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Logo",
                    modifier = Modifier.size(40.dp),
                    tint = CouleurPrincipale
                )
            }
        }
        
        Text(
            text = "SalleMusicale",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = TextePrimaire
        )
        
        Text(
            text = "Connectez-vous pour découvrir la musique",
            style = MaterialTheme.typography.bodyLarge,
            color = TexteSecondaire,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun CarteFormulaireConnexion(
    email: String,
    surChangementEmail: (String) -> Unit,
    motDePasse: String,
    surChangementMotDePasse: (String) -> Unit,
    motDePasseVisible: Boolean,
    surBasculementVisibilite: () -> Unit,
    surConnexion: () -> Unit,
    surMotDePasseOublie: () -> Unit,
    surConnexionGoogle: () -> Unit,
    enChargement: Boolean,
    formulaireValide: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceSombre.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Champ Email moderne
            OutlinedTextField(
                value = email,
                onValueChange = surChangementEmail,
                label = { Text("Adresse e-mail") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = null,
                        tint = CouleurPrincipale
                    ) 
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextePrimaire,
                    unfocusedTextColor = TextePrimaire,
                    cursorColor = CouleurPrincipale,
                    focusedBorderColor = CouleurPrincipale,
                    unfocusedBorderColor = CouleurBordure,
                    focusedLabelColor = CouleurPrincipale,
                    unfocusedLabelColor = TexteSecondaire
                )
            )
            
            // Champ Mot de passe moderne
            OutlinedTextField(
                value = motDePasse,
                onValueChange = surChangementMotDePasse,
                label = { Text("Mot de passe") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null,
                        tint = CouleurPrincipale
                    ) 
                },
                trailingIcon = {
                    IconButton(onClick = surBasculementVisibilite) {
                        Icon(
                            if (motDePasseVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (motDePasseVisible) "Masquer" else "Afficher",
                            tint = TexteSecondaire
                        )
                    }
                },
                visualTransformation = if (motDePasseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextePrimaire,
                    unfocusedTextColor = TextePrimaire,
                    cursorColor = CouleurPrincipale,
                    focusedBorderColor = CouleurPrincipale,
                    unfocusedBorderColor = CouleurBordure,
                    focusedLabelColor = CouleurPrincipale,
                    unfocusedLabelColor = TexteSecondaire
                )
            )
            
            // Lien mot de passe oublié
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = surMotDePasseOublie,
                    enabled = !enChargement
                ) {
                    Text(
                        "Mot de passe oublié ?",
                        color = CouleurPrincipale,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bouton de connexion moderne
            Button(
                onClick = surConnexion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !enChargement && formulaireValide,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CouleurPrincipale,
                    disabledContainerColor = CouleurPrincipale.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (enChargement) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Se connecter",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Diviseur "OU"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = CouleurBordure
                )
                Text(
                    text = "OU",
                    color = TexteSecondaire,
                    fontSize = 14.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = CouleurBordure
                )
            }
            
            // Bouton Google moderne
            GoogleButton(
                onClick = surConnexionGoogle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CarteErreur(message: String, email: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ErreurSombre.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErreurSombre)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ErreurSombre,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Erreur de connexion",
                    color = ErreurSombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = message,
                    color = TexteSecondaire,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SectionInscription(surClicInscription: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pas encore de compte ?",
            color = TexteSecondaire,
            fontSize = 14.sp
        )
        TextButton(onClick = surClicInscription) {
            Text(
                "S'inscrire",
                color = CouleurPrincipale,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

