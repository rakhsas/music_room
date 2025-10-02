package com.example.musicroom.presentation.playlists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.service.PlaylistApiService
import com.example.musicroom.data.service.PublicPlaylist
import com.example.musicroom.data.service.CreatePlaylistRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val playlistApiService: PlaylistApiService
) : ViewModel() {
    
    private val _publicPlaylistsState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val publicPlaylistsState: StateFlow<PlaylistsUiState> = _publicPlaylistsState.asStateFlow()
    
    private val _myPlaylistsState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val myPlaylistsState: StateFlow<PlaylistsUiState> = _myPlaylistsState.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(PlaylistTab.PUBLIC)
    val selectedTab: StateFlow<PlaylistTab> = _selectedTab.asStateFlow()
    
    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()
    
    private val _createResult = MutableStateFlow<CreatePlaylistResult?>(null)
    val createResult: StateFlow<CreatePlaylistResult?> = _createResult.asStateFlow()
    
    init {
        Log.d("PlaylistDetailsVM", "🚀 ViewModel initialized, loading all playlists")
        loadAllPlaylists()
    }
    
    fun switchTab(tab: PlaylistTab) {
        Log.d("PlaylistDetailsVM", "🔄 Switching to tab: ${tab.displayName}")
        _selectedTab.value = tab
        
        // Load data if not already loaded or if we want to refresh
        when (tab) {
            PlaylistTab.PUBLIC -> {
                if (_publicPlaylistsState.value is PlaylistsUiState.Loading) {
                    Log.d("PlaylistDetailsVM", "📋 Loading public playlists for first time")
                    loadPublicPlaylists()
                }
            }
            PlaylistTab.MY_PLAYLISTS -> {
                if (_myPlaylistsState.value is PlaylistsUiState.Loading) {
                    Log.d("PlaylistDetailsVM", "📋 Loading my playlists for first time")
                    loadMyPlaylists()
                }
            }
        }
    }
    
    fun loadAllPlaylists() {
        Log.d("PlaylistDetailsVM", "🔄 Loading all playlists (public + my playlists)")
        loadPublicPlaylists()
        loadMyPlaylists()
    }
    
    fun loadPublicPlaylists() {
        viewModelScope.launch {
            try {
                Log.d("PlaylistDetailsVM", "📋 Starting to load public playlists")
                _publicPlaylistsState.value = PlaylistsUiState.Loading
                
                val result = playlistApiService.getPublicPlaylists()
                
                if (result.isSuccess) {
                    val playlists = result.getOrThrow()
                    Log.d("PlaylistDetailsVM", "✅ Successfully loaded ${playlists.size} public playlists")
                    
                    playlists.forEachIndexed { index, playlist ->
                        Log.d("PlaylistDetailsVM", "🎵 Public playlist $index: ${playlist.name}")
                    }
                    
                    _publicPlaylistsState.value = PlaylistsUiState.Success(playlists)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("PlaylistDetailsVM", "❌ Failed to load public playlists", error)
                    _publicPlaylistsState.value = PlaylistsUiState.Error(error?.message ?: "Failed to load public playlists")
                }
                
            } catch (e: Exception) {
                Log.e("PlaylistDetailsVM", "❌ Exception loading public playlists", e)
                _publicPlaylistsState.value = PlaylistsUiState.Error("Error loading public playlists: ${e.message}")
            }
        }
    }
    
    fun loadMyPlaylists() {
        viewModelScope.launch {
            try {
                Log.d("PlaylistDetailsVM", "📋 Starting to load my playlists")
                _myPlaylistsState.value = PlaylistsUiState.Loading
                
                val result = playlistApiService.getMyPlaylists()
                
                if (result.isSuccess) {
                    val playlists = result.getOrThrow()
                    Log.d("PlaylistDetailsVM", "✅ Successfully loaded ${playlists.size} my playlists")
                    
                    playlists.forEachIndexed { index, playlist ->
                        Log.d("PlaylistDetailsVM", "🎵 My playlist $index: ${playlist.name} (${if (playlist.isPublic) "public" else "private"})")
                    }
                    
                    _myPlaylistsState.value = PlaylistsUiState.Success(playlists)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("PlaylistDetailsVM", "❌ Failed to load my playlists", error)
                    _myPlaylistsState.value = PlaylistsUiState.Error(error?.message ?: "Failed to load your playlists")
                }
                
            } catch (e: Exception) {
                Log.e("PlaylistDetailsVM", "❌ Exception loading my playlists", e)
                _myPlaylistsState.value = PlaylistsUiState.Error("Error loading your playlists: ${e.message}")
            }
        }
    }
    
    fun refreshCurrentTab() {
        val currentTab = _selectedTab.value
        Log.d("PlaylistDetailsVM", "🔄 Refreshing current tab: ${currentTab.displayName}")
        
        when (currentTab) {
            PlaylistTab.PUBLIC -> loadPublicPlaylists()
            PlaylistTab.MY_PLAYLISTS -> loadMyPlaylists()
        }
    }
    
    fun refreshAllTabs() {
        Log.d("PlaylistDetailsVM", "🔄 Refreshing all tabs")
        loadAllPlaylists()
    }
    
    fun createPlaylist(playlistName: String, isPublic: Boolean = true, description: String? = null) {
        if (playlistName.isBlank()) {
            Log.w("PlaylistDetailsVM", "❌ Cannot create playlist with empty name")
            _createResult.value = CreatePlaylistResult.Error("Playlist name cannot be empty")
            return
        }
        
        if (playlistName.length > 100) {
            Log.w("PlaylistDetailsVM", "❌ Playlist name too long")
            _createResult.value = CreatePlaylistResult.Error("Playlist name is too long (max 100 characters)")
            return
        }
        
        viewModelScope.launch {
            try {
                _isCreating.value = true
                _createResult.value = null
                Log.d("PlaylistDetailsVM", "🆕 Creating playlist: $playlistName (public: $isPublic)")
                
                val request = CreatePlaylistRequest(
                    name = playlistName.trim(),
                    isPublic = isPublic,
                    description = description?.takeIf { it.isNotBlank() }
                )
                
                val result = playlistApiService.createPlaylist(request)
                
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    Log.d("PlaylistDetailsVM", "✅ Successfully created playlist: ${response.name}")
                    
                    _createResult.value = CreatePlaylistResult.Success(
                        playlistName = response.name,
                        message = response.message ?: "Playlist created successfully!"
                    )
                    
                    // Refresh both tabs to include the new playlist
                    Log.d("PlaylistDetailsVM", "🔄 Refreshing all tabs after playlist creation")
                    refreshAllTabs()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("PlaylistDetailsVM", "❌ Failed to create playlist", error)
                    _createResult.value = CreatePlaylistResult.Error(
                        error?.message ?: "Failed to create playlist"
                    )
                }
                
            } catch (e: Exception) {
                Log.e("PlaylistDetailsVM", "❌ Exception creating playlist", e)
                _createResult.value = CreatePlaylistResult.Error("Error creating playlist: ${e.message}")
            } finally {
                _isCreating.value = false
            }
        }
    }
    
    fun clearCreateResult() {
        _createResult.value = null
    }
}

/**
 * UI State for playlists
 */
sealed class PlaylistsUiState {
    object Loading : PlaylistsUiState()
    data class Success(val playlists: List<PublicPlaylist>) : PlaylistsUiState()
    data class Error(val message: String) : PlaylistsUiState()
}

/**
 * Playlist tabs
 */
enum class PlaylistTab(val displayName: String) {
    PUBLIC("All Public"),
    MY_PLAYLISTS("My Playlists")
}

/**
 * Result state for playlist creation
 */
sealed class CreatePlaylistResult {
    data class Success(val playlistName: String, val message: String) : CreatePlaylistResult()
    data class Error(val message: String) : CreatePlaylistResult()
}