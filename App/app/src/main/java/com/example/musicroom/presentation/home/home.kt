package com.example.musicroom.presentation.home

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
import com.example.musicroom.data.models.Track
import com.example.musicroom.presentation.theme.*

/**
 * Home Screen - Main dashboard with playlists, recommendations, and notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionInProgress by viewModel.actionInProgress.collectAsState()
    
    // Load home data when screen loads
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }
    
    when (val currentState = uiState) {
        is HomeUiState.Loading -> {
            LoadingScreen()
        }
        is HomeUiState.Success -> {
            HomeContent(
                homeData = currentState.data,
                navController = navController,
                onRefresh = { viewModel.loadHomeData() },
                onDismissEventNotification = { eventId, inviterName ->
                    viewModel.declineEventInvitation(eventId, inviterName)
                },
                onDismissPlaylistNotification = { playlistId, inviterName ->
                    viewModel.declinePlaylistInvitation(playlistId, inviterName)
                },
                onAcceptEventInvitation = { eventId ->
                    viewModel.acceptEventInvitation(eventId)
                },
                onAcceptPlaylistInvitation = { playlistId ->
                    viewModel.acceptPlaylistInvitation(playlistId)
                },
                actionInProgress = actionInProgress
            )
        }
        is HomeUiState.Error -> {
            ErrorScreen(
                message = currentState.message,
                onRetry = { viewModel.loadHomeData() }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = PrimaryPurple,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Chargement de votre musique...",
                color = TextPrimary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "Oops! Something went wrong",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun HomeContent(
    homeData: HomeResponse,
    navController: NavController,
    onRefresh: () -> Unit,
    onDismissEventNotification: (String, String) -> Unit,
    onDismissPlaylistNotification: (String, String) -> Unit,
    onAcceptEventInvitation: (String) -> Unit,
    onAcceptPlaylistInvitation: (String) -> Unit,
    actionInProgress: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcome header
        item {
            WelcomeHeader(navController)
        }
        
        // Notifications Section
        val hasNotifications = homeData.notifications.event_notifications.isNotEmpty() ||
                homeData.notifications.playlist_notifications.isNotEmpty()
        
        if (hasNotifications) {
            item {
                NotificationsSection(
                    notifications = homeData.notifications,
                    onDismissEventNotification = onDismissEventNotification,
                    onDismissPlaylistNotification = onDismissPlaylistNotification,
                    onAcceptEventInvitation = onAcceptEventInvitation,
                    onAcceptPlaylistInvitation = onAcceptPlaylistInvitation,
                    navController = navController,
                    actionInProgress = actionInProgress
                )
            }
        }
        
        // User Playlists Section
        if (homeData.user_playlists.results.isNotEmpty()) {
            item {
                PlaylistSection(
                    title = "Votre Playlists",
                    playlists = homeData.user_playlists.results,
                    onPlaylistClick = { playlist ->
                        // Navigate to playlist tracks screen
                        navController.navigate("playlist_tracks/${playlist.id}")
                    }
                )
            }
        }
        
        // Recommended Songs Section
        if (homeData.recommended_songs.results.isNotEmpty()) {
            item {
                SongsSection(
                    title = "Recommandé pour vous",
                    songs = homeData.recommended_songs.results,
                    onSongClick = { song ->
                        // Convert Song to Track and navigate to now playing
                        val track = Track(
                            id = song.id,
                            title = song.name,
                            artist = song.artist_name,
                            thumbnailUrl = song.image ?: song.album_image ?: "",
                            duration = formatDuration(song.duration),
                            channelTitle = song.album_name,
                            description = song.audio // Store audio URL in description field
                        )
                        
                        Log.d("HomeScreen", "🎵 Playing song: ${song.name} with audio: ${song.audio}")
                        
                        // Navigate to now playing screen with dynamic song data
                        val encodedTitle = java.net.URLEncoder.encode(track.title, "UTF-8")
                        val encodedArtist = java.net.URLEncoder.encode(track.artist, "UTF-8")
                        val encodedThumbnailUrl = java.net.URLEncoder.encode(track.thumbnailUrl, "UTF-8")
                        val encodedDuration = java.net.URLEncoder.encode(track.duration, "UTF-8")
                        val encodedDescription = java.net.URLEncoder.encode(track.description, "UTF-8")
                        navController.navigate("now_playing/${track.id}/$encodedTitle/$encodedArtist/$encodedThumbnailUrl/$encodedDuration/$encodedDescription")
                    }
                )
            }
        }
        
        // Popular Songs Section
        if (homeData.popular_songs.results.isNotEmpty()) {
            item {
                SongsSection(
                    title = "Populaires",
                    songs = homeData.popular_songs.results,
                    onSongClick = { song ->
                        // Convert Song to Track and navigate to now playing
                        val track = Track(
                            id = song.id,
                            title = song.name,
                            artist = song.artist_name,
                            thumbnailUrl = song.image ?: song.album_image ?: "",
                            duration = formatDuration(song.duration),
                            channelTitle = song.album_name,
                            description = song.audio // Store audio URL in description field
                        )
                        
                        Log.d("HomeScreen", "🎵 Playing song: ${song.name} with audio: ${song.audio}")
                        
                        // Navigate to now playing with song data
                        val encodedTitle = java.net.URLEncoder.encode(track.title, "UTF-8")
                        val encodedArtist = java.net.URLEncoder.encode(track.artist, "UTF-8")
                        val encodedThumbnailUrl = java.net.URLEncoder.encode(track.thumbnailUrl, "UTF-8")
                        val encodedDuration = java.net.URLEncoder.encode(track.duration, "UTF-8")
                        val encodedDescription = java.net.URLEncoder.encode(track.description, "UTF-8")
                        navController.navigate("now_playing/${track.id}/$encodedTitle/$encodedArtist/$encodedThumbnailUrl/$encodedDuration/$encodedDescription")
                    }
                )
            }
        }
        
        // Recently Listened Section
        if (homeData.recently_listened.results.isNotEmpty()) {
            item {
                SongsSection(
                    title = "Récemment écouté",
                    songs = homeData.recently_listened.results,
                    onSongClick = { song ->
                        // Convert Song to Track and navigate to now playing
                        val track = Track(
                            id = song.id,
                            title = song.name,
                            artist = song.artist_name,
                            thumbnailUrl = song.image ?: song.album_image ?: "",
                            duration = formatDuration(song.duration),
                            channelTitle = song.album_name,
                            description = song.audio // Store audio URL in description field
                        )
                        
                        Log.d("HomeScreen", "🎵 Playing song: ${song.name} with audio: ${song.audio}")
                        
                        // Navigate to now playing with song data
                        val encodedTitle = java.net.URLEncoder.encode(track.title, "UTF-8")
                        val encodedArtist = java.net.URLEncoder.encode(track.artist, "UTF-8")
                        val encodedThumbnailUrl = java.net.URLEncoder.encode(track.thumbnailUrl, "UTF-8")
                        val encodedDuration = java.net.URLEncoder.encode(track.duration, "UTF-8")
                        val encodedDescription = java.net.URLEncoder.encode(track.description, "UTF-8")
                        navController.navigate("now_playing/${track.id}/$encodedTitle/$encodedArtist/$encodedThumbnailUrl/$encodedDuration/$encodedDescription")
                    }
                )
            }
        }
        
        // Popular Artists Section
        if (homeData.popular_artists.results.isNotEmpty()) {
            item {
                ArtistsSection(
                    title = "Artistes Populaires",
                    artists = homeData.popular_artists.results,
                    onArtistClick = { artist ->
                        // Navigate to artist details
                        navController.navigate("artist/${artist.id}")
                    }
                )
            }
        }
        
        // Events Section
        if (homeData.events.isNotEmpty()) {
            item {
                EventsSection(
                    title = "Événements récents",
                    events = homeData.events,
                    onEventClick = { event ->
                        navController.navigate("event_details/${event.id}")
                    }
                )
            }
        }
        
        // Bottom padding for better scrolling
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NotificationsSection(
    notifications: NotificationsSection,
    onDismissEventNotification: (String, String) -> Unit,
    onDismissPlaylistNotification: (String, String) -> Unit,
    onAcceptEventInvitation: (String) -> Unit,
    onAcceptPlaylistInvitation: (String) -> Unit,
    navController: NavController,
    actionInProgress: Boolean
) {
    Column {
        Text(
            text = "Notifications",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Event notifications
        notifications.event_notifications.forEach { notification ->
            NotificationCard(
                title = "Invitation à l'événement",
                message = notification.message,
                from = notification.inviter_name,
                onAccept = { 
                    if (!actionInProgress) {
                        onAcceptEventInvitation(notification.event_id)
                    }
                },
                onDismiss = { 
                    if (!actionInProgress) {
                        onDismissEventNotification(notification.event_id, notification.inviter_name)
                    }
                },
                onClick = { 
                    navController.navigate("event_details/${notification.event_id}")
                },
                isLoading = actionInProgress
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Playlist notifications
        notifications.playlist_notifications.forEach { notification ->
            NotificationCard(
                title = "Invitation à la playlist",
                message = notification.message,
                from = notification.inviter_name,
                onAccept = { 
                    if (!actionInProgress) {
                        onAcceptPlaylistInvitation(notification.playlist_id)
                    }
                },
                onDismiss = { 
                    if (!actionInProgress) {
                        onDismissPlaylistNotification(notification.playlist_id, notification.inviter_name)
                    }
                },
                onClick = { 
                    navController.navigate("playlist_tracks/${notification.playlist_id}")
                },
                isLoading = actionInProgress
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomeHeader(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = purpleGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Bienvenue sur MusicRoom",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Découvrez, créez et partagez de la musique ensemble",
                color = TextSecondary,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { navController.navigate("music_search") },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎵 Chercher")
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    title: String,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Placeholder for playlist image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = PrimaryPurple.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = playlist.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "par ${playlist.user_name}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SongsSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs.take(10)) { song -> // Limit to first 10 songs
                SongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Song artwork
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.image ?: song.album_image)
                    .crossfade(true)
                    .build(),
                contentDescription = song.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryPurple.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = song.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = song.artist_name,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatDuration(song.duration),
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ArtistsSection(
    title: String,
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artist image (circular)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.image)
                    .crossfade(true)
                    .build(),
                contentDescription = artist.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = artist.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EventsSection(
    title: String,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Event image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        color = PrimaryPurple.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = "Event",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = event.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = event.organizer.name,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "${event.attendee_count} attendees",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Format duration from seconds to MM:SS format
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
