package com.example.musicroom.presentation.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.musicroom.data.models.Event
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.EventsApiService
import com.example.musicroom.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State for Add to Event
sealed class AddToEventUiState {
    object Loading : AddToEventUiState()
    data class Success(val events: List<Event>) : AddToEventUiState()
    data class Error(val message: String) : AddToEventUiState()
}

sealed class AddTrackToEventResult {
    object Loading : AddTrackToEventResult()
    data class Success(val message: String) : AddTrackToEventResult()
    data class Error(val message: String) : AddTrackToEventResult()
}

// ViewModel for Add to Event functionality
@HiltViewModel
class AddToEventViewModel @Inject constructor(
    private val eventsApiService: EventsApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddToEventUiState>(AddToEventUiState.Loading)
    val uiState: StateFlow<AddToEventUiState> = _uiState.asStateFlow()
    
    private val _addTrackResult = MutableStateFlow<AddTrackToEventResult?>(null)
    val addTrackResult: StateFlow<AddTrackToEventResult?> = _addTrackResult.asStateFlow()
    
    init {
        loadMyEvents()
    }
    
    fun loadMyEvents() {
        viewModelScope.launch {
            try {
                _uiState.value = AddToEventUiState.Loading
                Log.d("AddToEventVM", "🔄 Loading my events for track addition")
                
                val result = eventsApiService.getMyEvents()
                
                if (result.isSuccess) {
                    val myEvents: List<Event> = result.getOrThrow()
                    _uiState.value = AddToEventUiState.Success(myEvents)
                    Log.d("AddToEventVM", "✅ Loaded ${myEvents.size} my events")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("AddToEventVM", "❌ Failed to load my events", error)
                    _uiState.value = AddToEventUiState.Error(error?.message ?: "Failed to load events")
                }
                
            } catch (e: Exception) {
                Log.e("AddToEventVM", "❌ Failed to load my events", e)
                _uiState.value = AddToEventUiState.Error("Failed to load events: ${e.message}")
            }
        }
    }
    
    fun addTrackToEvent(track: Track, event: Event) {
        viewModelScope.launch {
            try {
                _addTrackResult.value = AddTrackToEventResult.Loading
                Log.d("AddToEventVM", "🎵 Adding track ${track.title} (ID: ${track.id}) to event ${event.title} (ID: ${event.id})")
                
                val result = eventsApiService.addTrackToEvent(
                    eventId = event.id,
                    trackId = track.id
                )
                
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    _addTrackResult.value = AddTrackToEventResult.Success(response.message)
                    Log.d("AddToEventVM", "✅ Track added to event successfully: ${response.message}")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("AddToEventVM", "❌ Failed to add track to event", error)
                    _addTrackResult.value = AddTrackToEventResult.Error(error?.message ?: "Failed to add track to event")
                }
                
            } catch (e: Exception) {
                Log.e("AddToEventVM", "❌ Exception adding track to event", e)
                _addTrackResult.value = AddTrackToEventResult.Error("Error adding track to event: ${e.message}")
            }
        }
    }
    
    fun clearAddTrackResult() {
        _addTrackResult.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToEventDialog(
    track: Track,
    onDismiss: () -> Unit,
    viewModel: AddToEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val addTrackResult by viewModel.addTrackResult.collectAsState()
    
    // Handle add track result
    LaunchedEffect(addTrackResult) {
        addTrackResult?.let { result ->
            when (result) {
                is AddTrackToEventResult.Success -> {
                    kotlinx.coroutines.delay(1500)
                    viewModel.clearAddTrackResult()
                    onDismiss()
                }
                is AddTrackToEventResult.Error -> {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearAddTrackResult()
                }
                is AddTrackToEventResult.Loading -> {
                    // Keep dialog open during loading
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Track to My Event",
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
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Track",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = track.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "by ${track.artist}",
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
                        is AddTrackToEventResult.Success -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = result.message,
                                        color = Color.Green,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        is AddTrackToEventResult.Error -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = result.message,
                                        color = DarkError,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        is AddTrackToEventResult.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryPurple,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adding track to event...",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                // My Events list
                when (val currentState = uiState) {
                    is AddToEventUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PrimaryPurple)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading your events...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    is AddToEventUiState.Success -> {
                        if (currentState.events.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "No events",
                                        modifier = Modifier.size(48.dp),
                                        tint = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No events found",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Create an event first to add tracks",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // Events list header
                            Text(
                                text = "Select an event to add this track:",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Events list
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentState.events) { event ->
                                    EventItemCard(
                                        event = event,
                                        onAddTrack = { viewModel.addTrackToEvent(track, event) },
                                        isLoading = addTrackResult is AddTrackToEventResult.Loading
                                    )
                                }
                            }
                        }
                    }
                    
                    is AddToEventUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    modifier = Modifier.size(48.dp),
                                    tint = DarkError
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Failed to load events",
                                    color = DarkError,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = currentState.message,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.loadMyEvents() },
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
    }
}

@Composable
private fun EventItemCard(
    event: Event,
    onAddTrack: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onAddTrack() },
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (event.is_public) Icons.Default.Public else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (event.is_public) Color.Green else Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${event.track_count} tracks",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${event.attendee_count} attending",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• ${event.location}",
                            color = PrimaryPurple,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    color = PrimaryPurple,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to event",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}