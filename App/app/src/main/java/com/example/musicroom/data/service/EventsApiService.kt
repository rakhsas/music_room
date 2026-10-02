package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.models.Event
import com.example.musicroom.data.models.EventOrganizer
import com.example.musicroom.data.models.Track
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * API Service for Events operations
 * Handles event listing, creation, and management
 */
@Singleton
class EventsApiService @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    /**
     * Get all public events
     */
    suspend fun getPublicEvents(location: String? = null): Result<List<Event>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "📋 Fetching public events${location?.let { " for location: $it" } ?: ""}")
                
                val baseUrl = NetworkConfig.BASE_URL + "/api/events/"
                val urlWithParams = if (location != null) {
                    "$baseUrl?location=$location"
                } else {
                    baseUrl
                }
                
                val connection = (URL(urlWithParams).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()  // Fix: READ_TIMEOUT not Read_TIMEOUT
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Public events response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Public events response: ${responseText.take(500)}...")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val events = parsePublicEventsResponse(responseText)
                            Log.d("EventsAPI", "✅ Successfully parsed ${events.size} public events")
                            Result.success(events)
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing public events response", e)
                            Result.failure(Exception("Failed to parse events: ${e.message}"))
                        }
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to fetch events: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error fetching public events", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get events where the user is involved (created, attending, etc.)
     */
    suspend fun getMyEvents(): Result<List<Event>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "📋 Fetching my events")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/my-events/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 My events response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 My events response: ${responseText.take(500)}...")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val events = parseMyEventsResponse(responseText)
                            Log.d("EventsAPI", "✅ Successfully parsed ${events.size} my events")
                            Result.success(events)
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing my events response", e)
                            Result.failure(Exception("Failed to parse my events: ${e.message}"))
                        }
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to fetch my events: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error fetching my events", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get events where the user can manage (owner/editor)
     * For adding tracks to events
     */
    suspend fun getUserManageableEvents(): Result<List<Event>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "📋 Fetching manageable events")
                
                // Use the my-events endpoint and filter for manageable roles
                val myEventsResult = getMyEvents()
                if (myEventsResult.isFailure) {
                    return@withContext Result.failure(myEventsResult.exceptionOrNull() ?: Exception("Failed to get events"))
                }
                
                val allMyEvents = myEventsResult.getOrThrow()
                
                // Filter events where user has management permissions (owner or editor)
                val manageableEvents = allMyEvents.filter { event ->
                    event.current_user_role in listOf("owner", "editor")
                }
                
                Log.d("EventsAPI", "✅ Found ${manageableEvents.size} manageable events out of ${allMyEvents.size} total events")
                Result.success(manageableEvents)
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error getting manageable events", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create a new event
     */
    suspend fun createEvent(request: CreateEventRequest): Result<CreateEventResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🎪 Creating new event: ${request.title}")
                Log.d("EventsAPI", "📍 Location: ${request.location}")
                Log.d("EventsAPI", "⏰ Start time: ${request.event_start_time}")
                Log.d("EventsAPI", "🌐 Is public: ${request.is_public}")
                
                // Use the correct endpoint from NetworkConfig
                val url = NetworkConfig.getFullUrl(NetworkConfig.Endpoints.CREATE_EVENT)
                Log.d("EventsAPI", "🔗 Using endpoint: $url")
                
                val requestBody = JSONObject().apply {
                    put("title", request.title)
                    put("description", request.description ?: "")
                    put("location", request.location)
                    put("eventStartTime", request.event_start_time)
                    if (!request.event_end_time.isNullOrBlank()) {
                        put("eventEndTime", request.event_end_time)
                    }
                    put("isPublic", request.is_public)
                }.toString()
                
                Log.d("EventsAPI", "📤 Create event request body: $requestBody")
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                        Log.d("EventsAPI", "🔑 Added authorization header")
                    } else {
                        Log.w("EventsAPI", "⚠️ No authentication token available")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                        Log.d("EventsAPI", "🌐 Added CORS origin header for Codespaces")
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
                Log.d("EventsAPI", "📨 Create event response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Create event response: $responseText")
                
                when (responseCode) {
                    201 -> {
                        try {
                            val jsonResponse = JSONObject(responseText)
                            val eventId = jsonResponse.optString("id")
                            val title = jsonResponse.optString("title")
                            
                            val response = CreateEventResponse(
                                success = true,
                                message = "Event created successfully",
                                eventId = eventId,
                                title = title
                            )
                            
                            Log.d("EventsAPI", "✅ Event created successfully with ID: $eventId")
                            Result.success(response)
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing create event response", e)
                            Result.failure(Exception("Failed to parse event creation response: ${e.message}"))
                        }
                    }
                    400 -> {
                        Log.e("EventsAPI", "❌ Bad request (400): $responseText")
                        val errorMessage = try {
                            val errorJson = JSONObject(responseText)
                            val errors = mutableListOf<String>()
                            
                            // Check for field-specific errors
                            if (errorJson.has("title")) {
                                val titleErrors = errorJson.get("title")
                                if (titleErrors is JSONArray) {
                                    errors.add("Title: ${titleErrors.getString(0)}")
                                } else {
                                    errors.add("Title: $titleErrors")
                                }
                            }
                            if (errorJson.has("location")) {
                                val locationErrors = errorJson.get("location")
                                if (locationErrors is JSONArray) {
                                    errors.add("Location: ${locationErrors.getString(0)}")
                                } else {
                                    errors.add("Location: $locationErrors")
                                }
                            }
                            if (errorJson.has("event_start_time")) {
                                val timeErrors = errorJson.get("event_start_time")
                                if (timeErrors is JSONArray) {
                                    errors.add("Start time: ${timeErrors.getString(0)}")
                                } else {
                                    errors.add("Start time: $timeErrors")
                                }
                            }
                            if (errorJson.has("non_field_errors")) {
                                val nonFieldErrors = errorJson.getJSONArray("non_field_errors")
                                for (i in 0 until nonFieldErrors.length()) {
                                    errors.add(nonFieldErrors.getString(i))
                                }
                            }
                            
                            if (errors.isNotEmpty()) {
                                errors.joinToString("; ")
                            } else {
                                errorJson.optString("message", errorJson.optString("detail", "Invalid event data"))
                            }
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing error response", e)
                            "Invalid event data. Please check all required fields and ensure location is valid."
                        }
                        Result.failure(Exception(errorMessage))
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized (401) - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required - please log in again"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Forbidden (403) - insufficient permissions")
                        Result.failure(Exception("You don't have permission to create events"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Unexpected error response ($responseCode): $responseText")
                        Result.failure(Exception("Failed to create event: HTTP $responseCode - $responseText"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error creating event", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * Add a track to an event
     */
    suspend fun addTrackToEvent(eventId: String, trackId: String): Result<AddTrackToEventResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🎵 Adding track $trackId to event $eventId")
                
                // Fix: Use the correct endpoint that matches your swagger
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/tracks/$trackId/add/"
                
                Log.d("EventsAPI", "🔗 Add track URL: $url")
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    // Note: No request body needed since track_id is in the URL path
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                        Log.d("EventsAPI", "🔑 Added authorization header")
                    } else {
                        Log.w("EventsAPI", "⚠️ No authentication token available")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                        Log.d("EventsAPI", "🌐 Added CORS origin header for Codespaces")
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                // No need to write request body since all parameters are in the URL
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Add track response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Add track response: $responseText")
                
                when (responseCode) {
                    200, 201 -> {
                        try {
                            val jsonResponse = JSONObject(responseText)
                            val message = jsonResponse.optString("message", "Track added successfully")
                            
                            val response = AddTrackToEventResponse(
                                success = true,
                                message = message
                            )
                            
                            Log.d("EventsAPI", "✅ Track added to event successfully: $message")
                            Result.success(response)
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing add track response", e)
                            // Fallback success response if JSON parsing fails but status is 200/201
                            val response = AddTrackToEventResponse(
                                success = true,
                                message = "Track added successfully"
                            )
                            Result.success(response)
                        }
                    }
                    400 -> {
                        Log.e("EventsAPI", "❌ Bad request (400): $responseText")
                        val errorMessage = try {
                            val errorJson = JSONObject(responseText)
                            errorJson.optString("message", errorJson.optString("detail", "Invalid track or event"))
                        } catch (e: Exception) {
                            "Invalid track or event"
                        }
                        Result.failure(Exception(errorMessage))
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized (401): $responseText")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("You need to be logged in to add tracks"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Forbidden (403): $responseText")
                        Result.failure(Exception("You don't have permission to add tracks to this event"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Not found (404): $responseText")
                        Result.failure(Exception("Event or track not found"))
                    }
                    405 -> {
                        Log.e("EventsAPI", "❌ Method not allowed (405): $responseText")
                        Result.failure(Exception("Invalid request method - this should not happen"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Unexpected response code: $responseCode")
                        Result.failure(Exception("Failed to add track to event: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error adding track to event", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Get event details by ID
     */
    suspend fun getEventDetails(eventId: String): Result<Event> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🎪 Fetching event details for ID: $eventId")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Event details response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Event details response: $responseText")
                
                // Add specific logging before parsing
                Log.d("EventsAPI", "🔍 Raw response analysis:")
                val tempJson = JSONObject(responseText)
                Log.d("EventsAPI", "   - Raw currentUserRole value: '${tempJson.opt("currentUserRole")}'")
                Log.d("EventsAPI", "   - Is currentUserRole null? ${tempJson.isNull("currentUserRole")}")
                Log.d("EventsAPI", "   - userRoles object: '${tempJson.opt("userRoles")}'")
                Log.d("EventsAPI", "   - songs array: '${tempJson.opt("songs")}'")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val event = parseEventDetailsResponse(responseText)
                            Log.d("EventsAPI", "✅ Successfully parsed event details: ${event.title}")
                            Log.d("EventsAPI", "   - Parsed current_user_role: '${event.current_user_role}'")
                            Result.success(event)
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing event details response", e)
                            Result.failure(Exception("Failed to parse event details: ${e.message}"))
                        }
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Access denied to event")
                        Result.failure(Exception("Access denied to this event"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Event not found")
                        Result.failure(Exception("Event not found"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to fetch event details: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error fetching event details", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get tracks in an event with vote counts and full details
     */
    suspend fun getEventTracks(eventId: String): Result<List<Track>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🎵 Fetching tracks for event ID: $eventId")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/tracks/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Event tracks response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Event tracks response: $responseText")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val tracks = parseEventTracksResponse(responseText)
                            Log.d("EventsAPI", "✅ Successfully parsed ${tracks.size} event tracks")
                            Result.success(tracks)
                        } catch (e: Exception) {
                            Log.e("EventsAPI", "❌ Error parsing event tracks response", e)
                            Result.failure(Exception("Failed to parse event tracks: ${e.message}"))
                        }
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Access denied to event tracks")
                        Result.failure(Exception("Access denied to this event"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Event not found")
                        Result.failure(Exception("Event not found"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to fetch event tracks: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error fetching event tracks", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get tracks in an event with vote counts and full details
     * This combines the track IDs from event details with full track information
     */
    suspend fun getEventTracksWithVotes(eventId: String): Result<List<Track>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🎵 Fetching tracks with votes for event ID: $eventId")

                // Get tracks from the tracks endpoint. The backend proxies this through Jamendo,
                // which occasionally blips transiently (rate limit/timeout) - one retry clears up
                // most of those instead of the event flashing "0 tracks" on every other open.
                var tracksResult = getEventTracks(eventId)
                if (tracksResult.isFailure) {
                    Log.w("EventsAPI", "⚠️ Track fetch failed, retrying once: ${tracksResult.exceptionOrNull()?.message}")
                    delay(500)
                    tracksResult = getEventTracks(eventId)
                }
                if (tracksResult.isFailure) {
                    return@withContext Result.failure(tracksResult.exceptionOrNull() ?: Exception("Failed to get tracks"))
                }
                
                val tracks = tracksResult.getOrThrow()
                
                // Get vote information from event details
                val eventDetailsUrl = "${NetworkConfig.BASE_URL}/api/events/$eventId/"
                val voteData = getTrackVoteData(eventDetailsUrl)
                
                // Merge tracks with vote information and sort by vote count
                val tracksWithVotes = tracks.map { track ->
                    val voteInfo = voteData[track.id]
                    track.copy(
                        voteCount = voteInfo?.first ?: 0,
                        hasUserVoted = voteInfo?.second ?: false
                    )
                }.sortedByDescending { it.voteCount } // Sort by vote count (highest first)
                
                Log.d("EventsAPI", "✅ Successfully merged ${tracksWithVotes.size} tracks with votes")
                Result.success(tracksWithVotes)
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error getting tracks with votes", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Lightweight vote refresh: hits only the event-detail endpoint (DB-backed, no Jamendo call)
     * to get current vote counts/hasUserVoted per track. Use this after voting/unvoting instead
     * of [getEventTracksWithVotes] - track metadata (title/artist/art/audio) never changes from a
     * vote, so there's no reason to re-fetch it from Jamendo on every tap.
     */
    suspend fun getEventVoteData(eventId: String): Result<Map<String, Pair<Int, Boolean>>> {
        return withContext(Dispatchers.IO) {
            try {
                val voteData = getTrackVoteData("${NetworkConfig.BASE_URL}/api/events/$eventId/")
                Result.success(voteData)
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error fetching vote data", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get vote data for tracks from event details response
     * Returns map of track_id to Pair(vote_count, user_has_voted)
     */
    private suspend fun getTrackVoteData(eventDetailsUrl: String): Map<String, Pair<Int, Boolean>> {
        return try {
            val connection = (URL(eventDetailsUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                
                val token = tokenManager.getToken()
                if (token != null) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                NetworkConfig.applyDeviceHeaders(this)

                if (NetworkConfig.isCodespaces()) {
                    setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                }
                
                connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
            }
            
            val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
            
            val eventJson = JSONObject(responseText)
            val songsArray = eventJson.optJSONArray("songs") ?: JSONArray()
            val voteData = mutableMapOf<String, Pair<Int, Boolean>>()
            
            for (i in 0 until songsArray.length()) {
                val songJson = songsArray.getJSONObject(i)
                val trackId = songJson.optString("trackId")
                val voteCount = songJson.optInt("voteCount", 0)
                val hasUserVoted = songJson.optBoolean("hasUserVoted", false)

                if (trackId.isNotBlank()) {
                    voteData[trackId] = Pair(voteCount, hasUserVoted)
                }
            }
            
            Log.d("EventsAPI", "🗳️ Extracted vote data for ${voteData.size} tracks")
            voteData
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Error extracting vote data", e)
            emptyMap()
        }
    }
    
    /**
     * Vote for a track in an event
     */
    suspend fun voteForTrack(eventId: String, trackId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "👍 Voting for track $trackId in event $eventId")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/tracks/$trackId/vote/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Vote response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Vote response: $responseText")
                
                when (responseCode) {
                    200, 201 -> {
                        val jsonResponse = JSONObject(responseText)
                        val message = jsonResponse.optString("message", "Vote added successfully")
                        Log.d("EventsAPI", "✅ Vote added successfully")
                        Result.success(message)
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Access denied to vote")
                        Result.failure(Exception("Access denied"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Event or track not found")
                        Result.failure(Exception("Event or track not found"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to vote: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error voting for track", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Remove vote for a track in an event
     */
    suspend fun unvoteForTrack(eventId: String, trackId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "👎 Removing vote for track $trackId in event $eventId")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/tracks/$trackId/unvote/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Unvote response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Unvote response: $responseText")
                
                when (responseCode) {
                    200, 204 -> {
                        val message = try {
                            val jsonResponse = JSONObject(responseText)
                            jsonResponse.optString("message", "Vote removed successfully")
                        } catch (e: Exception) {
                            "Vote removed successfully"
                        }
                        Log.d("EventsAPI", "✅ Vote removed successfully")
                        Result.success(message)
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Access denied to unvote")
                        Result.failure(Exception("Access denied"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Event or track not found")
                        Result.failure(Exception("Event or track not found"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to remove vote: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error removing vote for track", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Join an event
     */
    suspend fun joinEvent(eventId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🎪 Joining event ID: $eventId")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/join/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Join event response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Join event response: $responseText")
                
                when (responseCode) {
                    200, 201 -> {
                        val jsonResponse = JSONObject(responseText)
                        val message = jsonResponse.optString("message", "Joined event successfully")
                        Log.d("EventsAPI", "✅ Joined event successfully")
                        Result.success(message)
                    }
                    400 -> {
                        Log.e("EventsAPI", "❌ Bad request: $responseText")
                        Result.failure(Exception("Unable to join event"))
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Access denied to join event")
                        Result.failure(Exception("You cannot join this event"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Event not found")
                        Result.failure(Exception("Event not found"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to join event: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error joining event", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Leave an event
     */
    suspend fun leaveEvent(eventId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🚪 Leaving event ID: $eventId")
                
                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/leave/"
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📨 Leave event response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("EventsAPI", "📨 Leave event response: $responseText")
                
                when (responseCode) {
                    200, 204 -> {
                        val message = try {
                            val jsonResponse = JSONObject(responseText)
                            jsonResponse.optString("message", "Left event successfully")
                        } catch (e: Exception) {
                            "Left event successfully"
                        }
                        Log.d("EventsAPI", "✅ Left event successfully")
                        Result.success(message)
                    }
                    400 -> {
                        Log.e("EventsAPI", "❌ Bad request: $responseText")
                        Result.failure(Exception("Unable to leave event"))
                    }
                    401 -> {
                        Log.e("EventsAPI", "❌ Unauthorized - token may be expired")
                        tokenManager.notifySessionExpired()
                        Result.failure(Exception("Authentication required"))
                    }
                    403 -> {
                        Log.e("EventsAPI", "❌ Access denied to leave event")
                        Result.failure(Exception("You cannot leave this event"))
                    }
                    404 -> {
                        Log.e("EventsAPI", "❌ Event not found")
                        Result.failure(Exception("Event not found"))
                    }
                    else -> {
                        Log.e("EventsAPI", "❌ Error response: $responseText")
                        Result.failure(Exception("Failed to leave event: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error leaving event", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update an event's details (organizer/manager only)
     */
    suspend fun updateEvent(eventId: String, request: CreateEventRequest): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "✏️ Updating event $eventId")

                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/update/"

                val requestBody = JSONObject().apply {
                    put("title", request.title)
                    put("description", request.description ?: "")
                    put("location", request.location)
                    put("event_start_time", request.event_start_time)
                    if (!request.event_end_time.isNullOrBlank()) {
                        put("event_end_time", request.event_end_time)
                    }
                    put("is_public", request.is_public)
                }.toString()

                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true

                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }

                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                }

                Log.d("EventsAPI", "📨 Update event response ($responseCode): $responseText")

                when (responseCode) {
                    200 -> Result.success(JSONObject(responseText).optString("message", "Event updated successfully"))
                    400 -> Result.failure(Exception(JSONObject(responseText).optString("error", "Unable to update event")))
                    401 -> { tokenManager.notifySessionExpired(); Result.failure(Exception("Authentication required")) }
                    403 -> Result.failure(Exception("You don't have permission to edit this event"))
                    404 -> Result.failure(Exception("Event not found"))
                    else -> Result.failure(Exception("Failed to update event: HTTP $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error updating event", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete an event (organizer only)
     */
    suspend fun deleteEvent(eventId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🗑️ Deleting event $eventId")

                val url = "${NetworkConfig.BASE_URL}/api/events/$eventId/delete/"

                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")

                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }

                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                }

                Log.d("EventsAPI", "📨 Delete event response ($responseCode): $responseText")

                when (responseCode) {
                    200 -> Result.success(JSONObject(responseText).optString("message", "Event deleted successfully"))
                    401 -> { tokenManager.notifySessionExpired(); Result.failure(Exception("Authentication required")) }
                    403 -> Result.failure(Exception("Only the organizer can delete this event"))
                    404 -> Result.failure(Exception("Event not found"))
                    else -> Result.failure(Exception("Failed to delete event: HTTP $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Network error deleting event", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get all users for invitation
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "👥 Fetching all users for invitation")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/users/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken() // Fixed: changed from getAccessToken() to getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📡 Users API Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("EventsAPI", "📄 Users API Response: $responseText")
                    
                    val users = parseUsersResponse(responseText)
                    Log.d("EventsAPI", "✅ Successfully fetched ${users.size} users")
                    Result.success(users)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("EventsAPI", "❌ Failed to fetch users: $responseCode - $errorText")
                    Result.failure(Exception("Failed to fetch users: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error fetching users", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Invite user to event
     */
    suspend fun inviteUserToEvent(eventId: String, userId: String, role: String = "attendee"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "📧 Inviting user $userId to event $eventId with role $role")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/events/$eventId/invite/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                // Create request body matching your API format
                val requestBody = JSONObject().apply {
                    put("userId", userId.toInt())
                    put("role", role)
                }
                
                Log.d("EventsAPI", "📤 Invite request URL: ${url}")
                Log.d("EventsAPI", "📤 Invite request body: $requestBody")
                
                // Send request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📡 Invite API Response Code: $responseCode")
                
                when (responseCode) {
                    HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("EventsAPI", "📄 Invite API Response: $responseText")
                        
                        val responseJson = JSONObject(responseText)
                        val message = responseJson.optString("message", "User invited successfully")
                        Log.d("EventsAPI", "✅ Successfully invited user: $message")
                        Result.success(message)
                    }
                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Bad request"
                        Log.e("EventsAPI", "❌ Bad request (400): $errorText")
                        Result.failure(Exception("User may already be invited or attending, or invalid role"))
                    }
                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Forbidden"
                        Log.e("EventsAPI", "❌ Forbidden (403): $errorText")
                        Result.failure(Exception("Permission denied - insufficient permissions for the requested role"))
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        Log.e("EventsAPI", "❌ Unauthorized (401)")
                        Result.failure(Exception("Authentication required - please log in again"))
                    }
                    else -> {
                        val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                        Log.e("EventsAPI", "❌ Failed to invite user: $responseCode - $errorText")
                        Result.failure(Exception("Failed to invite user: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error inviting user", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Accept event invitation
     */
    suspend fun acceptEventInvitation(eventId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "✅ Accepting invitation for event $eventId")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/events/$eventId/accept-invite/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken() // Fixed: changed from getAccessToken() to getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📡 Accept Invite API Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("EventsAPI", "📄 Accept Invite API Response: $responseText")
                    
                    val responseJson = JSONObject(responseText)
                    val message = responseJson.optString("message", "Invitation accepted successfully")
                    Log.d("EventsAPI", "✅ Successfully accepted invitation")
                    Result.success(message)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("EventsAPI", "❌ Failed to accept invitation: $responseCode - $errorText")
                    Result.failure(Exception("Failed to accept invitation: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error accepting invitation", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Decline event invitation
     */
    suspend fun declineEventInvitation(eventId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "❌ Declining invitation for event $eventId")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/events/$eventId/decline-invite/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken() // Fixed: changed from getAccessToken() to getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📡 Decline Invite API Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("EventsAPI", "📄 Decline Invite API Response: $responseText")
                    
                    val responseJson = JSONObject(responseText)
                    val message = responseJson.optString("message", "Invitation declined successfully")
                    Log.d("EventsAPI", "✅ Successfully declined invitation")
                    Result.success(message)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("EventsAPI", "❌ Failed to decline invitation: $responseCode - $errorText")
                    Result.failure(Exception("Failed to decline invitation: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error declining invitation", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get user's event notifications/invitations
     */
    suspend fun getEventNotifications(): Result<List<EventNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventsAPI", "🔔 Fetching event notifications")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/users/me/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken() // Fixed: changed from getAccessToken() to getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    NetworkConfig.applyDeviceHeaders(this)

                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("EventsAPI", "📡 Notifications API Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("EventsAPI", "📄 Notifications API Response: $responseText")
                    
                    val notifications = parseEventNotifications(responseText)
                    Log.d("EventsAPI", "✅ Successfully fetched ${notifications.size} event notifications")
                    Result.success(notifications)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("EventsAPI", "❌ Failed to fetch notifications: $responseCode - $errorText")
                    Result.failure(Exception("Failed to fetch notifications: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("EventsAPI", "❌ Error fetching notifications", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Parse public events response from JSON
     */
    private fun parsePublicEventsResponse(responseText: String): List<Event> {
        try {
            Log.d("EventsAPI", "📋 Parsing public events response: $responseText")
            
            // The API returns a direct array
            val eventsArray = JSONArray(responseText)
            Log.d("EventsAPI", "📋 Found ${eventsArray.length()} public events")
            
            val events = mutableListOf<Event>()
            for (i in 0 until eventsArray.length()) {
                val eventJson = eventsArray.getJSONObject(i)
                Log.d("EventsAPI", "📋 Processing event $i: $eventJson")
                
                // The organizer is a string, not an object in the public events API
                val organizerName = eventJson.optString("organizer", "Unknown")
                val organizer = EventOrganizer(
                    id = "", // No ID provided in public events
                    name = organizerName,
                    avatar = null // No avatar provided in public events
                )
                
                val event = Event(
                    id = eventJson.optInt("id").toString(), // Convert int to string
                    title = eventJson.optString("title"),
                    description = eventJson.optString("description", ""), // Not in API response
                    location = eventJson.optString("location"),
                    organizer = organizer,
                    attendeeCount = eventJson.optInt("attendeeCount", 0),
                    trackCount = eventJson.optInt("trackCount", 0), // Not in API response
                    is_public = eventJson.optBoolean("isPublic", true),
                    event_start_time = eventJson.optString("eventStartTime"),
                    event_end_time = eventJson.optString("eventEndTime"), // Not in API response
                    image_url = eventJson.optString("imageUrl"), // Not in API response
                    created_at = eventJson.optString("createdAt"), // Not in API response
                    current_user_role = null // Public events don't have user role info
                )
                
                events.add(event)
                Log.d("EventsAPI", "🎪 Parsed public event: ${event.title} by ${event.organizer.name}")
            }
            
            Log.d("EventsAPI", "✅ Successfully parsed ${events.size} public events")
            return events
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Error parsing public events JSON: $responseText", e)
            throw Exception("Failed to parse public events: ${e.message}")
        }
    }
    
    /**
     * Parse my events response from JSON
     */
    private fun parseMyEventsResponse(responseText: String): List<Event> {
        try {
            val responseJson = JSONObject(responseText)
            val eventsArray = responseJson.optJSONArray("events") ?: responseJson.optJSONArray("results") ?: JSONArray()
            
            val events = mutableListOf<Event>()
            for (i in 0 until eventsArray.length()) {
                val eventJson = eventsArray.getJSONObject(i)
                
                // For "my events", the userRole field indicates the current user's role
                val userRole = eventJson.optString("userRole").takeIf { it.isNotBlank() }

                // Parse organizer - the backend sends this as a plain name string here, not an object
                val organizerJson = eventJson.optJSONObject("organizer")
                val organizer = EventOrganizer(
                    id = organizerJson?.optString("id") ?: "",
                    name = organizerJson?.optString("name")
                        ?: eventJson.optString("organizer").takeIf { it.isNotBlank() }
                        ?: if (userRole == "owner") "You" else "Event Organizer", // Show "You" if current user is owner
                    avatar = organizerJson?.optString("avatar")
                )

                val event = Event(
                    id = eventJson.optString("id"),
                    title = eventJson.optString("title"),
                    description = eventJson.optString("description"),
                    location = eventJson.optString("location"),
                    organizer = organizer,
                    attendeeCount = eventJson.optInt("attendeesCount", 0),
                    trackCount = eventJson.optInt("trackCount", 0),
                    is_public = eventJson.optBoolean("isPublic", true),
                    event_start_time = eventJson.optString("startTime"),
                    event_end_time = eventJson.optString("endTime"),
                    image_url = eventJson.optString("imageUrl"),
                    created_at = eventJson.optString("createdAt"),
                    current_user_role = userRole // Map userRole to current_user_role
                )
                
                events.add(event)
                Log.d("EventsAPI", "🎪 Parsed my event: ${event.title} (role: $userRole)")
            }
            
            return events
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Error parsing my events JSON", e)
            throw e
        }
    }
    
    /**
     * Parse single event details response from JSON
     */
    private fun parseEventDetailsResponse(responseText: String): Event {
        try {
            val eventJson = JSONObject(responseText)
            
            // Parse organizer
            val organizerJson = eventJson.optJSONObject("organizer")
            val organizer = EventOrganizer(
                id = organizerJson?.optString("id") ?: "",
                name = organizerJson?.optString("name") ?: "Unknown",
                avatar = organizerJson?.optString("avatar")
            )
            
            // Parse songs array to get track count
            val songsArray = eventJson.optJSONArray("songs")
            val trackCount = songsArray?.length() ?: eventJson.optInt("trackCount", 0)

            // Handle current_user_role properly - check if it's actually null in JSON
            val currentUserRole = if (eventJson.isNull("currentUserRole")) {
                null
            } else {
                eventJson.optString("currentUserRole").takeIf { it.isNotBlank() }
            }

            Log.d("EventsAPI", "🔍 Parsing currentUserRole:")
            Log.d("EventsAPI", "   - Raw JSON isNull: ${eventJson.isNull("currentUserRole")}")
            Log.d("EventsAPI", "   - Raw JSON value: '${eventJson.opt("currentUserRole")}'")
            Log.d("EventsAPI", "   - Parsed role: '$currentUserRole'")

            return Event(
                id = eventJson.optString("id"),
                title = eventJson.optString("title"),
                description = eventJson.optString("description"),
                location = eventJson.optString("location"),
                organizer = organizer,
                attendeeCount = eventJson.optInt("attendeeCount", 0),
                trackCount = trackCount, // Use actual songs array length if available
                is_public = eventJson.optBoolean("isPublic", true),
                event_start_time = eventJson.optString("eventStartTime"),
                event_end_time = eventJson.optString("eventEndTime"),
                image_url = eventJson.optString("imageUrl"),
                created_at = eventJson.optString("createdAt"),
                current_user_role = currentUserRole
            )
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Error parsing event details JSON", e)
            throw e
        }
    }
    
    /**
     * Parse event tracks response from JSON
     */
    private fun parseEventTracksResponse(responseText: String): List<Track> {
        try {
            val responseJson = JSONObject(responseText)
            val tracksArray = responseJson.optJSONArray("tracks") ?: JSONArray()
            
            val tracks = mutableListOf<Track>()
            for (i in 0 until tracksArray.length()) {
                val trackJson = tracksArray.getJSONObject(i)
                
                // Get artist name - could be in artist_name or nested artist object
                val artistName = trackJson.optString("artist_name").takeIf { it.isNotBlank() }
                    ?: trackJson.optJSONObject("artist")?.optString("name")
                    ?: "Unknown Artist"
                
                // Get duration - handle different formats
                val durationValue = trackJson.optString("duration", "0")
                val duration = when {
                    durationValue.contains(":") -> durationValue // Already formatted as MM:SS
                    durationValue.isNotBlank() && durationValue != "0" -> {
                        try {
                            val seconds = durationValue.toInt()
                            "${seconds / 60}:${String.format("%02d", seconds % 60)}"
                        } catch (e: Exception) {
                            durationValue
                        }
                    }
                    else -> "0:00"
                }
                
                // Get image URL - try different possible fields
                val imageUrl = trackJson.optString("image")
                    .takeIf { it.isNotBlank() }
                    ?: trackJson.optString("album_image")
                    ?: trackJson.optString("cover")
                    ?: ""
                
                // Get audio URL - this will be used as description for playback
                val audioUrl = trackJson.optString("audio")
                    ?: trackJson.optString("audiodownload")
                    ?: ""
                
                val track = Track(
                    id = trackJson.optString("id", ""),
                    title = trackJson.optString("name", trackJson.optString("title", "Unknown Track")),
                    artist = artistName,
                    thumbnailUrl = imageUrl,
                    duration = duration,
                    description = audioUrl // Store audio URL in description for playback
                )
                
                tracks.add(track)
                Log.d("EventsAPI", "🎵 Parsed track: ${track.title} by ${track.artist}")
            }
            
            return tracks
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Error parsing event tracks JSON", e)
            throw e
        }
    }

    /**
     * Parse users response from JSON
     */
    private fun parseUsersResponse(responseText: String): List<User> {
        return try {
            Log.d("EventsAPI", "📋 Starting to parse users response")
            Log.d("EventsAPI", "📋 Raw response: $responseText")
            
            // Check if response is empty
            if (responseText.isBlank()) {
                Log.e("EventsAPI", "❌ Empty response received")
                throw Exception("Empty response from server")
            }
            
            // The API returns a direct array like: [{"id":1,"name":"abdessamad",...}, ...]
            val usersArray = JSONArray(responseText)
            Log.d("EventsAPI", "📋 Successfully created JSONArray with ${usersArray.length()} items")
            
            val users = mutableListOf<User>()
            for (i in 0 until usersArray.length()) {
                try {
                    val userJson = usersArray.getJSONObject(i)
                    Log.d("EventsAPI", "📋 Processing user $i: $userJson")
                    
                    val userId = userJson.optInt("id", -1)
                    val userFullName = userJson.optString("full_name", "Unknown")
                    val userUsername = userJson.optString("username", "")
                    val userEmail = userJson.optString("email", "")
                    val userAvatar = userJson.optString("avatar", "")

                    if (userId == -1) {
                        Log.w("EventsAPI", "⚠️ User at index $i has invalid ID, skipping")
                        continue
                    }

                    val user = User(
                        id = userId.toString(),
                        fullName = userFullName,
                        username = userUsername,
                        photoUrl = userAvatar,
                        email = userEmail
                    )

                    users.add(user)
                    Log.d("EventsAPI", "👤 Successfully parsed user: ${user.fullName} (ID: ${user.id})")
                    
                } catch (e: Exception) {
                    Log.e("EventsAPI", "❌ Error parsing user at index $i", e)
                    // Continue with next user instead of failing completely
                }
            }
            
            Log.d("EventsAPI", "✅ Successfully parsed ${users.size} out of ${usersArray.length()} users")
            
            if (users.isEmpty()) {
                throw Exception("No valid users found in response")
            }
            
            users
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Critical error parsing users JSON", e)
            Log.e("EventsAPI", "❌ Response that failed: $responseText")
            throw Exception("Failed to parse users: ${e.message}")
        }
    }

    /**
     * Parse event notifications from JSON
     */
    private fun parseEventNotifications(responseText: String): List<EventNotification> {
        try {
            val jsonResponse = JSONObject(responseText)
            val eventNotifications = jsonResponse.optJSONObject("event_notifications") ?: JSONObject()
            
            val notifications = mutableListOf<EventNotification>() // This line was missing!
            val keys = eventNotifications.keys()
            
            while (keys.hasNext()) {
                val key = keys.next()
                val notificationJson = eventNotifications.getJSONObject(key)
                
                val notification = EventNotification(
                    id = key,
                    eventId = notificationJson.optString("event_id"),
                    eventTitle = notificationJson.optString("event_title", "Unknown Event"),
                    inviterName = notificationJson.optString("inviter_name", "Unknown User"),
                    message = notificationJson.optString("message", "You have been invited to an event"),
                    timestamp = notificationJson.optLong("timestamp", System.currentTimeMillis())
                )
                
                notifications.add(notification)
                Log.d("EventsAPI", "🔔 Parsed notification: ${notification.eventTitle} from ${notification.inviterName}")
            }
            
            return notifications.sortedByDescending { it.timestamp }
            
        } catch (e: Exception) {
            Log.e("EventsAPI", "❌ Error parsing event notifications JSON", e)
            throw e
        }
    }

}

/**
 * Data classes for Events API
 */
data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    val location: String,
    val event_start_time: String,
    val event_end_time: String? = null,
    val is_public: Boolean = true
)

data class CreateEventResponse(
    val success: Boolean,
    val message: String,
    val eventId: String? = null,
    val title: String? = null
)

data class AddTrackToEventResponse(
    val success: Boolean,
    val message: String
)

/**
 * Data classes for invitation features
 */
data class User(
    val id: String,
    val fullName: String,
    val username: String,
    val photoUrl: String,
    val email: String
)

data class EventNotification(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val inviterName: String,
    val message: String,
    val timestamp: Long
)