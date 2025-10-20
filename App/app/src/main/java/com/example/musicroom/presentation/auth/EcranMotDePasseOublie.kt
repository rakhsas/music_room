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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicroom.presentation.theme.*

enum class EtapeReinitialisationMotDePasse {
    SAISIE_EMAIL,
    VERIFICATION_OTP,
    NOUVEAU_MOT_DE_PASSE
}

/**
 * ========================================================================
 * ÉCRAN MOT DE PASSE OUBLIÉ - Design Moderne
 * ========================================================================
 */
@Composable
fun EcranMotDePasseOublie(
    surRetourConnexion: () -> Unit,
    surReinitialisationComplete: () -> Unit,
    etapeInitiale: EtapeReinitialisationMotDePasse = EtapeReinitialisationMotDePasse.SAISIE_EMAIL,
    surChangementEtape: (EtapeReinitialisationMotDePasse) -> Unit = {},
    modeleVue: AuthViewModel = hiltViewModel()
) {
    var etapeActuelle by remember { mutableStateOf(etapeInitiale) }
    
    LaunchedEffect(etapeInitiale) {
        etapeActuelle = etapeInitiale
    }
    
    LaunchedEffect(etapeActuelle) {
        surChangementEtape(etapeActuelle)
    }
    
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var nouveauMotDePasse by remember { mutableStateOf("") }
    var confirmationMotDePasse by remember { mutableStateOf("") }
    var motDePasseVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    
    val etatAuth by modeleVue.authState.collectAsState()
    
    LaunchedEffect(etatAuth) {
        when (etatAuth) {
            is AuthState.PasswordResetOTPSent -> {
                etapeActuelle = EtapeReinitialisationMotDePasse.VERIFICATION_OTP
            }
            is AuthState.OTPVerified -> {
                etapeActuelle = EtapeReinitialisationMotDePasse.NOUVEAU_MOT_DE_PASSE
            }
            is AuthState.PasswordResetComplete -> {
                surReinitialisationComplete()
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
            
            // Contenu selon l'étape
            when (etapeActuelle) {
                EtapeReinitialisationMotDePasse.SAISIE_EMAIL -> {
                    EtapeSaisieEmail(
                        email = email,
                        surChangementEmail = { email = it },
                        surContinuer = { modeleVue.requestPasswordResetOTP(email) },
                        etatAuth = etatAuth
                    )
                }
                EtapeReinitialisationMotDePasse.VERIFICATION_OTP -> {
                    EtapeVerificationOTP(
                        email = email,
                        otp = otp,
                        surChangementOTP = { otp = it },
                        surVerifier = { modeleVue.verifyPasswordResetOTP(email, otp) },
                        surRenvoyerOTP = { modeleVue.requestPasswordResetOTP(email) },
                        etatAuth = etatAuth
                    )
                }
                EtapeReinitialisationMotDePasse.NOUVEAU_MOT_DE_PASSE -> {
                    EtapeNouveauMotDePasse(
                        nouveauMotDePasse = nouveauMotDePasse,
                        confirmationMotDePasse = confirmationMotDePasse,
                        motDePasseVisible = motDePasseVisible,
                        confirmationVisible = confirmationVisible,
                        surChangementNouveauMotDePasse = { nouveauMotDePasse = it },
                        surChangementConfirmation = { confirmationMotDePasse = it },
                        surBasculementVisibilite = { motDePasseVisible = !motDePasseVisible },
                        surBasculementVisibiliteConfirmation = { confirmationVisible = !confirmationVisible },
                        surReinitialiser = {
                            modeleVue.resetPasswordWithOTP(email, otp, nouveauMotDePasse, confirmationMotDePasse)
                        },
                        etatAuth = etatAuth
                    )
                }
            }
        }
    }
}

@Composable
private fun EtapeSaisieEmail(
    email: String,
    surChangementEmail: (String) -> Unit,
    surContinuer: () -> Unit,
    etatAuth: AuthState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icône
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = CouleurPrincipale.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = "Réinitialiser",
                    modifier = Modifier.size(40.dp),
                    tint = CouleurPrincipale
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Mot de passe oublié ?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextePrimaire,
            fontSize = 28.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Saisissez votre e-mail et nous vous enverrons un code à 6 chiffres pour réinitialiser votre mot de passe",
            style = MaterialTheme.typography.bodyLarge,
            color = TexteSecondaire,
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                Button(
                    onClick = surContinuer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = email.isNotBlank() && etatAuth !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CouleurPrincipale
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (etatAuth is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Envoyer le code",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = etatAuth is AuthState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CarteMessageErreur((etatAuth as? AuthState.Error)?.message ?: "")
        }
    }
}

@Composable
private fun EtapeVerificationOTP(
    email: String,
    otp: String,
    surChangementOTP: (String) -> Unit,
    surVerifier: () -> Unit,
    surRenvoyerOTP: () -> Unit,
    etatAuth: AuthState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = CouleurPrincipale.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = "Code",
                    modifier = Modifier.size(40.dp),
                    tint = CouleurPrincipale
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Entrez le code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextePrimaire,
            fontSize = 28.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Nous avons envoyé un code à 6 chiffres à",
            style = MaterialTheme.typography.bodyLarge,
            color = TexteSecondaire,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = CouleurPrincipale,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) surChangementOTP(it) },
                    label = { Text("Code à 6 chiffres") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Pin,
                            contentDescription = null,
                            tint = CouleurPrincipale
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                Button(
                    onClick = surVerifier,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = otp.length == 6 && etatAuth !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CouleurPrincipale
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (etatAuth is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Vérifier le code",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                TextButton(
                    onClick = surRenvoyerOTP,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = etatAuth !is AuthState.Loading
                ) {
                    Text(
                        "Renvoyer le code",
                        color = CouleurPrincipale,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = etatAuth is AuthState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CarteMessageErreur((etatAuth as? AuthState.Error)?.message ?: "")
        }
    }
}

@Composable
private fun EtapeNouveauMotDePasse(
    nouveauMotDePasse: String,
    confirmationMotDePasse: String,
    motDePasseVisible: Boolean,
    confirmationVisible: Boolean,
    surChangementNouveauMotDePasse: (String) -> Unit,
    surChangementConfirmation: (String) -> Unit,
    surBasculementVisibilite: () -> Unit,
    surBasculementVisibiliteConfirmation: () -> Unit,
    surReinitialiser: () -> Unit,
    etatAuth: AuthState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = CouleurSucces.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Succès",
                    modifier = Modifier.size(40.dp),
                    tint = CouleurSucces
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Nouveau mot de passe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextePrimaire,
            fontSize = 28.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Créez un mot de passe sécurisé",
            style = MaterialTheme.typography.bodyLarge,
            color = TexteSecondaire,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                OutlinedTextField(
                    value = nouveauMotDePasse,
                    onValueChange = surChangementNouveauMotDePasse,
                    label = { Text("Nouveau mot de passe") },
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
                                contentDescription = null,
                                tint = TexteSecondaire
                            )
                        }
                    },
                    visualTransformation = if (motDePasseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                OutlinedTextField(
                    value = confirmationMotDePasse,
                    onValueChange = surChangementConfirmation,
                    label = { Text("Confirmer le mot de passe") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = CouleurPrincipale
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = surBasculementVisibiliteConfirmation) {
                            Icon(
                                if (confirmationVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TexteSecondaire
                            )
                        }
                    },
                    visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                // Messages de validation
                if (nouveauMotDePasse.isNotBlank() && nouveauMotDePasse.length < 8) {
                    Text(
                        text = "Le mot de passe doit contenir au moins 8 caractères",
                        color = ErreurSombre,
                        fontSize = 12.sp
                    )
                }
                
                if (confirmationMotDePasse.isNotBlank() && nouveauMotDePasse != confirmationMotDePasse) {
                    Text(
                        text = "Les mots de passe ne correspondent pas",
                        color = ErreurSombre,
                        fontSize = 12.sp
                    )
                }
                
                Button(
                    onClick = surReinitialiser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = nouveauMotDePasse.isNotBlank() &&
                            confirmationMotDePasse.isNotBlank() &&
                            nouveauMotDePasse == confirmationMotDePasse &&
                            nouveauMotDePasse.length >= 8 &&
                            etatAuth !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CouleurPrincipale
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (etatAuth is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Réinitialiser le mot de passe",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = etatAuth is AuthState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CarteMessageErreur((etatAuth as? AuthState.Error)?.message ?: "")
        }
    }
}

@Composable
private fun CarteMessageErreur(message: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ErreurSombre.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErreurSombre)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ErreurSombre
            )
            Text(
                text = message,
                color = TexteSecondaire,
                fontSize = 14.sp
            )
        }
    }
}

