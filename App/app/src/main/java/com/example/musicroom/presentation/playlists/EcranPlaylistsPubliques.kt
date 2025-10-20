package com.example.musicroom.presentation.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.musicroom.data.service.PublicPlaylist
import com.example.musicroom.presentation.theme.*

/**
 * ========================================================================
 * ÉCRAN DES PLAYLISTS PUBLIQUES - Design Moderne
 * ========================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPlaylistsPubliques(
    controleurNavigation: NavController,
    modeleVue: PlaylistDetailsViewModel = hiltViewModel()
) {
    val etatUI by modeleVue.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        modeleVue.loadPublicPlaylists()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
    ) {
        // En-tête
        EnTetePlaylists(
            surRafraichir = { modeleVue.loadPublicPlaylists() }
        )
        
        // Contenu
        when (etatUI) {
            is PlaylistUiState.Loading -> {
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
                            "Chargement des playlists...",
                            color = TexteSecondaire,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            is PlaylistUiState.Success -> {
                val playlists = (etatUI as PlaylistUiState.Success).playlists
                
                if (playlists.isEmpty()) {
                    EtatVide()
                } else {
                    ListePlaylists(
                        playlists = playlists,
                        surClicPlaylist = { playlist ->
                            controleurNavigation.navigate("playlist_tracks/${playlist.id}")
                        }
                    )
                }
            }
            
            is PlaylistUiState.Error -> {
                EtatErreur(
                    message = (etatUI as PlaylistUiState.Error).message,
                    surReessayer = { modeleVue.loadPublicPlaylists() }
                )
            }
        }
    }
}

@Composable
private fun EnTetePlaylists(surRafraichir: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CouleurPrincipale.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = CouleurPrincipale,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = "Playlists Publiques",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextePrimaire
                    )
                    Text(
                        text = "Découvrez les playlists partagées",
                        fontSize = 14.sp,
                        color = TexteSecondaire
                    )
                }
            }
            
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = CouleurPrincipale.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(onClick = surRafraichir) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Rafraîchir",
                        tint = CouleurPrincipale
                    )
                }
            }
        }
    }
}

@Composable
private fun ListePlaylists(
    playlists: List<PublicPlaylist>,
    surClicPlaylist: (PublicPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists) { playlist ->
            CartePlaylist(
                playlist = playlist,
                surClic = { surClicPlaylist(playlist) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun CartePlaylist(
    playlist: PublicPlaylist,
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
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icône de playlist
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CouleurPrincipale.copy(alpha = 0.4f),
                                CouleurProfondeViolette.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TextePrimaire,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Informations
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = playlist.name,
                    color = TextePrimaire,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = CouleurPrincipale,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = playlist.user_name,
                        color = TexteSecondaire,
                        fontSize = 14.sp
                    )
                }
                
                if (playlist.isPublic) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CouleurAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Publique",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = CouleurAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EtatVide() {
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
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CouleurPrincipale.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = CouleurPrincipale,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Text(
                    text = "Aucune playlist publique",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextePrimaire,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Les playlists publiques apparaîtront ici",
                    fontSize = 14.sp,
                    color = TexteSecondaire,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EtatErreur(
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
                    text = "Erreur de chargement",
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
                    colors = ButtonDefaults.buttonColors(containerColor = CouleurPrincipale),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Réessayer")
                }
            }
        }
    }
}

