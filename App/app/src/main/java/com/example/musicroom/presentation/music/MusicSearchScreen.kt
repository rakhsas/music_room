package com.example.musicroom.presentation.music

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicroom.data.models.Track
import com.example.musicroom.presentation.theme.*

private val musicCategories = listOf(
    "Popular", "Random", "Jazz", "Electronic", "Rock", "Classical", "Ambient", "Hip Hop"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSearchScreen(
    navController: NavController,
    viewModel: MusicSearchViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search music", color = TextSecondary) },
            leadingIcon = { 
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextSecondary) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search button
        Button(
            onClick = { viewModel.searchTracks(searchQuery.trim()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Search", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Music Categories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(musicCategories) { category ->
                FilterChip(
                    onClick = { 
                        searchQuery = category
                        viewModel.searchTracks(category) 
                    },
                    label = { Text(category) },
                    selected = false,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurface,
                        labelColor = TextPrimary,
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when (uiState) {
            is MusicSearchUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
            
            is MusicSearchUiState.Success -> {
                val successState = uiState as MusicSearchUiState.Success
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(successState.tracks) { track ->
                        TrackItem(
                            track = track,
                            onTrackClick = { clickedTrack ->
                                try {
                                    // Navigate to now playing screen with track data
                                    Log.d("MusicSearch", "🎵 Track clicked: ${clickedTrack.title}")
                                    Log.d("MusicSearch", "🎵 Track ID: ${clickedTrack.id}")
                                    Log.d("MusicSearch", "🎵 Track audio: ${clickedTrack.description}")
                                    
                                    val encodedTitle = java.net.URLEncoder.encode(clickedTrack.title, "UTF-8")
                                    val encodedArtist = java.net.URLEncoder.encode(clickedTrack.artist, "UTF-8")
                                    val encodedThumbnailUrl = java.net.URLEncoder.encode(clickedTrack.thumbnailUrl, "UTF-8")
                                    val encodedDuration = java.net.URLEncoder.encode(clickedTrack.duration, "UTF-8")
                                    val encodedDescription = java.net.URLEncoder.encode(clickedTrack.description, "UTF-8")
                                    
                                    val navigationRoute = "now_playing/${clickedTrack.id}/$encodedTitle/$encodedArtist/$encodedThumbnailUrl/$encodedDuration/$encodedDescription"
                                    Log.d("MusicSearch", "🎵 Navigation route: $navigationRoute")
                                    
                                    navController.navigate(navigationRoute)
                                } catch (e: Exception) {
                                    Log.e("MusicSearch", "❌ Navigation error: ${e.message}", e)
                                }
                            }
                        )
                    }
                }
            }
            
            is MusicSearchUiState.Error -> {
                val errorState = uiState as MusicSearchUiState.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error searching for music",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.searchTracks(searchQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            is MusicSearchUiState.Empty -> {
                // Show initial state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎵 Search for music",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Enter a search term or select a category above",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.getPopularTracks() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Show Popular")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    onTrackClick: (Track) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        onClick = { onTrackClick(track) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.duration,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
