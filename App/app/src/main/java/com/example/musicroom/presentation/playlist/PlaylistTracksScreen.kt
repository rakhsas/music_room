package com.example.musicroom.presentation.playlist

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.PlaylistApiService
import com.example.musicroom.data.service.PlaylistWithTracks
import com.example.musicroom.data.service.PlaylistTrackDetails
import com.example.musicroom.presentation.theme.*
import com.example.musicroom.presentation.player.InviteUserDialog
import com.example.musicroom.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

// UI State
sealed class PlaylistTracksUiState {
    object Loading : PlaylistTracksUiState()
    data class Success(val playlistWithTracks: PlaylistWithTracks) : PlaylistTracksUiState()
    data class Error(val message: String) : PlaylistTracksUiState()
}

// ViewModel
@HiltViewModel
class PlaylistTracksViewModel @Inject constructor(
    private val playlistApiService: PlaylistApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PlaylistTracksUiState>(PlaylistTracksUiState.Loading)
    val uiState: StateFlow<PlaylistTracksUiState> = _uiState.asStateFlow()
    
    fun loadPlaylistTracks(playlistId: String) {
        viewModelScope.launch {
            Log.d("PlaylistTracksVM", "🎵 Loading tracks for playlist: $playlistId")
            _uiState.value = PlaylistTracksUiState.Loading
            
            playlistApiService.getPlaylistTracks(playlistId).fold(
                onSuccess = { playlistWithTracks ->
                    _uiState.value = PlaylistTracksUiState.Success(playlistWithTracks)
                },
                onFailure = { exception ->
                    _uiState.value = PlaylistTracksUiState.Error(
                        exception.message ?: "Failed to load playlist tracks"
                    )
                }
            )
        }
    }
    
    fun refresh(playlistId: String) {
        loadPlaylistTracks(playlistId)
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            playlistApiService.removeTrackFromPlaylist(playlistId, trackId).fold(
                onSuccess = { message ->
                    onSuccess()
                    loadPlaylistTracks(playlistId) // Refresh the list
                },
                onFailure = { exception ->
                    onError(exception.message ?: "Failed to remove track")
                }
            )
        }
    }

    fun deletePlaylist(playlistId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            playlistApiService.deletePlaylist(playlistId).fold(
                onSuccess = { message ->
                    onSuccess()
                },
                onFailure = { exception ->
                    onError(exception.message ?: "Failed to delete playlist")
                }
            )
        }
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistTracksScreen(
    playlistId: String,
    navController: NavController,
    viewModel: PlaylistTracksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Add debug logging
    LaunchedEffect(playlistId) {
        Log.d("PlaylistTracksScreen", "🎵 Loading playlist tracks for ID: $playlistId")
        try {
            viewModel.loadPlaylistTracks(playlistId)
        } catch (e: Exception) {
            Log.e("PlaylistTracksScreen", "❌ Error loading playlist tracks", e)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar with better error handling
        TopAppBar(
            title = { 
                Text(
                    text = when (val currentState = uiState) {
                        is PlaylistTracksUiState.Success -> currentState.playlistWithTracks.playlist_info.name
                        else -> "Playlist"
                    },
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { 
                        try {
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Log.e("PlaylistTracksScreen", "❌ Navigation error", e)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                // Add invite button for playlist owners
                if (uiState is PlaylistTracksUiState.Success) {
                    val currentPlaylist = (uiState as PlaylistTracksUiState.Success).playlistWithTracks
                    IconButton(
                        onClick = { showInviteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Invite Users",
                            tint = PrimaryPurple
                        )
                    }
                    
                    // Delete playlist button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete Playlist",
                            tint = Color.Red
                        )
                    }
                }
                
                IconButton(
                    onClick = { 
                        try {
                            viewModel.refresh(playlistId)
                        } catch (e: Exception) {
                            Log.e("PlaylistTracksScreen", "❌ Refresh error", e)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Refresh",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface
            )
        )
        
        // Content with enhanced error states
        when (val currentState = uiState) {
            is PlaylistTracksUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chargement de la playlist...",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Identifiant de la playlist: $playlistId",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            is PlaylistTracksUiState.Success -> {
                PlaylistContent(
                    playlistWithTracks = currentState.playlistWithTracks,
                    onTrackClick = { track ->
                        try {
                            // Convert PlaylistTrackDetails to Track and navigate
                            val convertedTrack = Track(
                                id = track.id,
                                title = track.name,
                                artist = track.artist_name,
                                thumbnailUrl = track.image.ifEmpty { track.album_image },
                                duration = formatDuration(track.duration),
                                channelTitle = track.album_name,
                                description = track.audio // Store audio URL
                            )
                            navigateToNowPlaying(navController, convertedTrack)
                        } catch (e: Exception) {
                            Log.e("PlaylistTracksScreen", "❌ Error playing track", e)
                        }
                    },
                    onInviteClick = { showInviteDialog = true },
                    onDeleteTrack = { track ->
                        viewModel.removeTrackFromPlaylist(
                            playlistId = playlistId,
                            trackId = track.id,
                            onSuccess = {
                                Log.d("PlaylistTracksScreen", "Track removed successfully")
                            },
                            onError = { error ->
                                errorMessage = error
                                Log.e("PlaylistTracksScreen", "Remove track failed: $error")
                            }
                        )
                    }
                )
            }
            
            is PlaylistTracksUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "😔",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Échec du chargement de la playlist",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentState.message,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                try {
                                    viewModel.refresh(playlistId)
                                } catch (e: Exception) {
                                    Log.e("PlaylistTracksScreen", "❌ Retry error", e)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPurple
                            )
                        ) {
                            Text("Try Again", color = Color.White)
                        }
                    }
                }
            }
        }
    }
    
    // Show invite dialog
    if (showInviteDialog) {
        InviteUserDialog(
            playlistName = when (val currentState = uiState) {
                is PlaylistTracksUiState.Success -> currentState.playlistWithTracks.playlist_info.name
                else -> "Playlist"
            },
            onDismiss = { showInviteDialog = false },
            onInvite = { username ->
                // TODO: Implement invite functionality
                Log.d("PlaylistTracksScreen", "🎯 Inviting user: $username to playlist: $playlistId")
                showInviteDialog = false
            }
        )
    }
    
    // Delete playlist confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Playlist",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this playlist? This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePlaylist(
                            playlistId = playlistId,
                            onSuccess = {
                                // Navigate back to playlists screen
                                navController.navigate("playlists") {
                                    popUpTo("playlist_tracks/$playlistId") { inclusive = true }
                                }
                            },
                            onError = { error ->
                                errorMessage = error
                                Log.e("PlaylistTracksScreen", "Delete playlist failed: $error")
                            }
                        )
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
    
    // Show error message
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("Dismiss", color = PrimaryPurple)
                }
            },
            containerColor = Color.Red.copy(alpha = 0.9f)
        ) {
            Text(text = error, color = Color.White)
        }
    }
}

