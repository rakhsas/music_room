package com.example.musicroom.presentation.artist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.models.Song
import com.example.musicroom.data.models.Artist
import com.example.musicroom.data.service.MusicApiService
import com.example.musicroom.data.service.HomeApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val musicApiService: MusicApiService,
    private val homeApiService: HomeApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ArtistDetailsUiState>(ArtistDetailsUiState.Loading)
    val uiState: StateFlow<ArtistDetailsUiState> = _uiState.asStateFlow()

    fun loadArtistDetails(artistId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ArtistDetailsUiState.Loading
                
                // Fetch real artist data from backend
                val artist = fetchArtistById(artistId)
                if (artist == null) {
                    _uiState.value = ArtistDetailsUiState.Error("Artist not found")
                    return@launch
                }
                
                // Get artist songs - for now using random songs and filtering by artist name
                // TODO: Replace with actual artist-specific endpoint when backend supports it
                val songsResult = musicApiService.getRandomSongs()
                
                if (songsResult.isSuccess) {
                    val allSongs: List<Song> = songsResult.getOrThrow()
                    
                    // Filter songs by artist name (placeholder logic)
                    // In a real app, this would be done on the backend
                    val artistSongs: List<Song> = allSongs.filter { song: Song ->
                        song.artist_name.contains(artist.name, ignoreCase = true) ||
                        artist.name.contains(song.artist_name, ignoreCase = true)
                    }.ifEmpty {
                        // If no matching songs found, take first 5 random songs as placeholder
                        allSongs.take(5)
                    }
                    
                    Log.d("ArtistDetailsVM", "Loaded ${artistSongs.size} songs for artist ${artist.name}")
                    
                    if (artistSongs.isNotEmpty()) {
                        _uiState.value = ArtistDetailsUiState.Success(artist, artistSongs)
                    } else {
                        _uiState.value = ArtistDetailsUiState.Error("No songs found for this artist")
                    }
                } else {
                    val error = songsResult.exceptionOrNull()
                    Log.e("ArtistDetailsVM", "Failed to fetch songs", error)
                    _uiState.value = ArtistDetailsUiState.Error("Failed to load artist songs: ${error?.message}")
                }
                
            } catch (e: Exception) {
                Log.e("ArtistDetailsVM", "Error loading artist details", e)
                _uiState.value = ArtistDetailsUiState.Error("Failed to load artist details: ${e.message}")
            }
        }
    }
    
    /**
     * Fetch real artist data by ID from the backend's popular artists
     */
    private suspend fun fetchArtistById(artistId: String): Artist? {
        return try {
            Log.d("ArtistDetailsVM", "🎤 Fetching real artist data for ID: $artistId")
            
            // Get home data which includes popular artists
            val homeResult = homeApiService.getHomeData()
            
            if (homeResult.isSuccess) {
                val homeData = homeResult.getOrThrow()
                val popularArtists = homeData.popular_artists.results
                
                Log.d("ArtistDetailsVM", "📋 Found ${popularArtists.size} popular artists")
                
                // Find the artist by ID
                val artist = popularArtists.find { it.id == artistId }
                
                if (artist != null) {
                    Log.d("ArtistDetailsVM", "✅ Found artist: ${artist.name}")
                } else {
                    Log.w("ArtistDetailsVM", "❌ Artist with ID $artistId not found in popular artists")
                }
                
                artist
            } else {
                val error = homeResult.exceptionOrNull()
                Log.e("ArtistDetailsVM", "Failed to fetch home data for artist lookup", error)
                null
            }
        } catch (e: Exception) {
            Log.e("ArtistDetailsVM", "Error fetching artist by ID", e)
            null
        }
    }
}