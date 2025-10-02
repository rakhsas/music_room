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

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
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
    
    fun play() {
        musicPlayerService.play()
    }
    
    fun pause() {
        musicPlayerService.pause()
    }
    
    fun seekTo(position: Float) {
        musicPlayerService.seekTo(position)
    }
    
    fun toggleShuffle() {
        musicPlayerService.toggleShuffle()
    }
    
    fun cycleRepeatMode() {
        musicPlayerService.cycleRepeatMode()
    }
    
    override fun onCleared() {
        super.onCleared()
        musicPlayerService.release()
    }
}
