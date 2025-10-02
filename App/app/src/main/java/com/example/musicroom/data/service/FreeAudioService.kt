package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreeAudioService @Inject constructor() {
    
    private val jamendoClientId = "56d30c95" // Free client ID for demo
    
    suspend fun findFreeAudioForTrack(track: Track): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Try different sources in order of preference
                findFromJamendo(track) 
                    ?: findFromFreeAudio(track)
                    ?: getDefaultSample()
            } catch (e: Exception) {
                Log.e("FreeAudioService", "Error finding audio: ${e.message}")
                getDefaultSample()
            }
        }
    }
    
    private suspend fun findFromJamendo(track: Track): String? {
        return withContext(Dispatchers.IO) {
            try {
                val query = URLEncoder.encode("${track.title} ${track.artist}", "UTF-8")
                val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=1&search=$query&include=musicinfo"
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val results = json.getJSONArray("results")
                    
                    if (results.length() > 0) {
                        val firstTrack = results.getJSONObject(0)
                        val audioUrl = firstTrack.getString("audio")
                        Log.d("FreeAudioService", "Found Jamendo track: $audioUrl")
                        return@withContext audioUrl
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("FreeAudioService", "Jamendo search failed: ${e.message}")
            }
            null
        }
    }
    
    private fun findFromFreeAudio(track: Track): String? {
        // Match track names with known free audio samples
        val trackName = track.title.lowercase()
        
        return when {
            trackName.contains("kalimba") || trackName.contains("classic") -> 
                "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3"
            
            trackName.contains("jazz") || trackName.contains("smooth") -> 
                "https://commondatastorage.googleapis.com/codelabs-media/music.mp3"
                
            trackName.contains("electronic") || trackName.contains("beat") -> 
                "https://file-examples.com/storage/fe68c9db8c566c2b7d9b43f/2017/11/file_example_MP3_700KB.mp3"
                
            trackName.contains("acoustic") || trackName.contains("guitar") -> 
                "https://www2.cs.uic.edu/~i101/SoundFiles/BabyElephantWalk60.wav"
                
            else -> null
        }
    }
    
    private fun getDefaultSample(): String {
        // Curated list of free sample tracks
        val samples = listOf(
            "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3",
            "https://commondatastorage.googleapis.com/codelabs-media/music.mp3",
            "https://file-examples.com/storage/fe68c9db8c566c2b7d9b43f/2017/11/file_example_MP3_700KB.mp3"
        )
        return samples.random()
    }
    
    suspend fun validateAudioUrl(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == HttpURLConnection.HTTP_OK
            } catch (e: Exception) {
                false
            }
        }
    }
}