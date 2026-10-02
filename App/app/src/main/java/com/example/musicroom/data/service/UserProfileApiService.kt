package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for user profile operations
 */
@Singleton
class UserProfileApiService @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    /**
     * Get current user profile
     */
    suspend fun getUserProfile(): Result<UserProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserProfileAPI", "🔍 Fetching user profile")
                
                val url = URL("${NetworkConfig.BASE_URL}/api/users/profile/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {

                        setRequestProperty("Authorization", "Bearer $token")
                        Log.d("UserProfileAPI", "🔑 Added authorization header" + token)
                    } else {
                        Log.w("UserProfileAPI", "⚠️ No authentication token available")
                        return@withContext Result.failure(Exception("Authentication required"))
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("UserProfileAPI", "📨 Profile response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("UserProfileAPI", "📨 Profile response: $responseText")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val userProfile = parseUserProfileResponse(responseText)
                            Log.d("UserProfileAPI", "✅ Successfully parsed user profile")
                            Result.success(userProfile)
                        } catch (e: Exception) {
                            Log.e("UserProfileAPI", "❌ Error parsing user profile response", e)
                            Result.failure(Exception("Failed to parse user profile: ${e.message}"))
                        }
                    }
                    401 -> {
                        Log.e("UserProfileAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    else -> {
                        Log.e("UserProfileAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to fetch user profile: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("UserProfileAPI", "❌ Network error fetching user profile", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(request: UpdateUserProfileRequest): Result<UserProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserProfileAPI", "📝 Updating user profile")
                
                val requestBody = JSONObject().apply {
                    request.fullName?.let { put("full_name", it) }
                    request.userName?.let { put("userName", it) }
                    request.bio?.let { put("bio", it) }
                    request.dateOfBirth?.let { put("date_of_birth", it) }
                    request.phoneNumber?.let { put("phone_number", it) }
                    request.profilePrivacy?.let { put("profile_privacy", it) }
                    request.emailPrivacy?.let { put("email_privacy", it) }
                    request.phonePrivacy?.let { put("phone_privacy", it) }

                    request.musicPreferences?.let { prefs ->
                        val prefsArray = JSONArray()
                        prefs.forEach { prefsArray.put(it) }
                        put("music_preferences", prefsArray)
                    }

                    request.likedArtists?.let { artists ->
                        val artistsArray = JSONArray()
                        artists.forEach { artistsArray.put(it) }
                        put("liked_artists", artistsArray)
                    }

                    request.likedAlbums?.let { albums ->
                        val albumsArray = JSONArray()
                        albums.forEach { albumsArray.put(it) }
                        put("liked_albums", albumsArray)
                    }

                    request.likedSongs?.let { songs ->
                        val songsArray = JSONArray()
                        songs.forEach { songsArray.put(it) }
                        put("liked_songs", songsArray)
                    }

                    request.genres?.let { genres ->
                        val genresArray = JSONArray()
                        genres.forEach { genresArray.put(it) }
                        put("genres", genresArray)
                    }
                }.toString()
                
                Log.d("UserProfileAPI", "📤 Update request body: $requestBody")
                
                val url = URL("${NetworkConfig.BASE_URL}/api/users/profile/update/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    } else {
                        return@withContext Result.failure(Exception("Authentication required"))
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                // Write request body
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("UserProfileAPI", "📨 Update response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("UserProfileAPI", "📨 Update response: $responseText")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val userProfile = parseUserProfileResponse(responseText)
                            Log.d("UserProfileAPI", "✅ Successfully updated user profile")
                            Result.success(userProfile)
                        } catch (e: Exception) {
                            Log.e("UserProfileAPI", "❌ Error parsing update response", e)
                            Result.failure(Exception("Failed to parse update response: ${e.message}"))
                        }
                    }
                    400 -> {
                        Log.e("UserProfileAPI", "❌ Bad request: $responseText")
                        val errorMessage = try {
                            val errorJson = JSONObject(responseText)
                            errorJson.optString("error", "Invalid profile data")
                        } catch (e: Exception) {
                            "Invalid profile data"
                        }
                        Result.failure(Exception(errorMessage))
                    }
                    401 -> {
                        Log.e("UserProfileAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    else -> {
                        Log.e("UserProfileAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to update profile: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("UserProfileAPI", "❌ Network error updating user profile", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Parse user profile response from JSON
     */
    private fun parseUserProfileResponse(responseText: String): UserProfileResponse {
        Log.e("PARSE USER PROFILE", responseText)
        try {
            val json = JSONObject(responseText)
            
            // Parse music preferences (backend stores either a dict of genre->weight or a plain list)
            val musicPrefsJson = json.optJSONObject("musicPreferences")
            val musicPreferences = if (musicPrefsJson != null) {
                val keys = musicPrefsJson.keys()
                val prefs = mutableListOf<String>()
                while (keys.hasNext()) {
                    prefs.add(keys.next())
                }
                prefs
            } else {
                parseJsonArray(json.optJSONArray("musicPreferences"))
            }

            // Parse arrays
            val likedArtists = parseJsonArray(json.optJSONArray("likedArtists"))
            val likedAlbums = parseJsonArray(json.optJSONArray("likedAlbums"))
            val likedSongs = parseJsonArray(json.optJSONArray("likedSongs"))
            val genres = parseJsonArray(json.optJSONArray("genres"))

            return UserProfileResponse(
                id = json.optInt("id"),
                email = json.optString("email", ""),
                fullName = json.optString("fullName", ""),
                userName = json.optString("userName").takeIf { it != "null" } ?: "",
                avatar = json.optString("avatar", ""),
                bio = json.optString("bio", ""),
                dateOfBirth = json.optString("dateOfBirth").takeIf { it != "null" },
                phoneNumber = json.optString("phoneNumber", ""),
                profilePrivacy = json.optString("profilePrivacy", "public"),
                emailPrivacy = json.optString("emailPrivacy", "friends"),
                phonePrivacy = json.optString("phonePrivacy", "private"),
                googleId = json.optString("googleId").takeIf { it != "null" },
                subscriptionType = json.optString("subscriptionType", "free"),
                isPremium = json.optBoolean("isPremium", false),
                isSubscribed = json.optBoolean("isSubscribed", false),
                musicPreferences = musicPreferences,
                likedArtists = likedArtists,
                likedAlbums = likedAlbums,
                likedSongs = likedSongs,
                genres = genres,
                createdAt = json.optString("createdAt", "")
            )
        } catch (e: Exception) {
            Log.e("UserProfileAPI", "❌ Error parsing user profile JSON", e)
            throw e
        }
    }
    
    /**
     * Helper function to parse JSON arrays
     */
    private fun parseJsonArray(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}

/**
 * Data classes for API requests and responses
 */
data class UserProfileResponse(
    val id: Int,
    val email: String,
    val fullName: String,
    val userName: String,
    val avatar: String,
    val bio: String,
    val dateOfBirth: String?,
    val phoneNumber: String,
    val profilePrivacy: String,
    val emailPrivacy: String,
    val phonePrivacy: String,
    val googleId: String?,
    val subscriptionType: String,
    val isPremium: Boolean,
    val isSubscribed: Boolean,
    val musicPreferences: List<String>,
    val likedArtists: List<String>,
    val likedAlbums: List<String>,
    val likedSongs: List<String>,
    val genres: List<String>,
    val createdAt: String
)

data class UpdateUserProfileRequest(
    val fullName: String? = null,
    val userName: String? = null,
    val bio: String? = null,
    val dateOfBirth: String? = null, // Format: YYYY-MM-DD
    val phoneNumber: String? = null,
    val profilePrivacy: String? = null, // "public", "friends", "private"
    val emailPrivacy: String? = null,   // "public", "friends", "private"
    val phonePrivacy: String? = null,   // "public", "friends", "private"
    val musicPreferences: List<String>? = null,
    val likedArtists: List<String>? = null,
    val likedAlbums: List<String>? = null,
    val likedSongs: List<String>? = null,
    val genres: List<String>? = null
)