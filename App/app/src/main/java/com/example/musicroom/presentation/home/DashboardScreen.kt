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
import androidx.compose.ui.graphics.Color
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
 * ========================================================================================
 * DASHBOARD SCREEN - Modern Home Feed
 * ========================================================================================
 * 
 * Modernized home screen with teal/cyan theme and glassmorphism design.
 * Main dashboard displaying playlists, recommendations, events, and notifications.
 * 
 * 🎯 KEY FEATURES:
 * ========================================================================================
 * ✅ User playlists with modern card design
 * ✅ Recommended songs feed
 * ✅ Popular tracks and artists
 * ✅ Events and notifications
 * ✅ Glassmorphism effects
 * ✅ Enhanced navigation and UX
 * 
 * 🎨 DESIGN UPDATES:
 * ========================================================================================
 * - New teal/cyan gradient theme
 * - Glass morphism cards
 * - Enhanced spacing and typography
 * - Smooth animations
 * - Modern Material Design 3 components
 * ========================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionInProgress by viewModel.actionInProgress.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }
    
    when (val currentState = uiState) {
        is HomeUiState.Loading -> {
            ModernLoadingScreen()
        }
        is HomeUiState.Success -> {
            DashboardContent(
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
            ModernErrorScreen(
                message = currentState.message,
                onRetry = { viewModel.loadHomeData() }
            )
        }
    }
}

@Composable
private fun ModernLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = PrimaryTeal,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                "Loading your music...",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModernErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = CoralPink,
                modifier = Modifier.size(64.dp)
            )
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun DashboardContent(
    homeData: HomeResponse,
    navController: NavController,
    onRefresh: () -> Unit,
    onDismissEventNotification: (String, String) -> Unit,
    onDismissPlaylistNotification: (String, String) -> Unit,
    onAcceptEventInvitation: (String) -> Unit,
    onAcceptPlaylistInvitation: (String) -> Unit,
    actionInProgress: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome header
            item {
                ModernWelcomeHeader(navController)
            }
            
            // Notifications Section
            val hasNotifications = homeData.notifications.event_notifications.isNotEmpty() ||
                    homeData.notifications.playlist_notifications.isNotEmpty()
            
            if (hasNotifications) {
                item {
                    ModernNotificationsSection(
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
                    ModernPlaylistSection(
                        title = "Your Playlists",
                        playlists = homeData.user_playlists.results,
                        onPlaylistClick = { playlist ->
                            navController.navigate("playlist_tracks/${playlist.id}")
                        }
                    )
                }
            }
            
            // Recommended Songs Section
            if (homeData.recommended_songs.results.isNotEmpty()) {
                item {
                    ModernSongsSection(
                        title = "Recommended for You",
                        songs = homeData.recommended_songs.results,
                        onSongClick = { song ->
                            val track = Track(
                                id = song.id,
                                title = song.name,
                                artist = song.artist_name,
                                thumbnailUrl = song.image ?: song.album_image ?: "",
                                duration = formatDuration(song.duration),
                                channelTitle = song.album_name,
                                description = song.audio
                            )
                            
                            Log.d("DashboardScreen", "🎵 Playing: ${song.name}")
                            
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
                    ModernSongsSection(
                        title = "Popular Now",
                        songs = homeData.popular_songs.results,
                        onSongClick = { song ->
                            val track = Track(
                                id = song.id,
                                title = song.name,
                                artist = song.artist_name,
                                thumbnailUrl = song.image ?: song.album_image ?: "",
                                duration = formatDuration(song.duration),
                                channelTitle = song.album_name,
                                description = song.audio
                            )
                            
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
                    ModernArtistsSection(
                        title = "Popular Artists",
                        artists = homeData.popular_artists.results,
                        onArtistClick = { artist ->
                            navController.navigate("artist/${artist.id}")
                        }
                    )
                }
            }
            
            // Events Section
            if (homeData.events.isNotEmpty()) {
                item {
                    ModernEventsSection(
                        title = "Upcoming Events",
                        events = homeData.events,
                        onEventClick = { event ->
                            navController.navigate("event_details/${event.id}")
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ModernWelcomeHeader(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GlassWhite
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryGradient)
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Music Room",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Discover, create and share music together",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { navController.navigate("music_search") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Search Music",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernNotificationsSection(
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        notifications.event_notifications.forEach { notification ->
            NotificationCard(
                title = "Event Invitation",
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
        
        notifications.playlist_notifications.forEach { notification ->
            NotificationCard(
                title = "Playlist Invitation",
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
private fun ModernPlaylistSection(
    title: String,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists) { playlist ->
                ModernPlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun ModernPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = primaryGradient,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = "Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = playlist.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "by ${playlist.user_name}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernSongsSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs.take(10)) { song ->
                ModernSongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun ModernSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.image ?: song.album_image)
                    .crossfade(true)
                    .build(),
                contentDescription = song.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = primaryGradient
                    ),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = song.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
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
                color = PrimaryTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModernArtistsSection(
    title: String,
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists) { artist ->
                ModernArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun ModernArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.image)
                    .crossfade(true)
                    .build(),
                contentDescription = artist.name,
                modifier = Modifier
                    .size(90.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        brush = primaryGradient
                    ),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = artist.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernEventsSection(
    title: String,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events) { event ->
                ModernEventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun ModernEventCard(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        brush = primaryGradient,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = "Event",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = event.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
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
                color = PrimaryTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

