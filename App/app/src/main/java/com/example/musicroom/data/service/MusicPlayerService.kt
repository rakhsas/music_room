package com.example.musicroom.data.service

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.musicroom.BuildConfig
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.network.NetworkConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val freeAudioService: FreeAudioService,
    private val deviceApiService: DeviceApiService
) {
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    // Music Control Delegation: this device's stable id, registered to the current
    // account so friends can be granted play/pause/skip control over it.
    private val localDeviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"
    }
    private var deviceRegistered = false

    init {
        startControlDelegationPolling()
    }
    
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    // Playback queue (V.2.4): the list the current track was played from, so Previous/Next can
    // step through it. playTrack() re-syncs the index whenever the track is found in this list,
    // so callers that never set a queue just get a single-item one (no prev/next).
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    private val _queueIndex = MutableStateFlow(-1)
    val hasPrevious: StateFlow<Boolean> = _queueIndex
        .map { it > 0 }
        .stateIn(serviceScope, SharingStarted.Eagerly, false)
    val hasNext: StateFlow<Boolean> = combine(_queue, _queueIndex) { queue, index ->
        index in 0 until (queue.size - 1)
    }.stateIn(serviceScope, SharingStarted.Eagerly, false)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(180f)
    val duration: StateFlow<Float> = _duration.asStateFlow()
    
    private val _isPlayerReady = MutableStateFlow(false)
    val isPlayerReady: StateFlow<Boolean> = _isPlayerReady.asStateFlow()
    
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    
    /** Sets the list the given track was picked from, then plays it - enables Previous/Next. */
    fun setQueueAndPlay(tracks: List<Track>, startIndex: Int) {
        _queue.value = tracks
        _queueIndex.value = startIndex
        playTrack(tracks[startIndex], syncQueue = false)
    }

    fun playTrack(track: Track, syncQueue: Boolean = true) {
        if (syncQueue) {
            val indexInQueue = _queue.value.indexOfFirst { it.id == track.id }
            if (indexInQueue >= 0) {
                _queueIndex.value = indexInQueue
            } else {
                _queue.value = listOf(track)
                _queueIndex.value = 0
            }
        }

        _currentTrack.value = track
        _currentPosition.value = 0f
        _isPlayerReady.value = false

        Log.d("MusicPlayer", "🎵 Loading track: ${track.title} by ${track.artist}")
        Log.d("MusicPlayer", "🔍 Track ID: ${track.id}")
        
        // Release existing MediaPlayer
        releaseMediaPlayer()
        
        serviceScope.launch {
            try {
                // First, try to get audio URL from track description (where we store the audio URL)
                val audioUrl = when {
                    // If track has audio URL in description field (our API songs)
                    track.description.startsWith("http") -> {
                        Log.d("MusicPlayer", "🎧 Using API audio URL from track description")
                        track.description
                    }
                    // If track ID indicates it's from API (not mock)
                    !track.id.startsWith("mock_") -> {
                        Log.d("MusicPlayer", "🎧 Fetching audio URL from API for track ID: ${track.id}")
                        getJamendoAudioUrl(track) ?: freeAudioService.findFreeAudioForTrack(track)
                    }
                    // This is a mock track, use free audio service
                    else -> {
                        Log.d("MusicPlayer", "🎭 Using free audio service for mock track")
                        freeAudioService.findFreeAudioForTrack(track)
                    }
                }
                
                if (audioUrl != null) {
                    Log.d("MusicPlayer", "🎧 Found audio URL: $audioUrl")
                    playAudioFromUrl(audioUrl, track)
                } else {
                    Log.w("MusicPlayer", "⚠️ No audio found, using simulation")
                    startSimulatedPlayback()
                }
                
            } catch (e: Exception) {
                Log.e("MusicPlayer", "❌ Error loading track: ${e.message}")
                startSimulatedPlayback()
            }
        }
    }
    
    private suspend fun playAudioFromUrl(audioUrl: String, track: Track) {
        withContext(Dispatchers.Main) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    
                    setOnPreparedListener { mp ->
                        val durationSeconds = if (mp.duration > 0) {
                            mp.duration.toFloat() / 1000f
                        } else {
                            180f // Default 3 minutes if unknown
                        }
                        
                        _duration.value = durationSeconds
                        _isPlayerReady.value = true
                        mp.start()
                        _isPlaying.value = true
                        startProgressTracking()
                        
                        Log.d("MusicPlayer", "✅ Playing: ${track.title} (${durationSeconds}s)")
                    }
                    
                    setOnCompletionListener {
                        _isPlaying.value = false
                        handleTrackCompletion()
                        Log.d("MusicPlayer", "🏁 Track completed: ${track.title}")
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e("MusicPlayer", "❌ MediaPlayer error: what=$what, extra=$extra")
                        // Fallback to simulation
                        serviceScope.launch { startSimulatedPlayback() }
                        true
                    }
                    
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("MusicPlayer", "❌ Error creating MediaPlayer: ${e.message}")
                startSimulatedPlayback()
            }
        }
    }
    
    private fun startSimulatedPlayback() {
        _isPlayerReady.value = true
        _isPlaying.value = true
        _duration.value = 180f
        startProgressTracking()
        Log.d("MusicPlayer", "🎭 Started simulation mode")
    }
    
    fun play() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _isPlaying.value = true
                startProgressTracking()
                Log.d("MusicPlayer", "▶️ Resumed real playback")
            }
        } ?: run {
            _isPlaying.value = true
            startProgressTracking()
            Log.d("MusicPlayer", "▶️ Resumed simulation")
        }
    }
    
    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                progressJob?.cancel()
                Log.d("MusicPlayer", "⏸️ Paused real playback")
            }
        } ?: run {
            _isPlaying.value = false
            progressJob?.cancel()
            Log.d("MusicPlayer", "⏸️ Paused simulation")
        }
    }
    
    fun seekTo(position: Float) {
        mediaPlayer?.let { mp ->
            val positionMs = (position * 1000).toInt()
            mp.seekTo(positionMs)
            _currentPosition.value = position
            Log.d("MusicPlayer", "⏩ Real seek to: ${position}s")
        } ?: run {
            _currentPosition.value = position
            Log.d("MusicPlayer", "⏩ Simulation seek to: ${position}s")
        }
    }
    
    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPosition.value = mp.currentPosition.toFloat() / 1000f
                    }
                } ?: run {
                    // Simulation mode
                    val currentPos = _currentPosition.value
                    val duration = _duration.value
                    if (currentPos < duration) {
                        _currentPosition.value = currentPos + 0.1f
                    } else {
                        handleTrackCompletion()
                        return@launch
                    }
                }
                delay(100)
            }
        }
    }
    
    /** Plays the next track in the queue, if any. */
    fun playNext() {
        val queue = _queue.value
        val nextIndex = _queueIndex.value + 1
        if (nextIndex < queue.size) {
            _queueIndex.value = nextIndex
            playTrack(queue[nextIndex], syncQueue = false)
        } else if (_repeatMode.value == RepeatMode.ALL && queue.isNotEmpty()) {
            _queueIndex.value = 0
            playTrack(queue[0], syncQueue = false)
        }
    }

    /** Plays the previous track in the queue, if any. */
    fun playPrevious() {
        val queue = _queue.value
        val prevIndex = _queueIndex.value - 1
        if (prevIndex >= 0) {
            _queueIndex.value = prevIndex
            playTrack(queue[prevIndex], syncQueue = false)
        } else if (_repeatMode.value == RepeatMode.ALL && queue.isNotEmpty()) {
            _queueIndex.value = queue.size - 1
            playTrack(queue[queue.size - 1], syncQueue = false)
        }
    }

    private fun handleTrackCompletion() {
        _isPlaying.value = false
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentPosition.value = 0f
                serviceScope.launch {
                    delay(500)
                    _currentTrack.value?.let { playTrack(it, syncQueue = false) }
                }
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                _currentPosition.value = 0f
                if (hasNext.value || _repeatMode.value == RepeatMode.ALL) {
                    serviceScope.launch {
                        delay(500)
                        playNext()
                    }
                }
            }
        }
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error releasing MediaPlayer: ${e.message}")
            }
            mediaPlayer = null
        }
        progressJob?.cancel()
    }
    
    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        Log.d("MusicPlayer", "🔀 Shuffle: ${_isShuffleEnabled.value}")
    }
    
    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        Log.d("MusicPlayer", "🔁 Repeat: ${_repeatMode.value}")
    }
    
    fun release() {
        releaseMediaPlayer()
    }

    /** Skip the current track: advances the queue if there's a next track, else just stops. */
    fun skip() {
        Log.d("MusicPlayer", "⏭️ Skip requested")
        if (hasNext.value) {
            playNext()
        } else {
            releaseMediaPlayer()
            _isPlaying.value = false
            _currentPosition.value = 0f
        }
    }

    /**
     * Music Control Delegation (V.2.2): registers this device once, then polls the backend
     * for commands a delegate has sent and applies them locally. Runs for the lifetime of the
     * app process since this service is an app-wide singleton.
     */
    private fun startControlDelegationPolling() {
        serviceScope.launch {
            while (true) {
                try {
                    if (!deviceRegistered) {
                        val name = "${Build.MANUFACTURER} ${Build.MODEL}"
                        deviceApiService.registerDevice(localDeviceId, name, "Android", BuildConfig.VERSION_NAME)
                            .onSuccess { deviceRegistered = true }
                    }

                    if (deviceRegistered) {
                        deviceApiService.getPendingCommands(localDeviceId)
                            .onSuccess { commands ->
                                commands.forEach { cmd ->
                                    Log.d("MusicPlayer", "🕹️ Applying delegated command: ${cmd.command} from ${cmd.issuedByName}")
                                    when (cmd.command) {
                                        "play" -> play()
                                        "pause" -> pause()
                                        "skip" -> skip()
                                    }
                                }
                            }
                            // Not registered under the currently logged-in user (e.g. logged out,
                            // or switched accounts on this device) - re-register on the next tick.
                            .onFailure { deviceRegistered = false }
                    }
                } catch (e: Exception) {
                    Log.e("MusicPlayer", "Control delegation poll failed: ${e.message}")
                }
                delay(5000)
            }
        }
    }
    
    /**
     * Get Jamendo audio URL for a track from API
     */
    private suspend fun getJamendoAudioUrl(track: Track): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Call your backend API to get the track details with audio URL
                val url = URL("${NetworkConfig.BASE_URL}/api/music/songs/${track.id}/")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    NetworkConfig.applyDeviceHeaders(this)
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val audioUrl = json.optString("audio")
                    
                    if (audioUrl.isNotEmpty()) {
                        Log.d("MusicPlayer", "🎵 Got Jamendo audio URL: $audioUrl")
                        return@withContext audioUrl
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Failed to get Jamendo audio URL: ${e.message}")
            }
            null
        }
    }
}

enum class RepeatMode {
    OFF, ALL, ONE
}
