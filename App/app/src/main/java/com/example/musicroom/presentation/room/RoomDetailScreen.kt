package com.example.musicroom.presentation.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.musicroom.data.models.MusicRoom
import com.example.musicroom.data.models.activeRooms
import com.example.musicroom.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    navController: NavController
) {
    val room = activeRooms.find { it.id == roomId }
    
    if (room == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Room not found",
                color = TextPrimary,
                fontSize = 18.sp
            )
        }
        return
    }

    // Sample playlist for the room
    val roomPlaylist = listOf(
        Track("1", "Current Song", "Artist 1", "3:45", true),
        Track("2", "Next Song", "Artist 2", "4:12", false),
        Track("3", "Another Song", "Artist 3", "3:30", false),
        Track("4", "Great Track", "Artist 4", "4:45", false),
        Track("5", "Cool Song", "Artist 5", "3:15", false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        room.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Room Info
            item {
                RoomInfoCard(room = room)
            }
            
            // Playlist Header
            item {
                Text(
                    "Playlist",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Playlist Items
            items(roomPlaylist) { track ->
                TrackItem(track = track)
            }
        }
    }
}

// Data class for tracks
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val isPlaying: Boolean = false
)

@Composable
private fun RoomInfoCard(room: MusicRoom) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        room.name,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Hosted by ${room.host}",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                
                if (room.isLive) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E4620)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Green, RoundedCornerShape(50))
                            )
                            Text(
                                "LIVE",
                                color = Color.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Currently playing: ${room.currentTrack ?: "Nothing"}",
                color = TextSecondary,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "${room.listeners} listeners",
                color = TextSecondary,
                fontSize = 14.sp
            )
            
            if (room.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    room.description,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TrackItem(track: Track) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (track.isPlaying) PrimaryPurple.copy(alpha = 0.2f) else DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (track.isPlaying) PrimaryPurple else Color.Gray.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = if (track.isPlaying) PrimaryPurple else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (track.isPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Duration
            Text(
                track.duration,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}