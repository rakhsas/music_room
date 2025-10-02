package com.example.musicroom.data.repository

import com.example.musicroom.data.models.Song
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.service.MusicApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val musicApiService: MusicApiService
) {
    
    /**
     * Get random songs from API
     */
    suspend fun getRandomSongs(limit: Int = 20): Result<List<Song>> {
        return musicApiService.getRandomSongs(limit)
    }
    
    /**
     * Get songs list from API
     */
    suspend fun getSongs(limit: Int = 20): Result<List<Song>> {
        return musicApiService.getSongs(limit)
    }

    suspend fun getSongsByQuery(query: String, limit: Int): Result<List<Song>> {
        return musicApiService.searchSongsByName(query, limit)
    }
    
    /**
     * Get related songs from API
     */
    suspend fun getRelatedSongs(userId: Int = 1, limit: Int = 10): Result<List<Song>> {
        return musicApiService.getRelatedSongs(userId, limit)
    }
    
    /**
     * Convert Song to Track for playback
     */
    fun songToTrack(song: Song): Track {
        return Track(
            id = song.id,
            title = song.name,
            artist = song.artist_name,
            thumbnailUrl = song.image ?: song.album_image ?: "",
            duration = formatDuration(song.duration),
            channelTitle = song.album_name,
            description = song.audio // Store the audio URL in description field
        )
    }
    
    /**
     * Format duration from seconds to MM:SS
     */
    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }
}