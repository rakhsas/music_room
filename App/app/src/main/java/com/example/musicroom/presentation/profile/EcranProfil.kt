package com.example.musicroom.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicroom.data.models.*
import com.example.musicroom.presentation.theme.*

enum class SectionProfil {
    INFORMATIONS_PUBLIQUES,
    INFORMATIONS_CONTACT,
    PREFERENCES_MUSICALES
}

/**
 * ========================================================================
 * ÉCRAN DE PROFIL - Design Moderne
 * ========================================================================
 * Profil utilisateur avec gestion des préférences et paramètres
 */
@Composable
fun EcranProfil(
    utilisateur: User,
    surNavigationVersConnexion: () -> Unit = {},
    modeleVue: ProfileViewModel = hiltViewModel()
) {
    val etatUI by modeleVue.uiState.collectAsStateWithLifecycle()
    var sectionSelectionnee by remember { mutableStateOf<SectionProfil?>(null) }
    var afficherDialogueDeconnexion by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        modeleVue.loadUserProfile()
    }
    
    // Gérer la déconnexion réussie
    LaunchedEffect(etatUI.logoutSuccess) {
        if (etatUI.logoutSuccess) {
            surNavigationVersConnexion()
            modeleVue.clearLogoutSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
    ) {
        when {
            etatUI.isLoading -> {
                EcranChargementProfil()
            }
            
            etatUI.error != null -> {
                EcranErreurProfil(
                    erreur = etatUI.error ?: "",
                    surReessayer = { modeleVue.loadUserProfile() }
                )
            }
            
            etatUI.userProfile != null -> {
                ContenuProfil(
                    profilUtilisateur = etatUI.userProfile!!,
                    enMiseAJour = etatUI.isUpdating,
                    enDeconnexion = etatUI.isLoggingOut,
                    sectionSelectionnee = sectionSelectionnee,
                    surSelectionSection = { sectionSelectionnee = it },
                    surClicDeconnexion = { afficherDialogueDeconnexion = true },
                    surMiseAJourProfil = { nom, bio, dateNaissance, numeroTel, prefsMusicales, artistesAimes, albumsAimes, chansonsAimees, genres ->
                        modeleVue.updateProfile(
                            name = nom,
                            bio = bio,
                            dateOfBirth = dateNaissance,
                            phoneNumber = numeroTel,
                            profilePrivacy = null,
                            emailPrivacy = null,
                            phonePrivacy = null,
                            musicPreferences = prefsMusicales,
                            likedArtists = artistesAimes,
                            likedAlbums = albumsAimes,
                            likedSongs = chansonsAimees,
                            genres = genres
                        )
                    }
                )
            }
        }
    }
    
    // Dialogue de confirmation de déconnexion
    if (afficherDialogueDeconnexion) {
        DialogueConfirmationDeconnexion(
            surConfirmer = {
                modeleVue.logout()
                afficherDialogueDeconnexion = false
            },
            surAnnuler = { afficherDialogueDeconnexion = false },
            enDeconnexion = etatUI.isLoggingOut
        )
    }
    
    // Afficher les erreurs
    etatUI.error?.let { erreur ->
        LaunchedEffect(erreur) {
            modeleVue.clearError()
        }
    }
}

@Composable
private fun EcranChargementProfil() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = CouleurPrincipale,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Chargement du profil...",
                color = TexteSecondaire,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EcranErreurProfil(
    erreur: String,
    surReessayer: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = ErreurSombre,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Erreur de chargement",
                    color = TextePrimaire,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = erreur,
                    color = TexteSecondaire,
                    fontSize = 16.sp
                )
                Button(
                    onClick = surReessayer,
                    colors = ButtonDefaults.buttonColors(containerColor = CouleurPrincipale),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Réessayer")
                }
            }
        }
    }
}

