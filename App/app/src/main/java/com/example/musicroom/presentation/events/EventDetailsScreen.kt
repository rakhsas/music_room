package com.example.musicroom.presentation.events

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicroom.data.models.Event
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.EventsApiService
import com.example.musicroom.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

// UI State for Event Details - Updated
sealed class EventDetailsUiState {
    object Loading : EventDetailsUiState()
    data class Success(
        val event: Event, 
        val tracks: List<Track>,
        val isAttending: Boolean
    ) : EventDetailsUiState()
    data class Error(val message: String) : EventDetailsUiState()
}

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val eventsApiService: EventsApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EventDetailsUiState>(EventDetailsUiState.Loading)
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()
    
    fun loadEventDetails(eventId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = EventDetailsUiState.Loading
                Log.d("EventDetailsVM", "🎪 Loading event details for ID: $eventId")
                
                // Get event details
                val eventResult = eventsApiService.getEventDetails(eventId)
                if (eventResult.isFailure) {
                    Log.e("EventDetailsVM", "❌ Failed to load event details: ${eventResult.exceptionOrNull()?.message}")
                    _uiState.value = EventDetailsUiState.Error("Failed to load event details")
                    return@launch
                }
                
                val eventDetails = eventResult.getOrThrow()
                Log.d("EventDetailsVM", "🔍 Event role analysis:")
                Log.d("EventDetailsVM", "   - current_user_role: '${eventDetails.current_user_role}'")
                
                val isAttending = when (eventDetails.current_user_role) {
                    "owner", "editor", "attendee", "listener" -> {
                        Log.d("EventDetailsVM", "     -> User IS attending (role: ${eventDetails.current_user_role})")
                        Log.d("EventDetailsVM", "     -> Should show LEAVE button and LOAD tracks")
                        true
                    }
                    null, "", "none" -> {
                        Log.d("EventDetailsVM", "     -> User NOT attending (role: ${eventDetails.current_user_role})")
                        Log.d("EventDetailsVM", "     -> Should show JOIN button and NOT load tracks")
                        false
                    }
                    else -> {
                        Log.d("EventDetailsVM", "     -> Unknown role: ${eventDetails.current_user_role}")
                        false
                    }
                }
                
                if (isAttending) {
                    // Load tracks only if attending
                    val tracksResult = eventsApiService.getEventTracksWithVotes(eventId)
                    val tracks = if (tracksResult.isSuccess) {
                        tracksResult.getOrThrow()
                    } else {
                        Log.w("EventDetailsVM", "⚠️ Failed to load tracks: ${tracksResult.exceptionOrNull()?.message}")
                        emptyList()
                    }
                    
                    Log.d("EventDetailsVM", "✅ Event details loaded: ${eventDetails.title} with ${tracks.size} tracks (ATTENDING)")
                    _uiState.value = EventDetailsUiState.Success(eventDetails, tracks, true)
                } else {
                    // User not attending, don't load tracks
                    Log.d("EventDetailsVM", "✅ Event details loaded: ${eventDetails.title} - User not attending")
                    _uiState.value = EventDetailsUiState.Success(eventDetails, emptyList(), false)
                }
                
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error loading event details", e)
                _uiState.value = EventDetailsUiState.Error("Failed to load event details: ${e.message}")
            }
        }
    }
    
    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            try {
                Log.d("EventDetailsVM", "🎪 Joining event: $eventId")
                val result = eventsApiService.joinEvent(eventId)
                if (result.isSuccess) {
                    Log.d("EventDetailsVM", "✅ Successfully joined event")
                    // Reload event details to get updated status and tracks
                    loadEventDetails(eventId)
                } else {
                    Log.e("EventDetailsVM", "❌ Failed to join event: ${result.exceptionOrNull()?.message}")
                    // Could show error state here
                }
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error joining event", e)
            }
        }
    }
    
    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            try {
                Log.d("EventDetailsVM", "🚪 Leaving event: $eventId")
                val result = eventsApiService.leaveEvent(eventId)
                if (result.isSuccess) {
                    Log.d("EventDetailsVM", "✅ Successfully left event")
                    // Reload event details to get updated status
                    loadEventDetails(eventId)
                } else {
                    Log.e("EventDetailsVM", "❌ Failed to leave event: ${result.exceptionOrNull()?.message}")
                    // Could show error state here
                }
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error leaving event", e)
            }
        }
    }
    
    fun voteForTrack(eventId: String, trackId: String) {
        viewModelScope.launch {
            try {
                Log.d("EventDetailsVM", "👍 Voting for track: $trackId")
                val result = eventsApiService.voteForTrack(eventId, trackId)
                if (result.isSuccess) {
                    Log.d("EventDetailsVM", "✅ Successfully voted for track")
                    // Reload event details to get updated vote counts
                    loadEventDetails(eventId)
                } else {
                    Log.e("EventDetailsVM", "❌ Failed to vote for track: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error voting for track", e)
            }
        }
    }
    
    fun unvoteForTrack(eventId: String, trackId: String) {
        viewModelScope.launch {
            try {
                Log.d("EventDetailsVM", "👎 Removing vote for track: $trackId")
                val result = eventsApiService.unvoteForTrack(eventId, trackId)
                if (result.isSuccess) {
                    Log.d("EventDetailsVM", "✅ Successfully removed vote for track")
                    // Reload event details to get updated vote counts
                    loadEventDetails(eventId)
                } else {
                    Log.e("EventDetailsVM", "❌ Failed to remove vote for track: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error removing vote for track", e)
            }
        }
    }
    
    fun refresh(eventId: String) {
        loadEventDetails(eventId)
    }
}

@Composable
fun EventDetailsScreen(
    eventId: String,
    navController: NavController,
    viewModel: EventDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(eventId) {
        viewModel.loadEventDetails(eventId)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = uiState) {
            is EventDetailsUiState.Loading -> {
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
                            text = "Loading event details...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            is EventDetailsUiState.Success -> {
                Column {
                    // Top bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground)
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = currentState.event.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Event details content
                    EventDetailsContent(
                        event = currentState.event,
                        tracks = currentState.tracks,
                        isAttending = currentState.isAttending,
                        onJoinEvent = { 
                            Log.d("EventDetailsScreen", "🎪 Join button clicked")
                            viewModel.joinEvent(eventId) 
                        },
                        onLeaveEvent = { 
                            Log.d("EventDetailsScreen", "🚪 Leave button clicked")
                            viewModel.leaveEvent(eventId) 
                        },
                        onVoteTrack = { trackId ->
                            Log.d("EventDetailsScreen", "👍 Vote for track: $trackId")
                            viewModel.voteForTrack(eventId, trackId)
                        },
                        onUnvoteTrack = { trackId ->
                            Log.d("EventDetailsScreen", "👎 Unvote for track: $trackId")
                            viewModel.unvoteForTrack(eventId, trackId)
                        },
                        navController = navController
                    )
                }
            }
            
            is EventDetailsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error",
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh(eventId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailsContent(
    event: Event,
    tracks: List<Track>,
    isAttending: Boolean,
    onJoinEvent: () -> Unit,
    onLeaveEvent: () -> Unit,
    onVoteTrack: (String) -> Unit,
    onUnvoteTrack: (String) -> Unit,
    navController: NavController
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event Header
        item {
            EventHeaderCard(event = event)
        }
        
        // Event Info
        item {
            EventInfoCard(event = event)
        }
        
        // Invite Users Card (for private events and owners/editors)
        if (!event.is_public && (event.current_user_role == "owner" || event.current_user_role == "editor")) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Private Event",
                                    tint = Color(0xFFFFA500), // Orange color
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Private Event",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Invite users to join this private event",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        
                        Button(
                            onClick = { showInviteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Invite Users",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Invite",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // Join/Leave Event Button (for non-owners)
        if (event.current_user_role != "owner") { 
            item {
                // Add debug logging
                Log.d("EventDetailsScreen", "🔍 UI Button Logic:")
                Log.d("EventDetailsScreen", "   - event.current_user_role: '${event.current_user_role}'")
                Log.d("EventDetailsScreen", "   - isAttending: $isAttending")
                Log.d("EventDetailsScreen", "   - Should show: ${if (isAttending) "LEAVE (Red)" else "JOIN (Purple)"} button")
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAttending) "You're attending this event" else "Join this event",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isAttending) "Access to tracks and event features" else "Join to access tracks and participate",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        
                        Button(
                            onClick = if (isAttending) onLeaveEvent else onJoinEvent,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAttending) Color.Red else PrimaryPurple
                            )
                        ) {
                            Text(
                                text = if (isAttending) "Leave" else "Join",
                                color = Color.White
                            )
                            Log.d("EventDetailsScreen", "🔘 Button rendered: '${if (isAttending) "Leave" else "Join"}' (${if (isAttending) "Red" else "Purple"})")
                        }
                    }
                }
            }
        }

        // Event Management Options (for owners only)
        if (event.current_user_role == "owner") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Event Management",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "You are the organizer of this event",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Edit Event Button
                            OutlinedButton(
                                onClick = { 
                                    // TODO: Navigate to edit event screen
                                    Log.d("EventDetailsScreen", "✏️ Edit event clicked")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PrimaryPurple
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                            
                            // Delete Event Button
                            OutlinedButton(
                                onClick = { 
                                    // TODO: Show delete confirmation dialog
                                    Log.d("EventDetailsScreen", "🗑️ Delete event clicked")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
        
        // Tracks Section - Only show if attending or is owner
        if (isAttending || event.current_user_role == "owner") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Event Tracks",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ThumbUp,
                                    contentDescription = "Votes",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${tracks.size} tracks",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (tracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "No tracks",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No tracks yet",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Tracks will appear here when added to the event",
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tracks.forEachIndexed { index, track ->
                                    EventTrackRow(
                                        track = track,
                                        position = index + 1,
                                        eventId = event.id,
                                        currentUserRole = event.current_user_role,
                                        onTrackClick = {
                                            try {
                                                navigateToNowPlaying(navController, track)
                                            } catch (e: Exception) {
                                                Log.e("EventDetailsScreen", "❌ Navigation error", e)
                                            }
                                        },
                                        onVoteClick = onVoteTrack,
                                        onUnvoteClick = onUnvoteTrack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Show message for non-attending users
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Join to see tracks",
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Join the event to see tracks",
                                color = TextSecondary,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "You need to join this event to view and vote on tracks",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Invite Users Dialog
    if (showInviteDialog) {
        InviteUsersDialog(
            eventId = event.id,
            eventTitle = event.title,
            onDismiss = { showInviteDialog = false }
        )
    }
}

@Composable
private fun EventTrackRow(
    track: Track,
    position: Int,
    eventId: String,
    currentUserRole: String?,
    onTrackClick: () -> Unit,
    onVoteClick: (String) -> Unit,
    onUnvoteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
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
                if (track.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.thumbnailUrl)
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
                    text = track.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Vote section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Vote count
                if (track.voteCount > 0) {
                    Text(
                        text = track.voteCount.toString(),
                        color = if (track.hasUserVoted) PrimaryPurple else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Vote button - only show if user is attending
                if (currentUserRole in listOf("owner", "editor", "attendee", "listener")) {
                    IconButton(
                        onClick = {
                            if (track.hasUserVoted) {
                                onUnvoteClick(track.id)
                            } else {
                                onVoteClick(track.id)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (track.hasUserVoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = if (track.hasUserVoted) "Remove vote" else "Vote",
                            tint = if (track.hasUserVoted) PrimaryPurple else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Duration
            Text(
                text = track.duration,
                color = TextSecondary,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Play icon
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play track",
                tint = PrimaryPurple,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EventHeaderCard(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.title,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Organizer",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "by ${event.organizer.name}",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Event status badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (event.is_public) 
                            PrimaryPurple.copy(alpha = 0.2f) 
                        else 
                            Color.Red.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (event.is_public) "Public" else "Private",
                        color = if (event.is_public) PrimaryPurple else Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventInfoCard(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Description
            if (!event.description.isNullOrBlank()) {
                Text(
                    text = "Description",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Event details
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = event.location,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
                
                // Start time
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Start time",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatEventDateTime(event.event_start_time),
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
                
                // Attendees and tracks count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Attendees",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${event.attendee_count} attending",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Tracks",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${event.track_count} tracks",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Helper function for navigation
private fun navigateToNowPlaying(navController: NavController, track: Track) {
    try {
        val encodedTitle = URLEncoder.encode(track.title, "UTF-8")
        val encodedArtist = URLEncoder.encode(track.artist, "UTF-8")
        val encodedDescription = URLEncoder.encode(track.description, "UTF-8")
        val encodedDuration = URLEncoder.encode(track.duration, "UTF-8")
        val encodedThumbnail = URLEncoder.encode(track.thumbnailUrl, "UTF-8")
        navController.navigate(
            "now_playing/${track.id}/$encodedTitle/$encodedArtist/$encodedThumbnail/$encodedDuration/$encodedDescription"
        )
    } catch (e: Exception) {
        Log.e("EventDetailsScreen", "❌ Navigation error: ${e.message}")
    }
}

// Helper function to format date time
private fun formatEventDateTime(dateTimeString: String): String {
    // TODO: Implement proper date/time formatting
    return dateTimeString.take(19).replace("T", " at ") // Simple formatting for now
}