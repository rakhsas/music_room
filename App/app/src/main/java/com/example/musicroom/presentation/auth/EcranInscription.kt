package com.example.musicroom.presentation.auth

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.musicroom.presentation.theme.*

/**
 * ========================================================================
 * ÉCRAN D'INSCRIPTION - Design Moderne
 * ========================================================================
 */
@Composable
fun EcranInscription(
    surRetourConnexion: () -> Unit,
    modeleVue: AuthViewModel = hiltViewModel()
) {
    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var motDePasseVisible by remember { mutableStateOf(false) }
    var erreurNom by remember { mutableStateOf("") }
    var erreurEmail by remember { mutableStateOf("") }
    var erreurMotDePasse by remember { mutableStateOf("") }
    
    val etatAuth by modeleVue.authState.collectAsState()
    
    LaunchedEffect(etatAuth) {
        when (etatAuth) {
            is AuthState.SignUpSuccess -> {
                Log.d("EcranInscription", "✅ Inscription réussie")
                // Le conteneur d'auth gère la navigation
            }
            else -> {}
        }
    }
    
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
            // Bouton retour
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = surRetourConnexion) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = TextePrimaire
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // En-tête
            EnTeteInscription()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Formulaire d'inscription
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceSombre.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Champ Nom
                    ChampTextePersonnalise(
                        valeur = nom,
                        surChangement = {
                            nom = it
                            erreurNom = ""
                        },
                        etiquette = "Nom complet",
                        icone = Icons.Default.Person,
                        messageErreur = erreurNom
                    )
                    
                    // Champ Email
                    ChampTextePersonnalise(
                        valeur = email,
                        surChangement = {
                            email = it
                            erreurEmail = ""
                        },
                        etiquette = "Adresse e-mail",
                        icone = Icons.Default.Email,
                        typeClavier = KeyboardType.Email,
                        messageErreur = erreurEmail
                    )
                    
                    // Champ Mot de passe
                    ChampTextePersonnalise(
                        valeur = motDePasse,
                        surChangement = {
                            motDePasse = it
                            erreurMotDePasse = ""
                        },
                        etiquette = "Mot de passe",
                        icone = Icons.Default.Lock,
                        typeClavier = KeyboardType.Password,
                        transformationVisuelle = if (motDePasseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        messageErreur = erreurMotDePasse,
                        iconeDroite = {
                            IconButton(onClick = { motDePasseVisible = !motDePasseVisible }) {
                                Icon(
                                    if (motDePasseVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (motDePasseVisible) "Masquer" else "Afficher",
                                    tint = TexteSecondaire
                                )
                            }
                        }
                    )
                    
                    // Indicateur de force du mot de passe
                    IndicateurForceMotDePasse(motDePasse)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bouton d'inscription
                    Button(
                        onClick = {
                            // Validation
                            erreurNom = ""
                            erreurEmail = ""
                            erreurMotDePasse = ""
                            
                            var estValide = true
                            
                            if (nom.isBlank()) {
                                erreurNom = "Le nom est requis"
                                estValide = false
                            }
                            
                            if (email.isBlank()) {
                                erreurEmail = "L'email est requis"
                                estValide = false
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                erreurEmail = "Veuillez entrer un email valide"
                                estValide = false
                            }
                            
                            if (motDePasse.isBlank()) {
                                erreurMotDePasse = "Le mot de passe est requis"
                                estValide = false
                            } else if (motDePasse.length < 6) {
                                erreurMotDePasse = "Le mot de passe doit contenir au moins 6 caractères"
                                estValide = false
                            }
                            
                            if (estValide) {
                                Log.d("EcranInscription", "Inscription pour : $email")
                                modeleVue.signUp(email, motDePasse, nom)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = etatAuth !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CouleurPrincipale
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        if (etatAuth is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Créer un compte",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // Message d'erreur
            AnimatedVisibility(
                visible = etatAuth is AuthState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CarteErreurInscription(
                    message = (etatAuth as? AuthState.Error)?.message ?: "",
                    email = email
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lien retour connexion
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Vous avez déjà un compte ?",
                    color = TexteSecondaire,
                    fontSize = 14.sp
                )
                TextButton(onClick = surRetourConnexion) {
                    Text(
                        "Se connecter",
                        color = CouleurPrincipale,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EnTeteInscription() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Rejoignez SalleMusicale",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = TextePrimaire
        )
        
        Text(
            text = "Créez votre compte pour commencer",
            style = MaterialTheme.typography.bodyLarge,
            color = TexteSecondaire,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun IndicateurForceMotDePasse(motDePasse: String) {
    val force = when {
        motDePasse.length > 8 && motDePasse.any { it.isDigit() } && 
        motDePasse.any { it.isUpperCase() } -> 3
        motDePasse.length > 6 -> 2
        motDePasse.isNotEmpty() -> 1
        else -> 0
    }
    
    val couleurForce = when (force) {
        3 -> CouleurSucces
        2 -> CouleurAvertissement
        1 -> ErreurSombre
        else -> CouleurBordure
    }
    
    val texteForce = when (force) {
        3 -> "Fort"
        2 -> "Moyen"
        1 -> "Faible"
        else -> ""
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index < force) couleurForce else CouleurBordure,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        
        if (motDePasse.isNotEmpty()) {
            Text(
                text = "Force du mot de passe : $texteForce",
                color = couleurForce,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ChampTextePersonnalise(
    valeur: String,
    surChangement: (String) -> Unit,
    etiquette: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    typeClavier: KeyboardType = KeyboardType.Text,
    transformationVisuelle: VisualTransformation = VisualTransformation.None,
    iconeDroite: @Composable (() -> Unit)? = null,
    messageErreur: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = valeur,
            onValueChange = surChangement,
            label = { Text(etiquette) },
            leadingIcon = { 
                Icon(
                    icone, 
                    contentDescription = null,
                    tint = CouleurPrincipale
                ) 
            },
            trailingIcon = iconeDroite,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = typeClavier,
                imeAction = ImeAction.Next
            ),
            visualTransformation = transformationVisuelle,
            modifier = Modifier.fillMaxWidth(),
            isError = messageErreur.isNotEmpty(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextePrimaire,
                unfocusedTextColor = TextePrimaire,
                cursorColor = CouleurPrincipale,
                focusedBorderColor = if (messageErreur.isEmpty()) CouleurPrincipale else ErreurSombre,
                unfocusedBorderColor = if (messageErreur.isEmpty()) CouleurBordure else ErreurSombre,
                errorBorderColor = ErreurSombre,
                focusedLabelColor = CouleurPrincipale,
                unfocusedLabelColor = TexteSecondaire
            )
        )
        
        if (messageErreur.isNotEmpty()) {
            Text(
                text = messageErreur,
                color = ErreurSombre,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun CarteErreurInscription(message: String, email: String) {
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
                    text = "Erreur d'inscription",
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

