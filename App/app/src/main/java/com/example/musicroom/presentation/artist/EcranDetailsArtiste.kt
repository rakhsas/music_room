package com.example.musicroom.presentation.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicroom.data.models.Song
import com.example.musicroom.presentation.theme.*

/**
 * ========================================================================
 * ÉCRAN DES DÉTAILS D'ARTISTE - Design Moderne
 * ========================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranDetailsArtiste(
    idArtiste: String,
    controleurNavigation: NavController,
    modeleVue: ArtistDetailsViewModel = hiltViewModel()
) {
    val etatUI by modeleVue.uiState.collectAsState()
    
    LaunchedEffect(idArtiste) {
        modeleVue.loadArtistDetails(idArtiste)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
    ) {
        when (etatUI) {
            is ArtistDetailsUiState.Loading -> {
                EcranChargement()
            }
            
            is ArtistDetailsUiState.Success -> {
                val artiste = (etatUI as ArtistDetailsUiState.Success).artist
                val chansons = (etatUI as ArtistDetailsUiState.Success).songs
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // En-tête avec photo
                    EnTeteArtiste(
                        nomArtiste = artiste.name,
                        imageUrl = artiste.image,
                        surRetour = { controleurNavigation.popBackStack() }
                    )
                    
                    // Liste des chansons
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Chansons Populaires",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextePrimaire
                        )
                        
                        chansons.forEach { chanson ->
                            CarteChanson(
                                chanson = chanson,
                                surClic = {
                                    // Navigation vers le lecteur
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
            
            is ArtistDetailsUiState.Error -> {
                EcranErreur(
                    message = (etatUI as ArtistDetailsUiState.Error).message,
                    surReessayer = { modeleVue.loadArtistDetails(idArtiste) }
                )
            }
        }
    }
}

@Composable
private fun EcranChargement() {
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
                "Chargement...",
                color = TexteSecondaire,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EnTeteArtiste(
    nomArtiste: String,
    imageUrl: String,
    surRetour: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Image de fond avec gradient
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = nomArtiste,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                FondSombre
                            )
                        )
                    )
            )
        }
        
        // Bouton retour
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .size(48.dp)
                .align(Alignment.TopStart),
            shape = CircleShape,
            color = SurfaceSombre.copy(alpha = 0.7f)
        ) {
            IconButton(onClick = surRetour) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = TextePrimaire
                )
            }
        }
        
        // Nom de l'artiste
        Text(
            text = nomArtiste,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextePrimaire
        )
    }
}

@Composable
private fun CarteChanson(
    chanson: Song,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône de lecture
            Surface(
                shape = CircleShape,
                color = CouleurPrincipale.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Jouer",
                        tint = CouleurPrincipale
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chanson.name,
                    color = TextePrimaire,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = chanson.album_name,
                    color = TexteSecondaire,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun EcranErreur(
    message: String,
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
                    text = "Erreur",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextePrimaire
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TexteSecondaire,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = surReessayer,
                    colors = ButtonDefaults.buttonColors(containerColor = CouleurPrincipale)
                ) {
                    Text("Réessayer")
                }
            }
        }
    }
}

