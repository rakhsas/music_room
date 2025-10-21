package com.example.musicroom.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.MusicPlayerService
import com.example.musicroom.data.service.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow 
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ========================================================================================
 * MEDIA PLAYER VIEW MODEL
 * ========================================================================================
 * 
 * Modernized view model for the music player.
 * Manages playback state, controls, and track information.
 * 
 * 🎯 KEY FEATURES:
 * ========================================================================================
 * ✅ Real audio playback with MediaPlayer
 * ✅ Playback controls (play, pause, seek)
 * ✅ Shuffle and repeat modes
 * ✅ Track position and duration tracking
 * ✅ State management with StateFlow
 * ========================================================================================
 */
@HiltViewModel
class MediaPlayerViewModel @Inject constructor(
    private val musicPlayerService: MusicPlayerService
) : ViewModel() {
    
    // Expose music player service states
    val currentTrack: StateFlow<Track?> = musicPlayerService.currentTrack
    val isPlaying: StateFlow<Boolean> = musicPlayerService.isPlaying
    val currentPosition: StateFlow<Float> = musicPlayerService.currentPosition
    val duration: StateFlow<Float> = musicPlayerService.duration
    val isPlayerReady: StateFlow<Boolean> = musicPlayerService.isPlayerReady
    val isShuffleEnabled: StateFlow<Boolean> = musicPlayerService.isShuffleEnabled
    val repeatMode: StateFlow<RepeatMode> = musicPlayerService.repeatMode
    
    /**
     * Play a track with real audio from API
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            musicPlayerService.playTrack(track)
        }
    }
    
    /**
     * Resume playback
     */
    fun play() {
        musicPlayerService.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        musicPlayerService.pause()
    }
    
    /**
     * Seek to a specific position in the track
     */
    fun seekTo(position: Float) {
        musicPlayerService.seekTo(position)
    }
    
    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        musicPlayerService.toggleShuffle()
    }
    
    /**
     * Cycle through repeat modes (OFF → ALL → ONE)
     */
    fun cycleRepeatMode() {
        musicPlayerService.cycleRepeatMode()
    }
    
    /**
     * Clean up resources when view model is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        musicPlayerService.release()
    }
}

