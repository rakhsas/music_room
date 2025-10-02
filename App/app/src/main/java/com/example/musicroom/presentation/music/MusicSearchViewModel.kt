/**
 * ========================================================================================
 * MUSIC SEARCH VIEW MODEL
 * ========================================================================================
 * 
 * Handles music search functionality with real API integration.
 * Connected to backend Jamendo API for dynamic music discovery.
 * 
 * 🎵 CURRENT FUNCTIONALITY:
 * ========================================================================================
 * ✅ Real music search with Jamendo API backend
 * ✅ Loading states and error handling
 * ✅ State management with Kotlin Flow
 * ✅ Coroutine-based async operations
 * 
 * 🔄 BACKEND INTEGRATION:
 * ========================================================================================
 * ✅ Connected to /api/music/songs/ endpoint
 * ✅ Connected to /api/music/random-songs/ endpoint  
 * ✅ Connected to /api/music/related/ endpoint
 * ✅ Real Song data from Jamendo API
 * ✅ Audio URL integration for playback
 * 
 * 📊 STATE MANAGEMENT:
 * ========================================================================================
 * - Empty: Initial state before any search
 * - Loading: During API call
 * - Success: Search completed with results
 * - Error: Search failed or no results found
 * ========================================================================================
 */
package com.example.musicroom.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicSearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    // ============================================================================
    // STATE MANAGEMENT - UI state flow for reactive programming
    // ============================================================================
    private val _uiState = MutableStateFlow<MusicSearchUiState>(MusicSearchUiState.Empty)
    val uiState: StateFlow<MusicSearchUiState> = _uiState.asStateFlow()
    
    // ============================================================================
    // MUSIC SEARCH - Real API Integration
    // ============================================================================
    
    /**
     * Search for tracks using real backend API
     * Now fetches dynamic songs from your Jamendo API backend
     */
    fun searchTracks(query: String) {
        if (query.isBlank()) return // Don't search empty queries
        
        viewModelScope.launch {
            _uiState.value = MusicSearchUiState.Loading
            
            try {
                // Fetch dynamic songs from your backend API
                val result = if (query.lowercase().contains("random")) {
                    musicRepository.getRandomSongs(20)
                } else {
                    // For now, get random songs for any search
                    // You can implement actual search in your backend later
                    musicRepository.getSongsByQuery(query, 20)
                }
                
                result.onSuccess { songs ->
                    if (songs.isEmpty()) {
                        _uiState.value = MusicSearchUiState.Error("No tracks found for '$query'")
                    } else {
                        // Convert Song objects to Track objects for UI
                        val tracks = songs.map { song ->
                            musicRepository.songToTrack(song)
                        }
                        _uiState.value = MusicSearchUiState.Success(tracks)
                    }
                }.onFailure { exception ->
                    _uiState.value = MusicSearchUiState.Error("Search failed: ${exception.message}")
                }
                
            } catch (e: Exception) {
                _uiState.value = MusicSearchUiState.Error("Search failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get popular/trending tracks
     */
    fun getPopularTracks() {
        viewModelScope.launch {
            _uiState.value = MusicSearchUiState.Loading
            
            try {
                val result = musicRepository.getRandomSongs(15)
                
                result.onSuccess { songs ->
                    val tracks = songs.map { song ->
                        musicRepository.songToTrack(song)
                    }
                    _uiState.value = MusicSearchUiState.Success(tracks)
                }.onFailure { exception ->
                    _uiState.value = MusicSearchUiState.Error("Failed to load popular tracks: ${exception.message}")
                }
                
            } catch (e: Exception) {
                _uiState.value = MusicSearchUiState.Error("Failed to load popular tracks: ${e.message}")
            }
        }
    }
    
    /**
     * Get related tracks for a user
     */
    fun getRelatedTracks(userId: Int = 1) {
        viewModelScope.launch {
            _uiState.value = MusicSearchUiState.Loading
            
            try {
                val result = musicRepository.getRelatedSongs(userId, 10)
                
                result.onSuccess { songs ->
                    val tracks = songs.map { song ->
                        musicRepository.songToTrack(song)
                    }
                    _uiState.value = MusicSearchUiState.Success(tracks)
                }.onFailure { exception ->
                    _uiState.value = MusicSearchUiState.Error("Failed to load related tracks: ${exception.message}")
                }
                
            } catch (e: Exception) {
                _uiState.value = MusicSearchUiState.Error("Failed to load related tracks: ${e.message}")
            }
        }
    }
}

/**
 * ============================================================================
 * MUSIC SEARCH UI STATE
 * ============================================================================
 */
sealed class MusicSearchUiState {
    object Empty : MusicSearchUiState()
    object Loading : MusicSearchUiState()
    data class Success(val tracks: List<Track>) : MusicSearchUiState()
    data class Error(val message: String) : MusicSearchUiState()
}
