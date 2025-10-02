package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.models.Song
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * API Service for music operations
 * Fetches songs, tracks, and music data from backend
 */
@Singleton
class MusicApiService @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    /**
     * Get random songs from backend
     */
    suspend fun getRandomSongs(limit: Int = 20): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MusicAPI", "🎵 Fetching random songs (limit: $limit)")
                
                val fullUrl = NetworkConfig.BASE_URL + "/api/music/random-songs/?limit=$limit"
                Log.d("MusicAPI", "📡 URL: $fullUrl")
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                // Handle HTTPS for Codespaces
                if (connection is HttpsURLConnection) {
                    Log.d("MusicAPI", "🔒 Using HTTPS connection")
                }
                
                connection.apply {
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")
                    
                    // Add authentication token if available
                    tokenManager.getToken()?.let { token ->
                        setRequestProperty("Authorization", "Bearer $token")
                        Log.d("MusicAPI", "🔑 Added auth token to request")
                    }
                    
                    // Add CORS headers for Codespaces
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("MusicAPI", "📨 Response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("MusicAPI", "📨 Response: ${responseText.take(200)}...")
                
                when (responseCode) {
                    200 -> {
                        val songs = parseSongsResponse(responseText)
                        Log.d("MusicAPI", "✅ Fetched ${songs.size} songs")
                        Result.success(songs)
                    }
                    401 -> {
                        Log.e("MusicAPI", "❌ Unauthorized")
                        Result.failure(Exception("Unauthorized"))
                    }
                    404 -> {
                        Log.e("MusicAPI", "❌ Endpoint not found")
                        Result.failure(Exception("Music API endpoint not found"))
                    }
                    else -> {
                        Log.e("MusicAPI", "❌ Error: $responseCode")
                        Result.failure(Exception("Failed to fetch songs: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MusicAPI", "❌ Network error: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * Get songs list from backend
     */
    suspend fun getSongs(limit: Int = 20): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MusicAPI", "🎵 Fetching songs list (limit: $limit)")
                
                val fullUrl = NetworkConfig.BASE_URL + "/api/music/songs/?limit=$limit"
                Log.d("MusicAPI", "📡 URL: $fullUrl")
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")
                    
                    // Add authentication token if available
                    tokenManager.getToken()?.let { token ->
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("MusicAPI", "📨 Songs response code: $responseCode")
                
                when (responseCode) {
                    200 -> {
                        val songs = parseSongsResponse(responseText)
                        Log.d("MusicAPI", "✅ Fetched ${songs.size} songs from list")
                        Result.success(songs)
                    }
                    else -> {
                        Log.e("MusicAPI", "❌ Error fetching songs: $responseCode")
                        Result.failure(Exception("Failed to fetch songs: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MusicAPI", "❌ Network error fetching songs: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * Get related songs from backend
     */
    suspend fun getRelatedSongs(userId: Int = 1, limit: Int = 10): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MusicAPI", "🎵 Fetching related songs (user: $userId, limit: $limit)")
                
                val fullUrl = NetworkConfig.BASE_URL + "/api/music/related/?user_id=$userId&limit=$limit"
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")
                    
                    tokenManager.getToken()?.let { token ->
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                when (responseCode) {
                    200 -> {
                        val songs = parseSongsResponse(responseText)
                        Log.d("MusicAPI", "✅ Fetched ${songs.size} related songs")
                        Result.success(songs)
                    }
                    else -> {
                        Result.failure(Exception("Failed to fetch related songs: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MusicAPI", "❌ Error fetching related songs: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Search songs by name from backend
     */
    suspend fun searchSongsByName(query: String, limit: Int = 20): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MusicAPI", "🔍 Searching songs by name: '$query' (limit: $limit)")

                // URL encode the query to handle special characters
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val fullUrl = NetworkConfig.BASE_URL + "/api/music/tracks/search-name/$encodedQuery/"
                Log.d("MusicAPI", "📡 URL: $fullUrl")

                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")

                    // Add authentication token if available
                    tokenManager.getToken()?.let { token ->
                        setRequestProperty("Authorization", "Bearer $token")
                        Log.d("MusicAPI", "🔑 Added auth token to request")
                    }

                    // Add CORS headers for Codespaces
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }

                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }

                val responseCode = connection.responseCode
                Log.d("MusicAPI", "📨 Response code: $responseCode")

                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }

                Log.d("MusicAPI", "📨 Response: ${responseText.take(200)}...")

                when (responseCode) {
                    200 -> {
                        val songs = parseSongsResponse(responseText)
                        Log.d("MusicAPI", "✅ Found ${songs.size} songs for query: '$query'")
                        Result.success(songs)
                    }
                    404 -> {
                        Log.w("MusicAPI", "⚠️ No songs found for query: '$query'")
                        Result.success(emptyList()) // Return empty list instead of failure for 404
                    }
                    401 -> {
                        Log.e("MusicAPI", "❌ Unauthorized")
                        Result.failure(Exception("Unauthorized"))
                    }
                    else -> {
                        Log.e("MusicAPI", "❌ Error: $responseCode")
                        Result.failure(Exception("Search failed: HTTP $responseCode"))
                    }
                }

            } catch (e: Exception) {
                Log.e("MusicAPI", "❌ Network error during search: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Parse Jamendo API response to Song objects
     */
    private fun parseSongsResponse(responseText: String): List<Song> {
        return try {
            val json = JSONObject(responseText)
            val resultsArray = json.optJSONArray("results") ?: return emptyList()
            
            val songs = mutableListOf<Song>()
            
            for (i in 0 until resultsArray.length()) {
                val songJson = resultsArray.optJSONObject(i)
                songJson?.let { 
                    try {
                        val song = Song(
                            id = it.optString("id", ""),
                            name = it.optString("name", "Unknown Song"),
                            duration = it.optInt("duration", 0),
                            artist_id = it.optString("artist_id", ""),
                            artist_name = it.optString("artist_name", "Unknown Artist"),
                            artist_idstr = it.optString("artist_idstr", ""),
                            album_name = it.optString("album_name", "Unknown Album"),
                            album_id = it.optString("album_id", ""),
                            license_ccurl = it.optString("license_ccurl"),
                            position = it.optInt("position", 0),
                            releasedate = it.optString("releasedate", ""),
                            album_image = it.optString("album_image"),
                            audio = it.optString("audio", ""),
                            audiodownload = it.optString("audiodownload"),
                            prourl = it.optString("prourl"),
                            shorturl = it.optString("shorturl", ""),
                            shareurl = it.optString("shareurl", ""),
                            waveform = it.optString("waveform"),
                            image = it.optString("image"),
                            audiodownload_allowed = it.optBoolean("audiodownload_allowed", false)
                        )
                        songs.add(song)
                    } catch (e: Exception) {
                        Log.w("MusicAPI", "Failed to parse song at index $i: ${e.message}")
                    }
                }
            }
            
            songs
        } catch (e: Exception) {
            Log.e("MusicAPI", "❌ Error parsing songs response: ${e.message}")
            emptyList()
        }
    }
}