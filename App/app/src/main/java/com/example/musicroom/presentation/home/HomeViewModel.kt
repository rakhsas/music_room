package com.example.musicroom.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.models.HomeResponse
import com.example.musicroom.data.service.HomeApiService
import com.example.musicroom.data.service.EventsApiService
import com.example.musicroom.data.service.PlaylistApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: HomeResponse) : HomeUiState()
    data class Error(val message: String, val isAuthError: Boolean = false) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeApiService: HomeApiService,
    private val eventsApiService: EventsApiService,
    private val playlistApiService: PlaylistApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    /**
     * Load home screen data from backend
     */
    fun loadHomeData() {
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Loading
                Log.d("HomeViewModel", "🏠 Loading home data...")
                
                val result = homeApiService.getHomeData()
                
                if (result.isSuccess) {
                    val homeData = result.getOrNull()!!
                    Log.d("HomeViewModel", "✅ Home data loaded successfully")
                    Log.d("HomeViewModel", "📊 Playlists: ${homeData.user_playlists.results.size}")
                    Log.d("HomeViewModel", "🎵 Recommended songs: ${homeData.recommended_songs.results.size}")
                    Log.d("HomeViewModel", "🔥 Popular songs: ${homeData.popular_songs.results.size}")
                    Log.d("HomeViewModel", "🎤 Popular artists: ${homeData.popular_artists.results.size}")
                    Log.d("HomeViewModel", "🔔 Event notifications: ${homeData.notifications.event_notifications.size}")
                    Log.d("HomeViewModel", "📋 Playlist notifications: ${homeData.notifications.playlist_notifications.size}")
                    
                    _uiState.value = HomeUiState.Success(homeData)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("HomeViewModel", "❌ Failed to load home data: $error")
                    val isAuthError = error.contains("Authentication required", ignoreCase = true)
                    if (isAuthError) tokenManager.clearTokens()
                    _uiState.value = HomeUiState.Error(error, isAuthError)
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Unexpected error loading home data: ${e.message}")
                _uiState.value = HomeUiState.Error("An unexpected error occurred")
            }
        }
    }
    
    /**
     * Accept event invitation
     */
    fun acceptEventInvitation(eventId: String) {
        viewModelScope.launch {
            try {
                _actionInProgress.value = true
                Log.d("HomeViewModel", "✅ Accepting event invitation for event: $eventId")
                
                val result = eventsApiService.acceptEventInvitation(eventId)
                
                if (result.isSuccess) {
                    val message = result.getOrNull() ?: "Invitation accepted successfully"
                    Log.d("HomeViewModel", "✅ Event invitation accepted: $message")
                    
                    // Reload home data to refresh notifications
                    loadHomeData()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to accept invitation"
                    Log.e("HomeViewModel", "❌ Failed to accept event invitation: $error")
                    // You might want to show an error message to the user here
                }
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error accepting event invitation: ${e.message}")
            } finally {
                _actionInProgress.value = false
            }
        }
    }
    
    /**
     * Decline event invitation
     */
    fun declineEventInvitation(eventId: String, inviterName: String) {
        viewModelScope.launch {
            try {
                _actionInProgress.value = true
                Log.d("HomeViewModel", "❌ Declining event invitation for event: $eventId from $inviterName")
                
                val result = eventsApiService.declineEventInvitation(eventId)
                
                if (result.isSuccess) {
                    val message = result.getOrNull() ?: "Invitation declined successfully"
                    Log.d("HomeViewModel", "✅ Event invitation declined: $message")
                    
                    // Reload home data to refresh notifications
                    loadHomeData()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to decline invitation"
                    Log.e("HomeViewModel", "❌ Failed to decline event invitation: $error")
                    // You might want to show an error message to the user here
                }
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error declining event invitation: ${e.message}")
            } finally {
                _actionInProgress.value = false
            }
        }
    }
    
    /**
     * Accept playlist invitation (placeholder for future implementation)
     */
    fun acceptPlaylistInvitation(playlistId: String) {
        viewModelScope.launch {
            try {
                _actionInProgress.value = true
                Log.d("HomeViewModel", "✅ Accepting playlist invitation for playlist: $playlistId")

                val result = playlistApiService.acceptPlaylistInvitation(playlistId)
                if (result.isFailure) {
                    Log.e("HomeViewModel", "❌ Failed to accept playlist invitation: ${result.exceptionOrNull()?.message}")
                }

                // Reload home data to refresh notifications
                loadHomeData()

            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error accepting playlist invitation: ${e.message}")
            } finally {
                _actionInProgress.value = false
            }
        }
    }
    
    /**
     * Decline playlist invitation (placeholder for future implementation)
     */
    fun declinePlaylistInvitation(playlistId: String, inviterName: String) {
        viewModelScope.launch {
            try {
                _actionInProgress.value = true
                Log.d("HomeViewModel", "❌ Declining playlist invitation for playlist: $playlistId from $inviterName")

                val result = playlistApiService.declinePlaylistInvitation(playlistId)
                if (result.isFailure) {
                    Log.e("HomeViewModel", "❌ Failed to decline playlist invitation: ${result.exceptionOrNull()?.message}")
                }

                // Reload home data to refresh notifications
                loadHomeData()

            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error declining playlist invitation: ${e.message}")
            } finally {
                _actionInProgress.value = false
            }
        }
    }
    
    /**
     * Refresh home data
     */
    fun refreshHomeData() {
        loadHomeData()
    }
}