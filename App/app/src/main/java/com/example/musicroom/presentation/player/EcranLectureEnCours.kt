package com.example.musicroom.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.RepeatMode
import com.example.musicroom.presentation.theme.*
import kotlin.math.roundToInt

/**
 * ========================================================================
 * ÉCRAN DE LECTURE EN COURS - Design Moderne
 * ========================================================================
 * Interface de lecteur musical repensée avec visualisation audio et
 * contrôles modernes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranLectureEnCours(
    piste: Track,
    controleurNavigation: NavController,
    modeleVue: NowPlayingViewModel = hiltViewModel()
) {
    val enLecture by modeleVue.isPlaying.collectAsState()
    val positionActuelle by modeleVue.currentPosition.collectAsState()
    val duree by modeleVue.duration.collectAsState()
    val lecteurPret by modeleVue.isPlayerReady.collectAsState()
    val lectureAleatoireActive by modeleVue.isShuffleEnabled.collectAsState()
    val modeRepetition by modeleVue.repeatMode.collectAsState()
    var estAime by remember { mutableStateOf(false) }
    var afficherDialoguePlaylist by remember { mutableStateOf(false) }
    var afficherDialogueEvenement by remember { mutableStateOf(false) }
    
    // Démarrer la lecture au chargement
    LaunchedEffect(piste) {
        modeleVue.playTrack(piste)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
    ) {
        // Image d'arrière-plan floue
        AsyncImage(
            model = piste.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FondSombre.copy(alpha = 0.7f),
                            FondSombre,
                            FondSombre
                        )
                    )
                )
        )
        
        // Contenu principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Barre supérieure
            BarreSuperieure(
                surRetour = { controleurNavigation.popBackStack() },
                surAjouterPlaylist = { afficherDialoguePlaylist = true },
                surAjouterEvenement = { afficherDialogueEvenement = true }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pochette d'album moderne
            PochetteAlbum(
                urlImage = piste.thumbnailUrl,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Statut du lecteur
            StatutLecteur(
                lecteurPret = lecteurPret,
                titrePiste = piste.title
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visualiseur audio
            AudioVisualizer(
                isPlaying = enLecture,
                modifier = Modifier
                    .height(70.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Informations de la piste
            InformationsPiste(
                titre = piste.title,
                artiste = piste.artist,
                estAime = estAime,
                surBasculementAime = { estAime = !estAime }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Barre de progression
            BarreProgression(
                positionActuelle = if (duree > 0) positionActuelle else 0f,
                duree = duree,
                surChangementPosition = { nouvellePosition ->
                    modeleVue.seekTo(nouvellePosition)
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Boutons de contrôle
            BoutonsControle(
                enLecture = enLecture,
                lectureAleatoireActive = lectureAleatoireActive,
                modeRepetition = modeRepetition,
                surLecture = { modeleVue.play() },
                surPause = { modeleVue.pause() },
                surBasculementAleatoire = { modeleVue.toggleShuffle() },
                surCyclerRepetition = { modeleVue.cycleRepeatMode() },
                surPrecedent = { /* À implémenter */ },
                surSuivant = { /* À implémenter */ }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Actions supplémentaires
            ActionsSupplementaires()
        }
    }
    
    // Dialogues
    if (afficherDialoguePlaylist) {
        AddToPlaylistDialog(
            track = piste,
            onDismiss = { afficherDialoguePlaylist = false }
        )
    }
    
    if (afficherDialogueEvenement) {
        AddToEventDialog(
            track = piste,
            onDismiss = { afficherDialogueEvenement = false }
        )
    }
}

@Composable
private fun BarreSuperieure(
    surRetour: () -> Unit,
    surAjouterPlaylist: () -> Unit,
    surAjouterEvenement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bouton retour
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = SurfaceSombre.copy(alpha = 0.7f)
        ) {
            IconButton(onClick = surRetour) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Retour",
                    tint = TextePrimaire,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Titre central
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EN LECTURE",
                color = TexteSecondaire,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "SalleMusicale",
                color = TextePrimaire,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Actions droite
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = SurfaceSombre.copy(alpha = 0.7f)
            ) {
                IconButton(onClick = surAjouterPlaylist) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "Ajouter à la playlist",
                        tint = TextePrimaire
                    )
                }
            }
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = SurfaceSombre.copy(alpha = 0.7f)
            ) {
                IconButton(onClick = surAjouterEvenement) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = "Ajouter à l'événement",
                        tint = TextePrimaire
                    )
                }
            }
        }
    }
}

