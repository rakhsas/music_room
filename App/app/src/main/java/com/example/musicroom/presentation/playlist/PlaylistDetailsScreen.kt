package com.example.musicroom.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicroom.R
import com.example.musicroom.presentation.theme.*
// import com.example.musicroom.presentation.playlist.PlaylistDetailsScreen

// Data models for playlist
data class PlaylistResponse(
    val headers: PlaylistHeaders,
    val results: List<PlaylistDetails>
)

data class PlaylistHeaders(
    val status: String,
    val code: Int,
    val error_message: String,
    val warnings: String,
    val results_count: Int
)

data class PlaylistDetails(
    val id: String,
    val name: String,
    val creationdate: String,
    val user_id: String,
    val user_name: String,
    val zip: String,
    val tracks: List<PlaylistTrack>
)

data class PlaylistTrack(
    val id: String,
    val name: String,
    val album_id: String,
    val artist_id: String,
    val duration: String,
    val artist_name: String,
    val playlistadddate: String,
    val position: String,
    val license_ccurl: String,
    val album_image: String,
    val image: String,
    val audio: String,
    val audiodownload: String,
    val audiodownload_allowed: Boolean
)

@Composable
fun PlaylistDetailsScreen(
    playlistId: String,
    navController: NavController
) {
    var playlistDetails by remember { mutableStateOf<PlaylistDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Sample data for testing
    LaunchedEffect(playlistId) {
        kotlinx.coroutines.delay(1000)
        // Create sample playlist data
        playlistDetails = PlaylistDetails(
            id = playlistId,
            name = "Fresh and happy",
            creationdate = "2009-01-17",
            user_id = "553995",
            user_name = "Lillysternchen",
            zip = "",
            tracks = listOf(
                PlaylistTrack(
                    id = "148229",
                    name = "Fresh & happy overture",
                    album_id = "21272",
                    artist_id = "2656",
                    duration = "196",
                    artist_name = "Arnaud Dromigny",
                    playlistadddate = "2009-01-17 00:00:00",
                    position = "1",
                    license_ccurl = "",
                    album_image = "",
                    image = "",
                    audio = "",
                    audiodownload = "",
                    audiodownload_allowed = true
                ),
                PlaylistTrack(
                    id = "148235",
                    name = "Arnold Srecords' psychedelic tennis match",
                    album_id = "21272",
                    artist_id = "2656",
                    duration = "345",
                    artist_name = "Arnaud Dromigny",
                    playlistadddate = "2009-01-17 00:00:00",
                    position = "2",
                    license_ccurl = "",
                    album_image = "",
                    image = "",
                    audio = "",
                    audiodownload = "",
                    audiodownload_allowed = true
                )
            )
        )
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Playlist",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF9C27B0))
            }
        } else {
            playlistDetails?.let { playlist ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Playlist Header
                    item {
                        PlaylistHeader(playlist = playlist)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Tracks Section
                    item {
                        Text(
                            text = "Tracks (${playlist.tracks.size})",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Track List
                    itemsIndexed(playlist.tracks) { index, track ->
                        PlaylistTrackItem(
                            track = track,
                            position = index + 1,
                            onTrackClick = { 
                                // Handle track click
                            }
                        )
                        if (index < playlist.tracks.size - 1) {
                            HorizontalDivider(
                                color = DarkSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(playlist: PlaylistDetails) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playlist Image placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Playlist Name
        Text(
            text = playlist.name,
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Creator and Date
        Text(
            text = "Created by ${playlist.user_name}",
            color = TextSecondary,
            fontSize = 14.sp
        )
        
        Text(
            text = "Created on ${formatDate(playlist.creationdate)}",
            color = TextSecondary,
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Play Button
        Button(
            onClick = { 
                // Handle play all
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C27B0)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Play All",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PlaylistTrackItem(
    track: PlaylistTrack,
    position: Int,
    onTrackClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Position
            Text(
                text = position.toString(),
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.width(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track Image placeholder
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist_name,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Duration
            Text(
                text = com.example.musicroom.presentation.playlist.formatDuration(track.duration.toIntOrNull() ?: 0),
                color = TextSecondary,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // More Options
            IconButton(
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = TextSecondary
                )
            }
        }
    }
}

// Helper functions
private fun formatDate(dateString: String): String {
    return try {
        val parts = dateString.split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        dateString
    }
}