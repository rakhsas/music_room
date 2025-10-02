package com.example.musicroom.presentation.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.PlaylistApiService
import com.example.musicroom.data.service.PublicPlaylist
import com.example.musicroom.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State for Add to Playlist
sealed class AddToPlaylistUiState {
    object Loading : AddToPlaylistUiState()
    data class Success(val playlists: List<PublicPlaylist>) : AddToPlaylistUiState()
    data class Error(val message: String) : AddToPlaylistUiState()
}

sealed class AddTrackResult {
    object Loading : AddTrackResult()
    data class Success(val message: String) : AddTrackResult()
    data class Error(val message: String) : AddTrackResult()
}

// ViewModel for Add to Playlist functionality
@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistApiService: PlaylistApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddToPlaylistUiState>(AddToPlaylistUiState.Loading)
    val uiState: StateFlow<AddToPlaylistUiState> = _uiState.asStateFlow()
    
    private val _addTrackResult = MutableStateFlow<AddTrackResult?>(null)
    val addTrackResult: StateFlow<AddTrackResult?> = _addTrackResult.asStateFlow()
    
    fun loadUserPlaylists() {
        viewModelScope.launch {
            _uiState.value = AddToPlaylistUiState.Loading
            
            playlistApiService.getMyPlaylists().fold(
                onSuccess = { playlists ->
                    _uiState.value = AddToPlaylistUiState.Success(playlists)
                },
                onFailure = { exception ->
                    _uiState.value = AddToPlaylistUiState.Error(
                        exception.message ?: "Failed to load playlists"
                    )
                }
            )
        }
    }
    
    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            _addTrackResult.value = AddTrackResult.Loading
            
            Log.d("AddToPlaylistVM", "🎵 Adding track $trackId to playlist $playlistId")
            
            playlistApiService.addTrackToPlaylist(playlistId, trackId).fold(
                onSuccess = { response ->
                    Log.d("AddToPlaylistVM", "✅ Successfully added track: ${response.message}")
                    _addTrackResult.value = AddTrackResult.Success(response.message)
                },
                onFailure = { exception ->
                    Log.e("AddToPlaylistVM", "❌ Failed to add track: ${exception.message}")
                    
                    // Provide more specific error messages
                    val errorMessage = when {
                        exception.message?.contains("not found") == true -> 
                            "Unable to add track to this playlist. Please try again."
                        exception.message?.contains("already") == true -> 
                            "This track is already in the playlist"
                        exception.message?.contains("Authentication") == true -> 
                            "Please log in again to add tracks"
                        exception.message?.contains("Access denied") == true -> 
                            "You don't have permission to add tracks to this playlist"
                        else -> "Failed to add track: ${exception.message}"
                    }
                    _addTrackResult.value = AddTrackResult.Error(errorMessage)
                }
            )
        }
    }
    
    fun clearAddTrackResult() {
        _addTrackResult.value = null
    }
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val addTrackResult by viewModel.addTrackResult.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserPlaylists()
    }

    // Handle add track result
    LaunchedEffect(addTrackResult) {
        addTrackResult?.let { result ->
            when (result) {
                is AddTrackResult.Success -> {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearAddTrackResult()
                    onDismiss()
                }
                is AddTrackResult.Error -> {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearAddTrackResult()
                }
                else -> {}
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to Playlist",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Track info
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add track result notification
                addTrackResult?.let { result ->
                    when (result) {
                        is AddTrackResult.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adding to playlist...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        is AddTrackResult.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Green.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.Green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = result.message,
                                        color = Color.Green,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        is AddTrackResult.Error -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkError.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = DarkError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = result.message,
                                        color = DarkError,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Playlists list
                when (val currentState = uiState) {
                    is AddToPlaylistUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading playlists...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    is AddToPlaylistUiState.Success -> {
                        if (currentState.playlists.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "📝",
                                        fontSize = 32.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No playlists yet",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Create a playlist first",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentState.playlists) { playlist ->
                                    PlaylistSelectionItem(
                                        playlist = playlist,
                                        onSelect = { 
                                            viewModel.addTrackToPlaylist(playlist.id, track.id)
                                        },
                                        isLoading = addTrackResult is AddTrackResult.Loading
                                    )
                                }
                            }
                        }
                    }
                    
                    is AddToPlaylistUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "❌",
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Failed to load playlists",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = currentState.message,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadUserPlaylists() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                ) {
                                    Text("Retry", color = DarkBackground)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simplified PlaylistSelectionItem without invite functionality
@Composable
private fun PlaylistSelectionItem(
    playlist: PublicPlaylist,
    onSelect: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onSelect() },
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Playlist info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${playlist.songCount} songs",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (playlist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                        contentDescription = if (playlist.isPublic) "Public" else "Private",
                        tint = if (playlist.isPublic) Color.Green else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    
                    // Show owner indicator
                    if (playlist.isOwner) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your playlist",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}