@Composable
private fun PochetteAlbum(
    urlImage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(320.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box {
            AsyncImage(
                model = urlImage,
                contentDescription = "Pochette de l'album",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay subtil
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun StatutLecteur(
    lecteurPret: Boolean,
    titrePiste: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (lecteurPret) Icons.Default.MusicNote else Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = if (lecteurPret) CouleurPrincipale else TexteSecondaire,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (lecteurPret) "En lecture : $titrePiste" else "Chargement...",
            color = if (lecteurPret) CouleurPrincipale else TexteSecondaire,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InformationsPiste(
    titre: String,
    artiste: String,
    estAime: Boolean,
    surBasculementAime: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titre,
                color = TextePrimaire,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artiste,
                color = TexteSecondaire,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (estAime) CouleurPrincipale.copy(alpha = 0.2f) else SurfaceSombre.copy(alpha = 0.7f)
        ) {
            IconButton(onClick = surBasculementAime) {
                Icon(
                    if (estAime) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (estAime) "Retirer des favoris" else "Ajouter aux favoris",
                    tint = if (estAime) CouleurPrincipale else TextePrimaire,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BarreProgression(
    positionActuelle: Float,
    duree: Float,
    surChangementPosition: (Float) -> Unit
) {
    Column {
        Slider(
            value = positionActuelle,
            onValueChange = surChangementPosition,
            valueRange = 0f..duree,
            colors = SliderDefaults.colors(
                thumbColor = CouleurPrincipale,
                activeTrackColor = CouleurPrincipale,
                inactiveTrackColor = SurfaceSombre
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formaterTemps(positionActuelle),
                color = TexteSecondaire,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formaterTemps(duree),
                color = TexteSecondaire,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BoutonsControle(
    enLecture: Boolean,
    lectureAleatoireActive: Boolean,
    modeRepetition: RepeatMode,
    surLecture: () -> Unit,
    surPause: () -> Unit,
    surBasculementAleatoire: () -> Unit,
    surCyclerRepetition: () -> Unit,
    surPrecedent: () -> Unit,
    surSuivant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lecture aléatoire
        BoutonControle(
            icone = Icons.Default.Shuffle,
            estActif = lectureAleatoireActive,
            surClic = surBasculementAleatoire
        )
        
        // Précédent
        BoutonControle(
            icone = Icons.Default.SkipPrevious,
            taille = 52.dp,
            surClic = surPrecedent
        )
        
        // Lecture/Pause
        BoutonControlePrincipal(
            icone = if (enLecture) Icons.Default.Pause else Icons.Default.PlayArrow,
            surClic = if (enLecture) surPause else surLecture
        )
        
        // Suivant
        BoutonControle(
            icone = Icons.Default.SkipNext,
            taille = 52.dp,
            surClic = surSuivant
        )
        
        // Répétition
        BoutonControle(
            icone = when (modeRepetition) {
                RepeatMode.OFF -> Icons.Default.Repeat
                RepeatMode.ALL -> Icons.Default.Repeat
                RepeatMode.ONE -> Icons.Default.RepeatOne
            },
            estActif = modeRepetition != RepeatMode.OFF,
            surClic = surCyclerRepetition
        )
    }
}

@Composable
private fun BoutonControle(
    icone: ImageVector,
    surClic: () -> Unit,
    modifier: Modifier = Modifier,
    taille: Dp = 44.dp,
    estActif: Boolean = false
) {
    Surface(
        modifier = modifier.size(taille),
        shape = CircleShape,
        color = if (estActif) CouleurPrincipale.copy(alpha = 0.2f) else Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { surClic() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icone,
                contentDescription = null,
                tint = if (estActif) CouleurPrincipale else TextePrimaire,
                modifier = Modifier.size(taille * 0.55f)
            )
        }
    }
}

@Composable
private fun BoutonControlePrincipal(
    icone: ImageVector,
    surClic: () -> Unit
) {
    Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = CouleurPrincipale,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { surClic() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun ActionsSupplementaires() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoutonAction(
            icone = Icons.Default.Devices,
            texte = "Appareils",
            surClic = { /* À implémenter */ }
        )
        
        BoutonAction(
            icone = Icons.Default.QueueMusic,
            texte = "File d'attente",
            surClic = { /* À implémenter */ }
        )
        
        BoutonAction(
            icone = Icons.Default.Share,
            texte = "Partager",
            surClic = { /* À implémenter */ }
        )
    }
}

@Composable
private fun BoutonAction(
    icone: ImageVector,
    texte: String,
    surClic: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { surClic() }
    ) {
        Icon(
            icone,
            contentDescription = texte,
            tint = TexteSecondaire,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = texte,
            color = TexteSecondaire,
            fontSize = 12.sp
        )
    }
}

private fun formaterTemps(secondes: Float): String {
    val totalSecondes = secondes.roundToInt()
    val minutes = totalSecondes / 60
    val secondesRestantes = totalSecondes % 60
    return String.format("%d:%02d", minutes, secondesRestantes)
}

