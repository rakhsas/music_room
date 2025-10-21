package com.example.musicroom.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.RepeatMode
import com.example.musicroom.presentation.theme.*
import kotlin.math.roundToInt

/**
 * ========================================================================================
 * MEDIA PLAYER VIEW - Modern Music Player
 * ========================================================================================
 * 
 * Modernized music player with teal/cyan theme and glassmorphism design.
 * Full-screen immersive player with enhanced controls and visualizations.
 * 
 * 🎯 KEY FEATURES:
 * ========================================================================================
 * ✅ Full-screen immersive player
 * ✅ Modern gradient background
 * ✅ Glassmorphism effects
 * ✅ Enhanced playback controls
 * ✅ Visual audio indicator
 * ✅ Playlist and event integration
 * 
 * 🎨 DESIGN UPDATES:
 * ========================================================================================
 * - New teal/cyan gradient theme
 * - Glass morphism controls
 * - Enhanced spacing and typography
 * - Smooth animations
 * - Modern Material Design 3 components
 * ========================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerView(
    track: Track,
    navController: NavController,
    viewModel: MediaPlayerViewModel = hiltViewModel()
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isPlayerReady by viewModel.isPlayerReady.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    var isLiked by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showAddToEventDialog by remember { mutableStateOf(false) }

    // Play track when screen opens
    LaunchedEffect(track) {
        viewModel.playTrack(track)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryTeal.copy(alpha = 0.4f),
                        DarkBackground,
                        DarkBackground,
                        DeepCyan.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top bar
            ModernPlayerTopBar(
                navController = navController,
                onPlaylistAdd = { showAddToPlaylistDialog = true },
                onEventAdd = { showAddToEventDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Album Art with glass effect
            ModernAlbumArt(track = track)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Player status indicator
            PlayerStatusIndicator(
                isPlayerReady = isPlayerReady,
                trackTitle = track.title,
                isPlaying = isPlaying
            )

            // Audio Visualizer
            AudioVisualizer(
                isPlaying = isPlaying,
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Track info with modern card
            ModernTrackInfo(
                track = track,
                isLiked = isLiked,
                onLikeToggle = { isLiked = !isLiked }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar with modern styling
            ModernProgressBar(
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Control buttons with modern design
            ModernControlButtons(
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onPlayPause = {
                    if (isPlaying) viewModel.pause() else viewModel.play()
                },
                onShuffle = { viewModel.toggleShuffle() },
                onRepeat = { viewModel.cycleRepeatMode() },
                onPrevious = { /* TODO: Implement previous track */ },
                onNext = { /* TODO: Implement next track */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom actions
            ModernBottomActions()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            track = track,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showAddToEventDialog) {
        AddToEventDialog(
            track = track,
            onDismiss = { showAddToEventDialog = false }
        )
    }
}

@Composable
private fun ModernPlayerTopBar(
    navController: NavController,
    onPlaylistAdd: () -> Unit,
    onEventAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .background(
                    color = GlassWhite,
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NOW PLAYING",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Text(
                text = "Music Room",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row {
            IconButton(
                onClick = onPlaylistAdd,
                modifier = Modifier
                    .background(
                        color = GlassWhite,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = PrimaryTeal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onEventAdd,
                modifier = Modifier
                    .background(
                        color = GlassWhite,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.Event,
                    contentDescription = "Add to Event",
                    tint = PrimaryTeal
                )
            }
        }
    }
}

@Composable
private fun ModernAlbumArt(track: Track) {
    Card(
        modifier = Modifier
            .size(300.dp)
            .align(Alignment.CenterHorizontally),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun PlayerStatusIndicator(
    isPlayerReady: Boolean,
    trackTitle: String,
    isPlaying: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isPlayerReady) {
                if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote
            } else {
                Icons.Default.HourglassEmpty
            },
            contentDescription = null,
            tint = if (isPlayerReady) PrimaryTeal else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (isPlayerReady) {
                if (isPlaying) "Playing" else "Paused"
            } else {
                "Loading..."
            },
            color = if (isPlayerReady) PrimaryTeal else TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModernTrackInfo(
    track: Track,
    isLiked: Boolean,
    onLikeToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GlassWhite
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isLiked) PrimaryTeal.copy(alpha = 0.2f) else GlassWhite,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) PrimaryTeal else TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernProgressBar(
    currentPosition: Float,
    duration: Float,
    onSeek: (Float) -> Unit
) {
    Column {
        Slider(
            value = if (duration > 0) currentPosition else 0f,
            onValueChange = onSeek,
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryTeal,
                activeTrackColor = PrimaryTeal,
                inactiveTrackColor = GlassWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(duration),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModernControlButtons(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        ModernControlButton(
            icon = Icons.Filled.Shuffle,
            isActive = isShuffleEnabled,
            onClick = onShuffle,
            size = 48.dp
        )

        // Previous
        ModernControlButton(
            icon = Icons.Filled.SkipPrevious,
            size = 56.dp,
            onClick = onPrevious
        )
        
        // Play/Pause - Main button
        Card(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = PrimaryTeal
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Next
        ModernControlButton(
            icon = Icons.Filled.SkipNext,
            size = 56.dp,
            onClick = onNext
        )
        
        // Repeat
        ModernControlButton(
            icon = when (repeatMode) {
                RepeatMode.OFF -> Icons.Filled.Repeat
                RepeatMode.ALL -> Icons.Filled.Repeat
                RepeatMode.ONE -> Icons.Filled.RepeatOne
            },
            isActive = repeatMode != RepeatMode.OFF,
            onClick = onRepeat,
            size = 48.dp
        )
    }
}

@Composable
private fun ModernControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isActive) PrimaryTeal.copy(alpha = 0.3f) else GlassWhite
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isActive) PrimaryTeal else TextSecondary,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun ModernBottomActions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { /* Connect to device */ },
            modifier = Modifier
                .background(
                    color = GlassWhite,
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Filled.Devices,
                contentDescription = "Connect",
                tint = TextSecondary
            )
        }

        IconButton(
            onClick = { /* Share */ },
            modifier = Modifier
                .background(
                    color = GlassWhite,
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = "Share",
                tint = TextSecondary
            )
        }
        
        IconButton(
            onClick = { /* More options */ },
            modifier = Modifier
                .background(
                    color = GlassWhite,
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = TextSecondary
            )
        }
    }
}

private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.roundToInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

