package com.example.musicroom.presentation.music

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicroom.data.models.Track
import com.example.musicroom.presentation.theme.*

private val categoriesMusique = listOf(
    "Populaire", "Aléatoire", "Jazz", "Électronique", "Rock", "Classique", "Ambiant", "Hip Hop"
)

/**
 * ========================================================================
 * ÉCRAN DE RECHERCHE MUSICALE - Design Moderne
 * ========================================================================
 * Interface de recherche avec catégories et résultats en temps réel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranRechercheMusique(
    controleurNavigation: NavController,
    modeleVue: MusicSearchViewModel = hiltViewModel()
) {
    var requeteRecherche by remember { mutableStateOf("") }
    val etatUI by modeleVue.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // En-tête de recherche
            EnTeteRecherche()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Barre de recherche moderne
            BarreRecherche(
                requete = requeteRecherche,
                surChangementRequete = { requeteRecherche = it },
                surRecherche = { modeleVue.searchTracks(requeteRecherche.trim()) }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Catégories de musique
            CategoriesMusique(
                categories = categoriesMusique,
                surSelectionCategorie = { categorie ->
                    requeteRecherche = categorie
                    modeleVue.searchTracks(categorie)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Contenu selon l'état
            when (etatUI) {
                is MusicSearchUiState.Loading -> {
                    EtatChargement()
                }
                
                is MusicSearchUiState.Success -> {
                    val etatSucces = etatUI as MusicSearchUiState.Success
                    ListeResultats(
                        pistes = etatSucces.tracks,
                        surClicPiste = { piste ->
                            naviguerVersPiste(controleurNavigation, piste)
                        }
                    )
                }
                
                is MusicSearchUiState.Error -> {
                    val etatErreur = etatUI as MusicSearchUiState.Error
                    EtatErreur(
                        message = etatErreur.message,
                        surReessayer = { modeleVue.searchTracks(requeteRecherche) }
                    )
                }
                
                is MusicSearchUiState.Empty -> {
                    EtatVide()
                }
            }
        }
    }
}

@Composable
private fun EnTeteRecherche() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = CouleurPrincipale.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = CouleurPrincipale,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Column {
            Text(
                text = "Rechercher",
                color = TextePrimaire,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Découvrez de nouvelles musiques",
                color = TexteSecondaire,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BarreRecherche(
    requete: String,
    surChangementRequete: (String) -> Unit,
    surRecherche: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = requete,
            onValueChange = surChangementRequete,
            label = { Text("Rechercher de la musique") },
            placeholder = { Text("Artiste, chanson, album...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = CouleurPrincipale
                )
            },
            trailingIcon = {
                if (requete.isNotEmpty()) {
                    IconButton(onClick = { surChangementRequete("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Effacer",
                            tint = TexteSecondaire
                        )
                    }
                }
            },
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
                unfocusedLabelColor = TexteSecondaire,
                focusedContainerColor = SurfaceSombre.copy(alpha = 0.5f),
                unfocusedContainerColor = SurfaceSombre.copy(alpha = 0.5f)
            )
        )
        
        Button(
            onClick = surRecherche,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = requete.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CouleurPrincipale,
                disabledContainerColor = CouleurPrincipale.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Rechercher",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CategoriesMusique(
    categories: List<String>,
    surSelectionCategorie: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Catégories",
            color = TextePrimaire,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { categorie ->
                FiltreTip(
                    texte = categorie,
                    surClic = { surSelectionCategorie(categorie) }
                )
            }
        }
    }
}

@Composable
private fun FiltreTip(
    texte: String,
    surClic: () -> Unit
) {
    Surface(
        onClick = surClic,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceSombre,
        border = androidx.compose.foundation.BorderStroke(1.dp, CouleurBordure)
    ) {
        Text(
            text = texte,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = TextePrimaire,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EtatChargement() {
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
                "Recherche en cours...",
                color = TexteSecondaire,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ListeResultats(
    pistes: List<Track>,
    surClicPiste: (Track) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "${pistes.size} résultat${if (pistes.size > 1) "s" else ""}",
            color = TexteSecondaire,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pistes) { piste ->
                CartePiste(
                    piste = piste,
                    surClic = { surClicPiste(piste) }
                )
            }
        }
    }
}

@Composable
private fun CartePiste(
    piste: Track,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { surClic() },
        colors = CardDefaults.cardColors(
            containerColor = SurfaceSombre
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image de la piste
            AsyncImage(
                model = piste.thumbnailUrl,
                contentDescription = piste.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CouleurPrincipale.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            
            // Informations de la piste
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = piste.title,
                    color = TextePrimaire,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = piste.artist,
                    color = TexteSecondaire,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = CouleurAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = piste.duration,
                        color = CouleurAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Bouton de lecture
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = CouleurPrincipale.copy(alpha = 0.2f)
            ) {
                IconButton(onClick = surClic) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Jouer",
                        tint = CouleurPrincipale
                    )
                }
            }
        }
    }
}

@Composable
private fun EtatErreur(
    message: String,
    surReessayer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ErreurSombre.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErreurSombre)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ErreurSombre,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Erreur de recherche",
                style = MaterialTheme.typography.titleMedium,
                color = TextePrimaire,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TexteSecondaire
            )
            Button(
                onClick = surReessayer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CouleurPrincipale
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Réessayer")
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
            colors = CardDefaults.cardColors(
                containerColor = SurfaceSombre
            ),
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
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = CouleurPrincipale,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Text(
                    text = "🎵 Rechercher de la musique",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextePrimaire,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Entrez un terme de recherche ou\nsélectionnez une catégorie ci-dessus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TexteSecondaire,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun naviguerVersPiste(controleurNavigation: NavController, piste: Track) {
    try {
        Log.d("RechercheMusique", "🎵 Piste cliquée: ${piste.title}")
        
        val titreEncode = java.net.URLEncoder.encode(piste.title, "UTF-8")
        val artisteEncode = java.net.URLEncoder.encode(piste.artist, "UTF-8")
        val imagetteEncodee = java.net.URLEncoder.encode(piste.thumbnailUrl, "UTF-8")
        val dureeEncodee = java.net.URLEncoder.encode(piste.duration, "UTF-8")
        val descriptionEncodee = java.net.URLEncoder.encode(piste.description, "UTF-8")
        
        val routeNavigation = "now_playing/${piste.id}/$titreEncode/$artisteEncode/$imagetteEncodee/$dureeEncodee/$descriptionEncodee"
        Log.d("RechercheMusique", "🎵 Navigation vers: $routeNavigation")
        
        controleurNavigation.navigate(routeNavigation)
    } catch (e: Exception) {
        Log.e("RechercheMusique", "❌ Erreur de navigation: ${e.message}", e)
    }
}

