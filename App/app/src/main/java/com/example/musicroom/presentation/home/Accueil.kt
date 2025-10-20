package com.example.musicroom.presentation.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicroom.data.models.*
import com.example.musicroom.presentation.theme.*

/**
 * ========================================================================
 * ÉCRAN D'ACCUEIL - Design Moderne
 * ========================================================================
 * Tableau de bord principal avec playlists, recommandations et notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranAccueil(
    controleurNavigation: NavController,
    modeleVue: HomeViewModel = hiltViewModel()
) {
    val etatUI by modeleVue.uiState.collectAsState()
    val actionEnCours by modeleVue.actionInProgress.collectAsState()
    
    LaunchedEffect(Unit) {
        modeleVue.loadHomeData()
    }
    
    when (val etatActuel = etatUI) {
        is HomeUiState.Loading -> {
            EcranChargement()
        }
        is HomeUiState.Success -> {
            ContenuAccueil(
                donneesAccueil = etatActuel.data,
                controleurNavigation = controleurNavigation,
                surRafraichir = { modeleVue.loadHomeData() },
                surRejetNotificationEvenement = { idEvenement, nomInvitant ->
                    modeleVue.declineEventInvitation(idEvenement, nomInvitant)
                },
                surRejetNotificationPlaylist = { idPlaylist, nomInvitant ->
                    modeleVue.declinePlaylistInvitation(idPlaylist, nomInvitant)
                },
                surAcceptationInvitationEvenement = { idEvenement ->
                    modeleVue.acceptEventInvitation(idEvenement)
                },
                surAcceptationInvitationPlaylist = { idPlaylist ->
                    modeleVue.acceptPlaylistInvitation(idPlaylist)
                },
                actionEnCours = actionEnCours
            )
        }
        is HomeUiState.Error -> {
            EcranErreur(
                message = etatActuel.message,
                surReessayer = { modeleVue.loadHomeData() }
            )
        }
    }
}

@Composable
private fun EcranChargement() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = CouleurPrincipale,
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp
            )
            Text(
                "Chargement de votre musique...",
                color = TextePrimaire,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EcranErreur(
    message: String,
    surReessayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceSombre
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = ErreurSombre,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Oups! Une erreur s'est produite",
                    color = TextePrimaire,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    message,
                    color = TexteSecondaire,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = surReessayer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CouleurPrincipale
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Réessayer", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ContenuAccueil(
    donneesAccueil: HomeResponse,
    controleurNavigation: NavController,
    surRafraichir: () -> Unit,
    surRejetNotificationEvenement: (String, String) -> Unit,
    surRejetNotificationPlaylist: (String, String) -> Unit,
    surAcceptationInvitationEvenement: (String) -> Unit,
    surAcceptationInvitationPlaylist: (String) -> Unit,
    actionEnCours: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // En-tête de bienvenue moderne
        item {
            EnTeteBienvenue(controleurNavigation)
        }
        
        // Section des notifications
        val aDesNotifications = donneesAccueil.notifications.event_notifications.isNotEmpty() ||
                donneesAccueil.notifications.playlist_notifications.isNotEmpty()
        
        if (aDesNotifications) {
            item {
                SectionNotifications(
                    notifications = donneesAccueil.notifications,
                    surRejetNotificationEvenement = surRejetNotificationEvenement,
                    surRejetNotificationPlaylist = surRejetNotificationPlaylist,
                    surAcceptationInvitationEvenement = surAcceptationInvitationEvenement,
                    surAcceptationInvitationPlaylist = surAcceptationInvitationPlaylist,
                    controleurNavigation = controleurNavigation,
                    actionEnCours = actionEnCours
                )
            }
        }
        
        // Vos playlists
        if (donneesAccueil.user_playlists.results.isNotEmpty()) {
            item {
                SectionPlaylists(
                    titre = "Vos Playlists",
                    playlists = donneesAccueil.user_playlists.results,
                    surClicPlaylist = { playlist ->
                        controleurNavigation.navigate("playlist_tracks/${playlist.id}")
                    }
                )
            }
        }
        
        // Chansons recommandées
        if (donneesAccueil.recommended_songs.results.isNotEmpty()) {
            item {
                SectionChansons(
                    titre = "Recommandé pour vous",
                    chansons = donneesAccueil.recommended_songs.results,
                    surClicChanson = { chanson ->
                        naviguerVersChanson(controleurNavigation, chanson)
                    }
                )
            }
        }
        
        // Chansons populaires
        if (donneesAccueil.popular_songs.results.isNotEmpty()) {
            item {
                SectionChansons(
                    titre = "Tendances",
                    chansons = donneesAccueil.popular_songs.results,
                    surClicChanson = { chanson ->
                        naviguerVersChanson(controleurNavigation, chanson)
                    }
                )
            }
        }
        
        // Récemment écouté
        if (donneesAccueil.recently_listened.results.isNotEmpty()) {
            item {
                SectionChansons(
                    titre = "Récemment écouté",
                    chansons = donneesAccueil.recently_listened.results,
                    surClicChanson = { chanson ->
                        naviguerVersChanson(controleurNavigation, chanson)
                    }
                )
            }
        }
        
        // Artistes populaires
        if (donneesAccueil.popular_artists.results.isNotEmpty()) {
            item {
                SectionArtistes(
                    titre = "Artistes Populaires",
                    artistes = donneesAccueil.popular_artists.results,
                    surClicArtiste = { artiste ->
                        controleurNavigation.navigate("artist/${artiste.id}")
                    }
                )
            }
        }
        
        // Événements
        if (donneesAccueil.events.isNotEmpty()) {
            item {
                SectionEvenements(
                    titre = "Événements récents",
                    evenements = donneesAccueil.events,
                    surClicEvenement = { evenement ->
                        controleurNavigation.navigate("event_details/${evenement.id}")
                    }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun EnTeteBienvenue(controleurNavigation: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceSombre
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            CouleurPrincipale.copy(alpha = 0.3f),
                            CouleurProfondeViolette.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = CouleurPrincipale,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Bienvenue",
                            color = TexteSecondaire,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "SalleMusicale",
                            color = TextePrimaire,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = "Découvrez, créez et partagez de la musique ensemble",
                    color = TexteSecondaire,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                
                Button(
                    onClick = { controleurNavigation.navigate("music_search") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CouleurPrincipale
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rechercher de la musique", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionNotifications(
    notifications: NotificationsSection,
    surRejetNotificationEvenement: (String, String) -> Unit,
    surRejetNotificationPlaylist: (String, String) -> Unit,
    surAcceptationInvitationEvenement: (String) -> Unit,
    surAcceptationInvitationPlaylist: (String) -> Unit,
    controleurNavigation: NavController,
    actionEnCours: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Notifications",
            color = TextePrimaire,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        notifications.event_notifications.forEach { notification ->
            NotificationCard(
                title = "Invitation à l'événement",
                message = notification.message,
                from = notification.inviter_name,
                onAccept = {
                    if (!actionEnCours) {
                        surAcceptationInvitationEvenement(notification.event_id)
                    }
                },
                onDismiss = {
                    if (!actionEnCours) {
                        surRejetNotificationEvenement(notification.event_id, notification.inviter_name)
                    }
                },
                onClick = {
                    controleurNavigation.navigate("event_details/${notification.event_id}")
                },
                isLoading = actionEnCours
            )
        }
        
        notifications.playlist_notifications.forEach { notification ->
            NotificationCard(
                title = "Invitation à la playlist",
                message = notification.message,
                from = notification.inviter_name,
                onAccept = {
                    if (!actionEnCours) {
                        surAcceptationInvitationPlaylist(notification.playlist_id)
                    }
                },
                onDismiss = {
                    if (!actionEnCours) {
                        surRejetNotificationPlaylist(notification.playlist_id, notification.inviter_name)
                    }
                },
                onClick = {
                    controleurNavigation.navigate("playlist_tracks/${notification.playlist_id}")
                },
                isLoading = actionEnCours
            )
        }
    }
}

@Composable
private fun SectionPlaylists(
    titre: String,
    playlists: List<Playlist>,
    surClicPlaylist: (Playlist) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = titre,
            color = TextePrimaire,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(playlists) { playlist ->
                CartePlaylist(
                    playlist = playlist,
                    surClic = { surClicPlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
private fun CartePlaylist(
    playlist: Playlist,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { surClic() },
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
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
                    contentDescription = "Jouer",
                    tint = TextePrimaire,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = playlist.name,
                color = TextePrimaire,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "par ${playlist.user_name}",
                color = TexteSecondaire,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionChansons(
    titre: String,
    chansons: List<Song>,
    surClicChanson: (Song) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = titre,
            color = TextePrimaire,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chansons.take(10)) { chanson ->
                CarteChanson(
                    chanson = chanson,
                    surClic = { surClicChanson(chanson) }
                )
            }
        }
    }
}

@Composable
private fun CarteChanson(
    chanson: Song,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { surClic() },
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(chanson.image ?: chanson.album_image)
                    .crossfade(true)
                    .build(),
                contentDescription = chanson.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CouleurPrincipale.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = chanson.name,
                color = TextePrimaire,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = chanson.artist_name,
                color = TexteSecondaire,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formaterDuree(chanson.duration),
                color = CouleurAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionArtistes(
    titre: String,
    artistes: List<Artist>,
    surClicArtiste: (Artist) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = titre,
            color = TextePrimaire,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artistes) { artiste ->
                CarteArtiste(
                    artiste = artiste,
                    surClic = { surClicArtiste(artiste) }
                )
            }
        }
    }
}

@Composable
private fun CarteArtiste(
    artiste: Artist,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { surClic() },
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artiste.image)
                    .crossfade(true)
                    .build(),
                contentDescription = artiste.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CouleurPrincipale.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = artiste.name,
                color = TextePrimaire,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionEvenements(
    titre: String,
    evenements: List<Event>,
    surClicEvenement: (Event) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = titre,
            color = TextePrimaire,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(evenements) { evenement ->
                CarteEvenement(
                    evenement = evenement,
                    surClic = { surClicEvenement(evenement) }
                )
            }
        }
    }
}

@Composable
private fun CarteEvenement(
    evenement: Event,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { surClic() },
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CouleurAccent.copy(alpha = 0.4f),
                                CouleurPrincipale.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = "Événement",
                    tint = TextePrimaire,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = evenement.title,
                color = TextePrimaire,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = evenement.organizer.name,
                color = TexteSecondaire,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = CouleurAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${evenement.attendee_count} participants",
                    color = CouleurAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun naviguerVersChanson(controleurNavigation: NavController, chanson: Song) {
    val track = Track(
        id = chanson.id,
        title = chanson.name,
        artist = chanson.artist_name,
        thumbnailUrl = chanson.image ?: chanson.album_image ?: "",
        duration = formaterDuree(chanson.duration),
        channelTitle = chanson.album_name,
        description = chanson.audio
    )
    
    Log.d("EcranAccueil", "🎵 Lecture : ${chanson.name}")
    
    val titreEncode = java.net.URLEncoder.encode(track.title, "UTF-8")
    val artisteEncode = java.net.URLEncoder.encode(track.artist, "UTF-8")
    val imagetteEncodee = java.net.URLEncoder.encode(track.thumbnailUrl, "UTF-8")
    val dureeEncodee = java.net.URLEncoder.encode(track.duration, "UTF-8")
    val descriptionEncodee = java.net.URLEncoder.encode(track.description, "UTF-8")
    
    controleurNavigation.navigate(
        "now_playing/${track.id}/$titreEncode/$artisteEncode/$imagetteEncodee/$dureeEncodee/$descriptionEncodee"
    )
}

private fun formaterDuree(secondes: Int): String {
    val minutes = secondes / 60
    val secondesRestantes = secondes % 60
    return String.format("%d:%02d", minutes, secondesRestantes)
}

