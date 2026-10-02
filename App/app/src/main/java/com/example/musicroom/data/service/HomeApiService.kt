package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.models.HomeResponse
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * API Service for home screen data
 * Fetches user playlists, recommended songs, popular content, etc.
 */
@Singleton
class HomeApiService @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    /**
     * Fetch home screen data from backend
     */
    suspend fun getHomeData(): Result<HomeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("HomeAPI", "🏠 Fetching home data")
                Log.d("HomeAPI", "🌐 Using base URL: ${NetworkConfig.getCurrentBaseUrl()}")
                
                val fullUrl = NetworkConfig.getFullUrl(NetworkConfig.Endpoints.HOME)
                Log.d("HomeAPI", "📡 Full URL: $fullUrl")
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                // Handle HTTPS for Codespaces
                if (connection is HttpsURLConnection) {
                    Log.d("HomeAPI", "🔒 Using HTTPS connection")
                }
                
                connection.apply {
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")
                    NetworkConfig.applyDeviceHeaders(this)
                    
                    // Add authentication token if available
                    tokenManager.getToken()?.let { token ->
                        setRequestProperty("Authorization", "Bearer $token")
                        Log.d("HomeAPI", "🔑 Added auth token to request")
                    }
                    
                    // Add CORS headers for Codespaces
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("HomeAPI", "📨 Response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("HomeAPI", "📋 Response received (${responseText.length} characters)")
                
                when (responseCode) {
                    200 -> {
                        try {
                            // Parse the JSON response
                            val homeResponse = parseHomeResponse(responseText)
                            Log.d("HomeAPI", "✅ Home data parsed successfully")
                            Result.success(homeResponse)
                        } catch (e: Exception) {
                            Log.e("HomeAPI", "❌ Error parsing home response", e)
                            Result.failure(Exception("Error parsing home data: ${e.message}"))
                        }
                    }
                    401 -> {
                        Log.e("HomeAPI", "❌ Unauthorized - token may be invalid")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    404 -> {
                        Log.e("HomeAPI", "❌ Home endpoint not found")
                        Result.failure(Exception("Home endpoint not found"))
                    }
                    else -> {
                        Log.e("HomeAPI", "❌ Server error: $responseCode")
                        Result.failure(Exception("Server error occurred (Code: $responseCode)"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("HomeAPI", "❌ Network error: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * Parse the JSON response into HomeResponse object
     */
    private fun parseHomeResponse(jsonString: String): HomeResponse {
        val json = JSONObject(jsonString)

        return HomeResponse(
            user_playlists = parsePlaylistsSection(json.optJSONObject("userPlaylists")),
            recommended_songs = parseSongsSection(json.optJSONArray("recommendedSongs")),
            popular_songs = parseSongsSection(json.optJSONArray("popularSongs")),
            recently_listened = parseSongsSection(json.optJSONArray("recentlyListened")),
            popular_artists = parseArtistsSection(json.optJSONArray("popularArtists")),
            events = parseEventsList(json.optJSONArray("events")),
            notifications = parseNotificationsSection(json.optJSONObject("notifications"))
        )
    }
    
    private fun parsePlaylistsSection(json: JSONObject?): com.example.musicroom.data.models.PlaylistsSection {
        if (json == null) {
            return com.example.musicroom.data.models.PlaylistsSection(
                headers = null,
                results = emptyList()
            )
        }
        
        val headers = parseHeaders(json.optJSONObject("headers"))
        val resultsArray = json.optJSONArray("results")
        val playlists = mutableListOf<com.example.musicroom.data.models.Playlist>()
        
        resultsArray?.let { array ->
            for (i in 0 until array.length()) {
                val playlistJson = array.optJSONObject(i)
                playlistJson?.let { 
                    playlists.add(
                        com.example.musicroom.data.models.Playlist(
                            id = it.optString("id"),
                            name = it.optString("name"),
                            creationdate = it.optString("creationdate"),
                            user_id = it.optString("user_id"),
                            user_name = it.optString("user_name"),
                            zip = it.optString("zip"),
                            shorturl = it.optString("shorturl"),
                            shareurl = it.optString("shareurl")
                        )
                    )
                }
            }
        }
        
        return com.example.musicroom.data.models.PlaylistsSection(headers, playlists)
    }
    
    // Backend returns a bare array of Jamendo tracks here (no {headers, results} wrapper).
    private fun parseSongsSection(resultsArray: org.json.JSONArray?): com.example.musicroom.data.models.SongsSection {
        val songs = mutableListOf<com.example.musicroom.data.models.Song>()

        resultsArray?.let { array ->
            for (i in 0 until array.length()) {
                val songJson = array.optJSONObject(i)
                songJson?.let { 
                    songs.add(
                        com.example.musicroom.data.models.Song(
                            id = it.optString("id"),
                            name = it.optString("name"),
                            duration = it.optInt("duration"),
                            artist_id = it.optString("artist_id"),
                            artist_name = it.optString("artist_name"),
                            artist_idstr = it.optString("artist_idstr"),
                            album_name = it.optString("album_name"),
                            album_id = it.optString("album_id"),
                            license_ccurl = it.optString("license_ccurl"),
                            position = it.optInt("position"),
                            releasedate = it.optString("releasedate"),
                            album_image = it.optString("album_image"),
                            audio = it.optString("audio"),
                            audiodownload = it.optString("audiodownload"),
                            prourl = it.optString("prourl"),
                            shorturl = it.optString("shorturl"),
                            shareurl = it.optString("shareurl"),
                            waveform = it.optString("waveform"),
                            image = it.optString("image"),
                            audiodownload_allowed = it.optBoolean("audiodownload_allowed")
                        )
                    )
                }
            }
        }
        
        return com.example.musicroom.data.models.SongsSection(null, songs)
    }

    // Backend returns a bare array here too.
    private fun parseArtistsSection(resultsArray: org.json.JSONArray?): com.example.musicroom.data.models.ArtistsSection {
        val artists = mutableListOf<com.example.musicroom.data.models.Artist>()
        
        resultsArray?.let { array ->
            for (i in 0 until array.length()) {
                val artistJson = array.optJSONObject(i)
                artistJson?.let { 
                    artists.add(
                        com.example.musicroom.data.models.Artist(
                            id = it.optString("id"),
                            name = it.optString("name"),
                            website = it.optString("website"),
                            joindate = it.optString("joindate"),
                            image = it.optString("image"),
                            shorturl = it.optString("shorturl"),
                            shareurl = it.optString("shareurl")
                        )
                    )
                }
            }
        }
        
        return com.example.musicroom.data.models.ArtistsSection(null, artists)
    }
    
    private fun parseEventsList(jsonArray: org.json.JSONArray?): List<com.example.musicroom.data.models.Event> {
        val events = mutableListOf<com.example.musicroom.data.models.Event>()
        
        jsonArray?.let { array ->
            for (i in 0 until array.length()) {
                val eventJson = array.optJSONObject(i)
                eventJson?.let { json ->
                    // Parse organizer
                    val organizerJson = json.optJSONObject("organizer")
                    val organizer = if (organizerJson != null) {
                        com.example.musicroom.data.models.EventOrganizer(
                            id = organizerJson.optString("id"),
                            name = organizerJson.optString("name"),
                            avatar = organizerJson.optString("avatar")
                        )
                    } else {
                        // Fallback: organizer might be a string (organizer name)
                        val organizerName = json.optString("organizer", "Unknown")
                        com.example.musicroom.data.models.EventOrganizer(
                            id = "",
                            name = organizerName,
                            avatar = null
                        )
                    }
                    
                    events.add(
                        com.example.musicroom.data.models.Event(
                            id = json.optString("id"),
                            title = json.optString("title"),
                            description = json.optString("description"),
                            location = json.optString("location"),
                            organizer = organizer,
                            attendeeCount = json.optInt("attendeeCount", 0),
                            trackCount = json.optInt("trackCount", 0),
                            is_public = json.optBoolean("isPublic", true),
                            event_start_time = json.optString("eventStartTime"),
                            event_end_time = json.optString("eventEndTime"),
                            image_url = json.optString("imageUrl"),
                            created_at = json.optString("createdAt"),
                            current_user_role = json.optString("currentUserRole")
                        )
                    )
                }
            }
        }
        
        return events
    }
    
    private fun parseNotificationsSection(json: JSONObject?): com.example.musicroom.data.models.NotificationsSection {
        if (json == null) {
            return com.example.musicroom.data.models.NotificationsSection(
                event_notifications = emptyList(),
                playlist_notifications = emptyList()
            )
        }
        
        val eventNotifications = mutableListOf<com.example.musicroom.data.models.EventNotification>()
        val playlistNotifications = mutableListOf<com.example.musicroom.data.models.PlaylistNotification>()
        
        // Parse event notifications
        val eventNotificationsArray = json.optJSONArray("eventNotifications")
        eventNotificationsArray?.let { array ->
            for (i in 0 until array.length()) {
                val notificationJson = array.optJSONObject(i)
                notificationJson?.let {
                    eventNotifications.add(
                        com.example.musicroom.data.models.EventNotification(
                            event_id = it.optString("eventId"),
                            inviter_name = it.optString("inviterName"),
                            message = it.optString("message"),
                            event_title = it.optString("eventTitle")
                        )
                    )
                }
            }
        }

        // Parse playlist notifications
        val playlistNotificationsArray = json.optJSONArray("playlistNotifications")
        playlistNotificationsArray?.let { array ->
            for (i in 0 until array.length()) {
                val notificationJson = array.optJSONObject(i)
                notificationJson?.let {
                    playlistNotifications.add(
                        com.example.musicroom.data.models.PlaylistNotification(
                            playlist_id = it.optString("playlistId"),
                            inviter_name = it.optString("inviterName"),
                            message = it.optString("message"),
                            playlist_name = it.optString("playlistName")
                        )
                    )
                }
            }
        }
        
        return com.example.musicroom.data.models.NotificationsSection(
            event_notifications = eventNotifications,
            playlist_notifications = playlistNotifications
        )
    }
    
    private fun parseHeaders(json: JSONObject?): com.example.musicroom.data.models.ApiHeaders? {
        return if (json != null) {
            com.example.musicroom.data.models.ApiHeaders(
                status = json.optString("status"),
                code = json.optInt("code"),
                error_message = json.optString("error_message"),
                warnings = json.optString("warnings"),
                results_count = json.optInt("results_count"),
                next = json.optString("next")
            )
        } else {
            null
        }
    }
}