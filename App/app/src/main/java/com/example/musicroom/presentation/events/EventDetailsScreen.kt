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
import com.example.musicroom.data.models.Song
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.EventsApiService
import com.example.musicroom.data.service.MusicPlayerService
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
        val isAttending: Boolean,
        val tracksLoadFailed: Boolean = false
    ) : EventDetailsUiState()
    data class Error(val message: String) : EventDetailsUiState()
}

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val eventsApiService: EventsApiService,
    private val musicPlayerService: MusicPlayerService
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
                    val tracks = tracksResult.getOrNull() ?: emptyList()
                    val tracksLoadFailed = tracksResult.isFailure
                    if (tracksLoadFailed) {
                        Log.w("EventDetailsVM", "⚠️ Failed to load tracks: ${tracksResult.exceptionOrNull()?.message}")
                    }

                    Log.d("EventDetailsVM", "✅ Event details loaded: ${eventDetails.title} with ${tracks.size} tracks (ATTENDING)")
                    _uiState.value = EventDetailsUiState.Success(eventDetails, tracks, true, tracksLoadFailed)
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
    
    fun voteForTrack(eventId: String, trackId: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("EventDetailsVM", "👍 Voting for track: $trackId")
                val result = eventsApiService.voteForTrack(eventId, trackId)
                if (result.isSuccess) {
                    Log.d("EventDetailsVM", "✅ Successfully voted for track")
                    refreshVoteCounts(eventId)
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Failed to vote"
                    Log.e("EventDetailsVM", "❌ Failed to vote for track: $message")
                    onError(message)
                }
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error voting for track", e)
                onError(e.message ?: "Failed to vote")
            }
        }
    }

    fun unvoteForTrack(eventId: String, trackId: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("EventDetailsVM", "👎 Removing vote for track: $trackId")
                val result = eventsApiService.unvoteForTrack(eventId, trackId)
                if (result.isSuccess) {
                    Log.d("EventDetailsVM", "✅ Successfully removed vote for track")
                    refreshVoteCounts(eventId)
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Failed to remove vote"
                    Log.e("EventDetailsVM", "❌ Failed to remove vote for track: $message")
                    onError(message)
                }
            } catch (e: Exception) {
                Log.e("EventDetailsVM", "❌ Error removing vote for track", e)
                onError(e.message ?: "Failed to remove vote")
            }
        }
    }

    /** Re-fetches just the track/vote list in place, without dropping to the full-screen Loading state. */
    private fun refreshTracks(eventId: String) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is EventDetailsUiState.Success) return@launch
            eventsApiService.getEventTracksWithVotes(eventId)
                .onSuccess { tracks -> _uiState.value = current.copy(tracks = tracks, tracksLoadFailed = false) }
                .onFailure {
                    Log.w("EventDetailsVM", "⚠️ Failed to refresh tracks: ${it.message}")
                    // Keep whatever tracks were already showing - just flag that a fresh load failed,
                    // instead of wiping the list (which used to look like "this event has 0 tracks").
                    _uiState.value = current.copy(tracksLoadFailed = true)
                }
        }
    }

    /** Retry hook for the "couldn't load tracks" state. */
    fun retryLoadTracks(eventId: String) {
        refreshTracks(eventId)
    }

    /**
     * Patches vote counts/hasUserVoted into the already-loaded track list after a vote/unvote.
     * Deliberately does NOT call [refreshTracks] - track metadata (title/artist/art/audio) is
     * fetched from Jamendo and never changes from a vote, so re-fetching it on every tap would
     * just be hammering Jamendo for no reason.
     */
    private fun refreshVoteCounts(eventId: String) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is EventDetailsUiState.Success) return@launch
            eventsApiService.getEventVoteData(eventId).onSuccess { voteData ->
                val updatedTracks = current.tracks.map { track ->
                    val info = voteData[track.id]
                    track.copy(voteCount = info?.first ?: 0, hasUserVoted = info?.second ?: false)
                }
                _uiState.value = current.copy(tracks = updatedTracks)
            }.onFailure {
                Log.w("EventDetailsVM", "⚠️ Failed to refresh vote counts: ${it.message}")
            }
        }
    }

    /**
     * Adds [song] (already fetched from Jamendo by the search dialog) to the event and appends
     * it straight to the cached track list - no need to hit Jamendo again just to re-learn the
     * metadata of a track we already have in hand.
     */
    fun addTrack(eventId: String, song: Song, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            eventsApiService.addTrackToEvent(eventId, song.id).fold(
                onSuccess = {
                    val current = _uiState.value
                    if (current is EventDetailsUiState.Success && current.tracks.none { it.id == song.id }) {
                        _uiState.value = current.copy(tracks = current.tracks + song.toTrack())
                    }
                    onSuccess()
                },
                onFailure = { onError(it.message ?: "Failed to add track") }
            )
        }
    }
    
    fun refresh(eventId: String) {
        loadEventDetails(eventId)
    }

    fun updateEvent(
        eventId: String,
        request: com.example.musicroom.data.service.CreateEventRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            eventsApiService.updateEvent(eventId, request).fold(
                onSuccess = {
                    loadEventDetails(eventId)
                    onSuccess()
                },
                onFailure = { onError(it.message ?: "Failed to update event") }
            )
        }
    }

    fun deleteEvent(eventId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            eventsApiService.deleteEvent(eventId).fold(
                onSuccess = { onSuccess() },
                onFailure = { onError(it.message ?: "Failed to delete event") }
            )
        }
    }

    /** Starts playback of the clicked track, queuing the rest of the event's tracks for Previous/Next. */
    fun playQueue(tracks: List<Track>, startIndex: Int) {
        musicPlayerService.setQueueAndPlay(tracks, startIndex)
    }
}

