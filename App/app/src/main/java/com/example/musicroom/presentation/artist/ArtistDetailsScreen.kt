package com.example.musicroom.presentation.artist

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.musicroom.data.models.Song
import com.example.musicroom.data.models.Artist
import com.example.musicroom.data.models.Track
import com.example.musicroom.presentation.theme.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailsScreen(
    artistId: String,
    navController: NavController,
    viewModel: ArtistDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(artistId) {
        viewModel.loadArtistDetails(artistId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Artist Details", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface
            )
        )
        
        // Content
        when (uiState) {
            is ArtistDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
            
            is ArtistDetailsUiState.Success -> {
                val successState = uiState as ArtistDetailsUiState.Success
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Artist Header
                    item {
                        ArtistHeader(
                            artist = successState.artist,
                            onPlayAllClick = {
                                // Play first song if available
                                if (successState.songs.isNotEmpty()) {
                                    val firstSong = successState.songs.first()
                                    val track = songToTrack(firstSong)
                                    navigateToNowPlaying(navController, track)
                                }
                            }
                        )
                    }
                    
                    // Songs Section
                    item {
                        Text(
                            text = "Songs (${successState.songs.size})",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Songs List
                    items(successState.songs) { song ->
                        ArtistSongItem(
                            song = song,
                            onSongClick = {
                                val track = songToTrack(song)
                                navigateToNowPlaying(navController, track)
                            }
                        )
                    }
                    
                    // Bottom padding
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
            
            is ArtistDetailsUiState.Error -> {
                val errorState = uiState as ArtistDetailsUiState.Error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading artist details",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorState.message,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadArtistDetails(artistId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist,
    onPlayAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artist Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.image ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = "Artist Image",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Artist Name
            Text(
                text = artist.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            // Website
            artist.website?.let { website ->
                if (website.isNotBlank()) {
                    Text(
                        text = website,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Play All Button
            Button(
                onClick = onPlayAllClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play All",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play All")
            }
        }
    }
}

@Composable
private fun ArtistSongItem(
    song: Song,
    onSongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        onClick = onSongClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.image ?: song.album_image ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = "Song Image",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = song.artist_name,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (song.album_name.isNotBlank()) {
                    Text(
                        text = song.album_name,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Duration
            Text(
                text = formatDuration(song.duration),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

// Helper functions
private fun songToTrack(song: Song): Track {
    return Track(
        id = song.id,
        title = song.name,
        artist = song.artist_name,
        thumbnailUrl = song.image ?: song.album_image ?: "",
        duration = formatDuration(song.duration),
        channelTitle = "",
        description = song.audio  // Pass audio URL through description
    )
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

private fun navigateToNowPlaying(navController: NavController, track: Track) {
    try {
        val encodedTitle = URLEncoder.encode(track.title, StandardCharsets.UTF_8.toString())
        val encodedArtist = URLEncoder.encode(track.artist, StandardCharsets.UTF_8.toString())
        val encodedThumbnail = URLEncoder.encode(track.thumbnailUrl, StandardCharsets.UTF_8.toString())
        val encodedDuration = URLEncoder.encode(track.duration, StandardCharsets.UTF_8.toString())
        val encodedChannel = URLEncoder.encode(track.channelTitle, StandardCharsets.UTF_8.toString())
        val encodedAudioUrl = URLEncoder.encode(track.description, StandardCharsets.UTF_8.toString())
        
        navController.navigate(
            "now_playing/$encodedTitle/$encodedArtist/$encodedThumbnail/$encodedDuration/$encodedChannel/$encodedAudioUrl"
        )
    } catch (e: Exception) {
        Log.e("ArtistDetailsScreen", "Navigation error", e)
    }
}

// UI State
sealed class ArtistDetailsUiState {
    object Loading : ArtistDetailsUiState()
    data class Success(val artist: Artist, val songs: List<Song>) : ArtistDetailsUiState()
    data class Error(val message: String) : ArtistDetailsUiState()
}