@Composable
private fun ContenuProfil(
    profilUtilisateur: UserProfile,
    enMiseAJour: Boolean,
    enDeconnexion: Boolean,
    sectionSelectionnee: SectionProfil?,
    surSelectionSection: (SectionProfil?) -> Unit,
    surClicDeconnexion: () -> Unit,
    surMiseAJourProfil: (
        nom: String?,
        bio: String?,
        dateNaissance: String?,
        numeroTel: String?,
        prefsMusicales: List<String>?,
        artistesAimes: List<String>?,
        albumsAimes: List<String>?,
        chansonsAimees: List<String>?,
        genres: List<String>?,
    ) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // En-tête du profil
        EnTeteProfil(profilUtilisateur)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Sections du profil
        CarteSectionProfil(
            titre = "Informations Publiques",
            description = "Nom et biographie",
            icone = Icons.Default.Person,
            surClic = { surSelectionSection(SectionProfil.INFORMATIONS_PUBLIQUES) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CarteSectionProfil(
            titre = "Informations de Contact",
            description = "Téléphone et date de naissance",
            icone = Icons.Default.ContactMail,
            surClic = { surSelectionSection(SectionProfil.INFORMATIONS_CONTACT) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CarteSectionProfil(
            titre = "Préférences Musicales",
            description = "Artistes et genres favoris",
            icone = Icons.Default.MusicNote,
            surClic = { surSelectionSection(SectionProfil.PREFERENCES_MUSICALES) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Bouton de déconnexion
        BoutonDeconnexion(
            surClic = surClicDeconnexion,
            enChargement = enDeconnexion
        )
        
        Spacer(modifier = Modifier.height(80.dp))
    }
    
    // Dialogues de modification
    when (sectionSelectionnee) {
        SectionProfil.INFORMATIONS_PUBLIQUES -> {
            DialogueInformationsPubliques(
                profilUtilisateur = profilUtilisateur,
                surAnnuler = { surSelectionSection(null) },
                surSauvegarder = { nom, bio ->
                    surMiseAJourProfil(nom, bio, null, null, null, null, null, null, null)
                    surSelectionSection(null)
                },
                enChargement = enMiseAJour
            )
        }
        SectionProfil.INFORMATIONS_CONTACT -> {
            DialogueInformationsContact(
                profilUtilisateur = profilUtilisateur,
                surAnnuler = { surSelectionSection(null) },
                surSauvegarder = { dateNaissance, numeroTel ->
                    surMiseAJourProfil(null, null, dateNaissance, numeroTel, null, null, null, null, null)
                    surSelectionSection(null)
                },
                enChargement = enMiseAJour
            )
        }
        SectionProfil.PREFERENCES_MUSICALES -> {
            MusicPreferencesDialog(
                userProfile = profilUtilisateur,
                onDismiss = { surSelectionSection(null) },
                onSave = { prefsMusicales, artistesAimes, albumsAimes, chansonsAimees, genres ->
                    surMiseAJourProfil(null, null, null, null, prefsMusicales, artistesAimes, albumsAimes, chansonsAimees, genres)
                    surSelectionSection(null)
                },
                isLoading = enMiseAJour
            )
        }
        null -> {}
    }
}

@Composable
private fun EnTeteProfil(profilUtilisateur: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CouleurPrincipale.copy(alpha = 0.3f),
                            SurfaceSombre
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo de profil
                Box {
                    AsyncImage(
                        model = if (profilUtilisateur.avatar.isNotBlank() && profilUtilisateur.avatar != "default_avatar.png") {
                            "https://your-api-url.com/media/${profilUtilisateur.avatar}"
                        } else {
                            null
                        },
                        contentDescription = "Photo de profil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(CouleurPrincipale.copy(alpha = 0.3f))
                            .border(4.dp, CouleurPrincipale, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Badge Premium
                    if (profilUtilisateur.isPremium) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp),
                            shape = CircleShape,
                            color = CouleurPrincipale
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Premium",
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(6.dp),
                                tint = TextePrimaire
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nom
                Text(
                    text = profilUtilisateur.name.ifBlank { "Utilisateur Inconnu" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextePrimaire,
                    fontSize = 26.sp
                )
                
                // Email
                Text(
                    text = profilUtilisateur.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TexteSecondaire,
                    fontSize = 16.sp
                )
                
                // Bio
                if (profilUtilisateur.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profilUtilisateur.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextePrimaire,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CarteSectionProfil(
    titre: String,
    description: String,
    icone: ImageVector,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { surClic() },
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CouleurPrincipale.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icone,
                            contentDescription = null,
                            tint = CouleurPrincipale,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = titre,
                        color = TextePrimaire,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        color = TexteSecondaire,
                        fontSize = 14.sp
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TexteSecondaire,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BoutonDeconnexion(
    surClic: () -> Unit,
    enChargement: Boolean
) {
    Button(
        onClick = surClic,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !enChargement,
        colors = ButtonDefaults.buttonColors(
            containerColor = ErreurSombre.copy(alpha = 0.2f),
            contentColor = ErreurSombre
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ErreurSombre)
    ) {
        if (enChargement) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = ErreurSombre,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se déconnecter", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DialogueConfirmationDeconnexion(
    surConfirmer: () -> Unit,
    surAnnuler: () -> Unit,
    enDeconnexion: Boolean
) {
    LogoutConfirmationDialog(
        onConfirm = surConfirmer,
        onDismiss = surAnnuler,
        isLoggingOut = enDeconnexion
    )
}

@Composable
private fun DialogueInformationsPubliques(
    profilUtilisateur: UserProfile,
    surAnnuler: () -> Unit,
    surSauvegarder: (String, String) -> Unit,
    enChargement: Boolean
) {
    var nom by remember { mutableStateOf(profilUtilisateur.name) }
    var bio by remember { mutableStateOf(profilUtilisateur.bio) }
    
    AlertDialog(
        onDismissRequest = surAnnuler,
        title = { Text("Informations Publiques", color = TextePrimaire) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextePrimaire,
                        unfocusedTextColor = TextePrimaire,
                        cursorColor = CouleurPrincipale,
                        focusedBorderColor = CouleurPrincipale,
                        unfocusedBorderColor = CouleurBordure
                    )
                )
                
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biographie") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextePrimaire,
                        unfocusedTextColor = TextePrimaire,
                        cursorColor = CouleurPrincipale,
                        focusedBorderColor = CouleurPrincipale,
                        unfocusedBorderColor = CouleurBordure
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { surSauvegarder(nom, bio) },
                enabled = !enChargement,
                colors = ButtonDefaults.buttonColors(containerColor = CouleurPrincipale)
            ) {
                if (enChargement) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sauvegarder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = surAnnuler, enabled = !enChargement) {
                Text("Annuler", color = TexteSecondaire)
            }
        },
        containerColor = SurfaceSombre
    )
}

@Composable
private fun DialogueInformationsContact(
    profilUtilisateur: UserProfile,
    surAnnuler: () -> Unit,
    surSauvegarder: (String, String) -> Unit,
    enChargement: Boolean
) {
    var dateNaissance by remember { mutableStateOf(profilUtilisateur.dateOfBirth ?: "") }
    var numeroTel by remember { mutableStateOf(profilUtilisateur.phoneNumber ?: "") }
    
    AlertDialog(
        onDismissRequest = surAnnuler,
        title = { Text("Informations de Contact", color = TextePrimaire) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = dateNaissance,
                    onValueChange = { dateNaissance = it },
                    label = { Text("Date de naissance (AAAA-MM-JJ)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextePrimaire,
                        unfocusedTextColor = TextePrimaire,
                        cursorColor = CouleurPrincipale,
                        focusedBorderColor = CouleurPrincipale,
                        unfocusedBorderColor = CouleurBordure
                    )
                )
                
                OutlinedTextField(
                    value = numeroTel,
                    onValueChange = { numeroTel = it },
                    label = { Text("Numéro de téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextePrimaire,
                        unfocusedTextColor = TextePrimaire,
                        cursorColor = CouleurPrincipale,
                        focusedBorderColor = CouleurPrincipale,
                        unfocusedBorderColor = CouleurBordure
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { surSauvegarder(dateNaissance, numeroTel) },
                enabled = !enChargement,
                colors = ButtonDefaults.buttonColors(containerColor = CouleurPrincipale)
            ) {
                if (enChargement) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sauvegarder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = surAnnuler, enabled = !enChargement) {
                Text("Annuler", color = TexteSecondaire)
            }
        },
        containerColor = SurfaceSombre
    )
}