@Composable
fun EventDetailsScreen(
    eventId: String,
    navController: NavController,
    viewModel: EventDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSavingEvent by remember { mutableStateOf(false) }
    var isDeletingEvent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                        tracksLoadFailed = currentState.tracksLoadFailed,
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
                            viewModel.voteForTrack(eventId, trackId, onError = { error -> errorMessage = error })
                        },
                        onUnvoteTrack = { trackId ->
                            Log.d("EventDetailsScreen", "👎 Unvote for track: $trackId")
                            viewModel.unvoteForTrack(eventId, trackId, onError = { error -> errorMessage = error })
                        },
                        onEditEvent = { showEditDialog = true },
                        onDeleteEvent = { showDeleteDialog = true },
                        onPlayTrack = { index -> viewModel.playQueue(currentState.tracks, index) },
                        onAddTrack = { song, onSuccess, onError ->
                            viewModel.addTrack(eventId, song, onSuccess, onError)
                        },
                        onRetryTracks = { viewModel.retryLoadTracks(eventId) },
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

    val currentEvent = (uiState as? EventDetailsUiState.Success)?.event

    if (showEditDialog && currentEvent != null) {
        CreateEventDialog(
            isCreating = isSavingEvent,
            dialogTitle = "Edit Event",
            confirmButtonText = "Save Changes",
            confirmButtonLoadingText = "Saving...",
            initialTitle = currentEvent.title,
            initialLocation = currentEvent.location,
            initialDescription = currentEvent.description ?: "",
            initialIsPublic = currentEvent.is_public,
            initialStartTime = currentEvent.event_start_time,
            initialEndTime = currentEvent.event_end_time,
            onCreateEvent = { title, location, description, isPublic, startTime, endTime ->
                isSavingEvent = true
                viewModel.updateEvent(
                    eventId = eventId,
                    request = com.example.musicroom.data.service.CreateEventRequest(
                        title = title,
                        description = description,
                        location = location,
                        event_start_time = startTime,
                        event_end_time = endTime,
                        is_public = isPublic
                    ),
                    onSuccess = {
                        isSavingEvent = false
                        showEditDialog = false
                    },
                    onError = { error ->
                        isSavingEvent = false
                        errorMessage = error
                        showEditDialog = false
                    }
                )
            },
            onDismiss = { if (!isSavingEvent) showEditDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingEvent) showDeleteDialog = false },
            title = { Text("Delete Event", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete this event? This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingEvent = true
                        viewModel.deleteEvent(
                            eventId = eventId,
                            onSuccess = {
                                isDeletingEvent = false
                                showDeleteDialog = false
                                navController.popBackStack()
                            },
                            onError = { error ->
                                isDeletingEvent = false
                                errorMessage = error
                                showDeleteDialog = false
                            }
                        )
                    },
                    enabled = !isDeletingEvent
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeletingEvent
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                actionOnNewLine = true,
                containerColor = Color.Red.copy(alpha = 0.9f),
                contentColor = Color.White
            ) {
                Text(text = error, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun EventDetailsContent(
    event: Event,
    tracks: List<Track>,
    tracksLoadFailed: Boolean,
    isAttending: Boolean,
    onJoinEvent: () -> Unit,
    onLeaveEvent: () -> Unit,
    onVoteTrack: (String) -> Unit,
    onUnvoteTrack: (String) -> Unit,
    onEditEvent: () -> Unit,
    onDeleteEvent: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onAddTrack: (song: Song, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onRetryTracks: () -> Unit,
    navController: NavController
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showAddTrackDialog by remember { mutableStateOf(false) }
    
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
                            ),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isAttending)
                                    Icons.Default.ExitToApp
                                else
                                    Icons.Default.PersonAdd,
                                contentDescription = if (isAttending) "Leave event" else "Join event",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
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
                                    Log.d("EventDetailsScreen", "✏️ Edit event clicked")
                                    onEditEvent()
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
                                    Log.d("EventDetailsScreen", "🗑️ Delete event clicked")
                                    onDeleteEvent()
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                IconButton(
                                    onClick = { showAddTrackDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add track",
                                        tint = PrimaryPurple
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (tracks.isEmpty() && tracksLoadFailed) {
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
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Couldn't load tracks",
                                        tint = Color.Red,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Couldn't load tracks",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "This event may still have tracks - check your connection and try again",
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onRetryTracks,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                    ) {
                                        Text("Try Again")
                                    }
                                }
                            }
                        } else if (tracks.isEmpty()) {
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
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddTrackDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add a Track")
                                    }
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
                                                onPlayTrack(index)
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

    // Add Track Dialog
    if (showAddTrackDialog) {
        AddEventTrackDialog(
            onDismiss = { showAddTrackDialog = false },
            onAddTrack = onAddTrack
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
                            text = "${event.attendeeCount} attending",
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
                            text = "${event.trackCount} tracks",
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
    val outputFormat = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault())
    val inputPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (pattern in inputPatterns) {
        try {
            val parsed = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).parse(dateTimeString)
            if (parsed != null) return outputFormat.format(parsed)
        } catch (e: java.text.ParseException) {
            // try next pattern
        }
    }
    return dateTimeString.take(19).replace("T", " at ")
}

private fun Song.toTrack(): Track = Track(
    id = id,
    title = name,
    artist = artist_name,
    thumbnailUrl = image ?: album_image ?: "",
    duration = String.format("%d:%02d", duration / 60, duration % 60),
    channelTitle = album_name,
    description = audio
)