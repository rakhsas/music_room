package com.example.musicroom.presentation.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.musicroom.data.service.PublicPlaylist
import com.example.musicroom.presentation.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicPlaylistsScreen(
    navController: NavController,
    viewModel: PlaylistDetailsViewModel = hiltViewModel()
) {
    val publicPlaylistsState by viewModel.publicPlaylistsState.collectAsState()
    val myPlaylistsState by viewModel.myPlaylistsState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val createResult by viewModel.createResult.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Handle create result
    LaunchedEffect(createResult) {
        createResult?.let { result ->
            when (result) {
                is CreatePlaylistResult.Success -> {
                    // Show success for 2 seconds then clear
                    delay(2000)
                    viewModel.clearCreateResult()
                }
                is CreatePlaylistResult.Error -> {
                    // Show error for 3 seconds then clear
                    delay(3000)
                    viewModel.clearCreateResult()
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Playlists",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Discover and manage your music",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            
            Row {
                // Refresh button
                IconButton(
                    onClick = { viewModel.refreshCurrentTab() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = PrimaryPurple
                    )
                }
                
                // Create playlist button
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = PrimaryPurple,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Custom Tab Buttons instead of TabRow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    DarkSurface,
                    RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlaylistTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                
                Button(
                    onClick = { viewModel.switchTab(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color.White else Color.Transparent,
                        contentColor = if (isSelected) DarkBackground else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isSelected) 2.dp else 0.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                PlaylistTab.PUBLIC -> Icons.Default.Public
                                PlaylistTab.MY_PLAYLISTS -> Icons.Default.Person
                            },
                            contentDescription = tab.displayName,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Create result notification
        createResult?.let { result ->
            when (result) {
                is CreatePlaylistResult.Success -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Green.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color.Green,
                                modifier = Modifier.size(20.dp)
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
                is CreatePlaylistResult.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkError.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = DarkError,
                                modifier = Modifier.size(20.dp)
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
        }
        
        // Content based on selected tab
        val currentState = when (selectedTab) {
            PlaylistTab.PUBLIC -> publicPlaylistsState
            PlaylistTab.MY_PLAYLISTS -> myPlaylistsState
        }
        
        PlaylistsContent(
            uiState = currentState,
            selectedTab = selectedTab,
            onRefresh = { viewModel.refreshCurrentTab() },
            onCreatePlaylist = { showCreateDialog = true },
            onPlaylistClick = { playlist ->
                // Debug logging
                println("🔍 Playlist clicked: ${playlist.name} with ID: ${playlist.id}")
                
                try {
                    // Navigate to playlist tracks screen
                    val route = "playlist_tracks/${playlist.id}"
                    println("🔍 Navigating to route: $route")
                    navController.navigate(route)
                    println("✅ Navigation call completed")
                } catch (e: Exception) {
                    println("❌ Navigation error: ${e.message}")
                    e.printStackTrace()
                }
            }
        )
    }
    
    // Enhanced create playlist dialog
    if (showCreateDialog) {
        EnhancedCreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreatePlaylist = { name, isPublic, description ->
                viewModel.createPlaylist(name, isPublic, description)
                showCreateDialog = false
            },
            isCreating = isCreating
        )
    }
}

@Composable
private fun PlaylistsContent(
    uiState: PlaylistsUiState,
    selectedTab: PlaylistTab,
    onRefresh: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistClick: (PublicPlaylist) -> Unit
) {
    when (val currentState = uiState) {
        is PlaylistsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading ${selectedTab.displayName.lowercase()}...",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        is PlaylistsUiState.Success -> {
            if (currentState.playlists.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (selectedTab) {
                                PlaylistTab.PUBLIC -> Icons.Default.Public
                                PlaylistTab.MY_PLAYLISTS -> Icons.Default.Person
                            },
                            contentDescription = "No playlists",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedTab) {
                                PlaylistTab.PUBLIC -> "No public playlists yet"
                                PlaylistTab.MY_PLAYLISTS -> "You haven't created any playlists yet"
                            },
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedTab) {
                                PlaylistTab.PUBLIC -> "Be the first to create a public playlist!\nShare your favorite music with the community."
                                PlaylistTab.MY_PLAYLISTS -> "Create your first playlist to organize\nyour favorite songs and albums."
                            },
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onCreatePlaylist,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (selectedTab) {
                                    PlaylistTab.PUBLIC -> "Create Public Playlist"
                                    PlaylistTab.MY_PLAYLISTS -> "Create Your First Playlist"
                                }
                            )
                        }
                    }
                }
            } else {
                // Playlists list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stats header
                    item {
                        PlaylistStatsCard(
                            playlists = currentState.playlists,
                            selectedTab = selectedTab
                        )
                    }
                    
                    items(currentState.playlists) { playlist ->
                        EnhancedPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                    
                    // Bottom padding
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
        
        is PlaylistsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = DarkError,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error loading ${selectedTab.displayName.lowercase()}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentState.message,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Retry")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onCreatePlaylist,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
                        ) {
                            Text("Create Playlist")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistStatsCard(
    playlists: List<PublicPlaylist>,
    selectedTab: PlaylistTab
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${playlists.size}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = "Playlists",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${playlists.sumOf { it.songCount }}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = "Total Songs",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            when (selectedTab) {
                PlaylistTab.PUBLIC -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${playlists.count { it.isPublic }}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Text(
                            text = "Public",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                PlaylistTab.MY_PLAYLISTS -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${playlists.count { !it.isPublic }}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Text(
                            text = "Private",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedPlaylistCard(
    playlist: PublicPlaylist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                println("🔍 Card clicked for playlist: ${playlist.name}")
                onClick() 
            },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist icon/image placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Playlist info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playlist.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (playlist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                        contentDescription = if (playlist.isPublic) "Public" else "Private",
                        tint = if (playlist.isPublic) Color.Green else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when {
                        playlist.songCount > 0 -> "${playlist.songCount} songs"
                        else -> "Empty playlist"
                    },
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                
                if (playlist.isOwner) {
                    Text(
                        text = "Created by you",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    playlist.createdBy?.let { createdBy ->
                        Text(
                            text = "Created by $createdBy",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                
                playlist.description?.let { description ->
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreatePlaylist: (String, Boolean, String?) -> Unit,
    isCreating: Boolean
) {
    var playlistName by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    // Validate name in real-time
    LaunchedEffect(playlistName) {
        nameError = when {
            playlistName.isBlank() -> null
            playlistName.length > 100 -> "Name is too long (max 100 characters)"
            else -> null
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create New Playlist",
                    color = TextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Fill in the details for your new playlist:",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Playlist name
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name *") },
                    enabled = !isCreating,
                    isError = nameError != null,
                    supportingText = nameError?.let { 
                        { Text(it, color = MaterialTheme.colorScheme.error) } 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple,
                        cursorColor = PrimaryPurple
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    enabled = !isCreating,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple,
                        cursorColor = PrimaryPurple
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Public/Private toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isPublic) "Public Playlist" else "Private Playlist",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isPublic) "Everyone can see and play this playlist" else "Only you can see this playlist",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        enabled = !isCreating,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryPurple,
                            checkedTrackColor = PrimaryPurple.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistName.isNotBlank() && nameError == null) {
                        onCreatePlaylist(
                            playlistName,
                            isPublic,
                            description.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = !isCreating && playlistName.isNotBlank() && nameError == null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Text("Create Playlist")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}