@Composable
fun PlaylistContent(
    playlistWithTracks: PlaylistWithTracks,
    onTrackClick: (PlaylistTrackDetails) -> Unit,
    onInviteClick: (() -> Unit)? = null,
    onDeleteTrack: ((PlaylistTrackDetails) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Playlist Header
        item {
            PlaylistHeaderInfo(
                playlistInfo = playlistWithTracks.playlist_info,
                onInviteClick = onInviteClick
            )
        }

        // Track List
        itemsIndexed(playlistWithTracks.tracks) { index, track ->
            PlaylistTrackRow(
                track = track,
                position = index + 1,
                onTrackClick = { onTrackClick(track) },
                onDeleteTrack = onDeleteTrack?.let { { onDeleteTrack(track) } }
            )
            if (index < playlistWithTracks.tracks.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = DarkSurface
                )
            }
        }
    }
}

@Composable
fun PlaylistHeaderInfo(
    playlistInfo: com.example.musicroom.data.service.PlaylistInfo,
    onInviteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = playlistInfo.name,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Créé par ${playlistInfo.owner}",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlistInfo.track_count} Morceaux de musique • ${playlistInfo.followers_count} followers",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (playlistInfo.is_public) Icons.Default.Public else Icons.Default.Lock,
                    contentDescription = if (playlistInfo.is_public) "Public playlist" else "Private playlist",
                    tint = if (playlistInfo.is_public) PrimaryPurple else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (playlistInfo.is_public) "Public" else "Privé",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PlaylistTrackRow(
    track: PlaylistTrackDetails,
    position: Int,
    onTrackClick: () -> Unit,
    onDeleteTrack: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position number
            Text(
                text = position.toString(),
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track artwork or music note icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (track.image.isNotEmpty() || track.album_image.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.image.ifEmpty { track.album_image })
                            .crossfade(true)
                            .build(),
                        contentDescription = "Track artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist_name,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Duration
            Text(
                text = formatDuration(track.duration),
                color = TextSecondary,
                fontSize = 12.sp
            )
            
            // Delete track button
            onDeleteTrack?.let { deleteCallback ->
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = deleteCallback,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Track",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Helper functions
fun navigateToNowPlaying(navController: NavController, track: Track) {
    try {
        val encodedTitle = URLEncoder.encode(track.title, "UTF-8")
        val encodedArtist = URLEncoder.encode(track.artist, "UTF-8")
        val encodedThumbnail = URLEncoder.encode(track.thumbnailUrl, "UTF-8")
        val encodedDuration = URLEncoder.encode(track.duration, "UTF-8")
        val encodedDescription = URLEncoder.encode(track.description, "UTF-8")
        
        navController.navigate(
            "now_playing/${track.id}/$encodedTitle/$encodedArtist/$encodedThumbnail/$encodedDuration/$encodedDescription"
        )
    } catch (e: Exception) {
        Log.e("PlaylistTracksScreen", "Navigation error: ${e.message}")
    }
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}