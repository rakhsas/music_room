package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling notifications and invitations
 */
@Singleton
class NotificationService @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    /**
     * Accept event invitation
     */
    suspend fun acceptEventInvitation(eventId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NotificationService", "✅ Accepting event invitation for event $eventId")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/events/$eventId/accept-invite/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    
                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("NotificationService", "📡 Accept invitation response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("NotificationService", "📄 Accept invitation response: $responseText")
                    
                    val responseJson = JSONObject(responseText)
                    val message = responseJson.optString("message", "Invitation accepted successfully")
                    Log.d("NotificationService", "✅ Successfully accepted invitation")
                    Result.success(message)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("NotificationService", "❌ Failed to accept invitation: $responseCode - $errorText")
                    Result.failure(Exception("Failed to accept invitation: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("NotificationService", "❌ Error accepting invitation", e)
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
                Log.d("NotificationService", "❌ Declining event invitation for event $eventId")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/events/$eventId/decline-invite/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    
                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("NotificationService", "📡 Decline invitation response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("NotificationService", "📄 Decline invitation response: $responseText")
                    
                    val responseJson = JSONObject(responseText)
                    val message = responseJson.optString("message", "Invitation declined successfully")
                    Log.d("NotificationService", "✅ Successfully declined invitation")
                    Result.success(message)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("NotificationService", "❌ Failed to decline invitation: $responseCode - $errorText")
                    Result.failure(Exception("Failed to decline invitation: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("NotificationService", "❌ Error declining invitation", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's event notifications
     */
    suspend fun getEventNotifications(): Result<List<EventNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NotificationService", "🔔 Fetching event notifications")
                
                val url = URL(NetworkConfig.BASE_URL + "/api/users/me/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    
                    // Add authorization header
                    val token = tokenManager.getToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    
                    connectTimeout = 30000
                    readTimeout = 30000
                }
                
                val responseCode = connection.responseCode
                Log.d("NotificationService", "📡 Notifications response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("NotificationService", "📄 Notifications response: $responseText")
                    
                    val notifications = parseEventNotifications(responseText)
                    Log.d("NotificationService", "✅ Successfully fetched ${notifications.size} notifications")
                    Result.success(notifications)
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("NotificationService", "❌ Failed to fetch notifications: $responseCode - $errorText")
                    Result.failure(Exception("Failed to fetch notifications: $errorText"))
                }
                
            } catch (e: Exception) {
                Log.e("NotificationService", "❌ Error fetching notifications", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Parse event notifications from JSON response
     */
    private fun parseEventNotifications(responseText: String): List<EventNotification> {
        try {
            val jsonResponse = JSONObject(responseText)
            val eventNotifications = jsonResponse.optJSONObject("event_notifications") ?: JSONObject()
            
            val notifications = mutableListOf<EventNotification>()
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
                Log.d("NotificationService", "🔔 Parsed notification: ${notification.eventTitle} from ${notification.inviterName}")
            }
            
            return notifications.sortedByDescending { it.timestamp }
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error parsing notifications JSON", e)
            throw e
        }
    }
}