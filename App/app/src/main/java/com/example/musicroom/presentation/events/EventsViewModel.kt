package com.example.musicroom.presentation.events

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.models.Event
import com.example.musicroom.data.service.EventsApiService
import com.example.musicroom.data.service.CreateEventRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI States for different data types
 */
sealed class EventsUiState {
    object Loading : EventsUiState()
    data class Success(val events: List<Event>) : EventsUiState()
    data class Error(val message: String) : EventsUiState()
}

sealed class CreateEventResult {
    data class Success(val eventId: String, val title: String) : CreateEventResult()
    data class Error(val message: String) : CreateEventResult()
}

enum class EventTab(val displayName: String) {
    PUBLIC("Public Events"),
    MY_EVENTS("My Events")
}

/**
 * ViewModel for Events screen with separate states
 * Similar to PlaylistDetailsViewModel structure
 */
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsApiService: EventsApiService
) : ViewModel() {
    
    // Separate states for each tab
    private val _publicEventsState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val publicEventsState: StateFlow<EventsUiState> = _publicEventsState.asStateFlow()
    
    private val _myEventsState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val myEventsState: StateFlow<EventsUiState> = _myEventsState.asStateFlow()
    
    // Tab management
    private val _selectedTab = MutableStateFlow(EventTab.PUBLIC)
    val selectedTab: StateFlow<EventTab> = _selectedTab.asStateFlow()
    
    // Create event state
    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()
    
    private val _createResult = MutableStateFlow<CreateEventResult?>(null)
    val createResult: StateFlow<CreateEventResult?> = _createResult.asStateFlow()
    
    init {
        loadPublicEvents()
        loadMyEvents()
    }
    
    /**
     * Load public events
     */
    private fun loadPublicEvents() {
        viewModelScope.launch {
            try {
                _publicEventsState.value = EventsUiState.Loading
                Log.d("EventsVM", "📋 Loading public events...")
                
                val result = eventsApiService.getPublicEvents()
                if (result.isSuccess) {
                    val events = result.getOrThrow()
                    Log.d("EventsVM", "✅ Loaded ${events.size} public events")
                    _publicEventsState.value = EventsUiState.Success(events)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("EventsVM", "❌ Failed to load public events: $error")
                    _publicEventsState.value = EventsUiState.Error("Failed to load public events: $error")
                }
                
            } catch (e: Exception) {
                Log.e("EventsVM", "❌ Error loading public events", e)
                _publicEventsState.value = EventsUiState.Error("Failed to load public events: ${e.message}")
            }
        }
    }
    
    /**
     * Load user's events
     */
    private fun loadMyEvents() {
        viewModelScope.launch {
            try {
                _myEventsState.value = EventsUiState.Loading
                Log.d("EventsVM", "📋 Loading my events...")
                
                val result = eventsApiService.getMyEvents()
                if (result.isSuccess) {
                    val events = result.getOrThrow()
                    Log.d("EventsVM", "✅ Loaded ${events.size} my events")
                    _myEventsState.value = EventsUiState.Success(events)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("EventsVM", "❌ Failed to load my events: $error")
                    _myEventsState.value = EventsUiState.Error("Failed to load my events: $error")
                }
                
            } catch (e: Exception) {
                Log.e("EventsVM", "❌ Error loading my events", e)
                _myEventsState.value = EventsUiState.Error("Failed to load my events: ${e.message}")
            }
        }
    }
    
    /**
     * Switch between tabs
     */
    fun switchTab(tab: EventTab) {
        _selectedTab.value = tab
    }
    
    /**
     * Refresh current tab
     */
    fun refreshCurrentTab() {
        when (_selectedTab.value) {
            EventTab.PUBLIC -> loadPublicEvents()
            EventTab.MY_EVENTS -> loadMyEvents()
        }
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        loadPublicEvents()
        loadMyEvents()
    }
    
    /**
     * Create a new event
     */
    fun createEvent(
        title: String,
        description: String?,
        location: String,
        startTime: String?,
        endTime: String?,
        isPublic: Boolean
    ) {
        viewModelScope.launch {
            try {
                _isCreating.value = true
                Log.d("EventsVM", "🎪 Creating event: $title")
                Log.d("EventsVM", "📍 Location: $location")
                Log.d("EventsVM", "⏰ Start time input: $startTime")
                Log.d("EventsVM", "⏰ End time input: $endTime")
                
                // Validate inputs
                if (title.isBlank()) {
                    _createResult.value = CreateEventResult.Error("Event title is required")
                    return@launch
                }
                
                if (location.isBlank()) {
                    _createResult.value = CreateEventResult.Error("Event location is required")
                    return@launch
                }
                
                if (startTime.isNullOrBlank()) {
                    _createResult.value = CreateEventResult.Error("Event start time is required")
                    return@launch
                }
                
                // Format start time properly
                val formattedStartTime = formatDateTime(startTime)
                if (formattedStartTime == null) {
                    _createResult.value = CreateEventResult.Error("Invalid start time format. Please use YYYY-MM-DD HH:MM")
                    return@launch
                }
                
                // Format end time if provided
                val formattedEndTime = if (!endTime.isNullOrBlank()) {
                    val formatted = formatDateTime(endTime)
                    if (formatted == null) {
                        _createResult.value = CreateEventResult.Error("Invalid end time format. Please use YYYY-MM-DD HH:MM")
                        return@launch
                    }
                    formatted
                } else {
                    null
                }
                
                Log.d("EventsVM", "⏰ Formatted start time: $formattedStartTime")
                Log.d("EventsVM", "⏰ Formatted end time: $formattedEndTime")
                
                val request = CreateEventRequest(
                    title = title.trim(),
                    description = description?.trim(),
                    location = location,
                    event_start_time = formattedStartTime,
                    event_end_time = formattedEndTime,
                    is_public = isPublic
                )
                
                Log.d("EventsVM", "📤 Sending create event request")
                val result = eventsApiService.createEvent(request)
                
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    Log.d("EventsVM", "✅ Event created successfully: ${response.eventId}")
                    _createResult.value = CreateEventResult.Success(
                        eventId = response.eventId ?: "",
                        title = response.title ?: title
                    )
                    
                    // Refresh both tabs to show the new event
                    loadPublicEvents()
                    loadMyEvents()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("EventsVM", "❌ Failed to create event: $error")
                    _createResult.value = CreateEventResult.Error(error)
                }
                
            } catch (e: Exception) {
                Log.e("EventsVM", "❌ Error creating event", e)
                _createResult.value = CreateEventResult.Error("Failed to create event: ${e.message}")
            } finally {
                _isCreating.value = false
            }
        }
    }
    
    /**
     * Format datetime string to ISO format
     */
    private fun formatDateTime(dateTimeString: String): String? {
        return try {
            if (dateTimeString.contains("T")) {
                // Already in ISO format
                if (dateTimeString.endsWith("Z")) {
                    dateTimeString
                } else {
                    "${dateTimeString}Z"
                }
            } else {
                // Convert date-time format to ISO
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateTimeString)
                
                val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getTimeZone("UTC")
                outputFormat.format(date ?: return null)
            }
        } catch (e: Exception) {
            Log.w("EventsVM", "⚠️ Could not parse datetime: $dateTimeString", e)
            null
        }
    }
    
    /**
     * Clear create result
     */
    fun clearCreateResult() {
        _createResult.value = null
    }
}