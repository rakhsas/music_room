package com.example.musicroom.data.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.musicroom.BuildConfig
import java.net.HttpURLConnection

/**
 * Network configuration for the MusicRoom app
 * Centralized place to manage all API endpoints and network settings
 */
object NetworkConfig {

    // 🔧 ENVIRONMENT CONFIGURATION
    private const val CURRENT_ENVIRONMENT = "DEVELOPMENT"

    // 🌐 BASE URLS FOR DIFFERENT ENVIRONMENTS
    private const val CODESPACES_BASE_URL = "https://crispy-fishstick-v7x7p6vgj75hpxrx-8000.app.github.dev"
    private const val LOCAL_BASE_URL_EMULATOR = "http://10.0.2.2:8000"
    private const val LOCAL_BASE_URL_PHYSICAL = "http://192.168.1.149:8000"
    private const val STAGING_BASE_URL = "https://staging-api.musicroom.com"
    private const val PRODUCTION_BASE_URL = "https://api.musicroom.com"

    // 🎯 DEPLOYMENT TYPE
    // Choose your deployment: "CODESPACES", "LOCAL", "STAGING", "PRODUCTION"
    private const val DEPLOYMENT_TYPE = "LOCAL"

    // 🧪 RUNTIME OVERRIDE - lets a tester point the app at any backend without rebuilding
    // ("the back-end's address must be configurable on the application for tests" - V.5).
    private const val PREFS_NAME = "network_config"
    private const val KEY_BASE_URL_OVERRIDE = "base_url_override"
    private var prefs: SharedPreferences? = null

    /** Call once from Application.onCreate(). */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val compiledDefaultBaseUrl: String
        get() = when (DEPLOYMENT_TYPE) {
            "CODESPACES" -> CODESPACES_BASE_URL
            "LOCAL" -> LOCAL_BASE_URL_PHYSICAL // or LOCAL_BASE_URL_EMULATOR for emulator
            "STAGING" -> STAGING_BASE_URL
            "PRODUCTION" -> PRODUCTION_BASE_URL
            else -> CODESPACES_BASE_URL
        }

    /**
     * Get the base URL: a runtime override set via [setBaseUrlOverride] if present,
     * otherwise the compile-time default for [DEPLOYMENT_TYPE].
     */
    val BASE_URL: String
        get() = getBaseUrlOverride()?.takeIf { it.isNotBlank() } ?: compiledDefaultBaseUrl

    fun getBaseUrlOverride(): String? = prefs?.getString(KEY_BASE_URL_OVERRIDE, null)

    fun getDefaultBaseUrl(): String = compiledDefaultBaseUrl

    /** Pass null or blank to clear the override and go back to the compiled default. */
    fun setBaseUrlOverride(url: String?) {
        val cleaned = url?.trim()?.trimEnd('/')
        prefs?.edit()?.apply {
            if (cleaned.isNullOrBlank()) remove(KEY_BASE_URL_OVERRIDE) else putString(KEY_BASE_URL_OVERRIDE, cleaned)
        }?.apply()
    }

    /** ws:// or wss:// counterpart of BASE_URL, for real-time connections. */
    fun getWebSocketUrl(path: String): String {
        val wsBase = when {
            BASE_URL.startsWith("https://") -> "wss://" + BASE_URL.removePrefix("https://")
            BASE_URL.startsWith("http://") -> "ws://" + BASE_URL.removePrefix("http://")
            else -> BASE_URL
        }
        return "$wsBase$path"
    }

    /**
     * Every mobile action must generate a backend log with platform/device/app version
     * (V.6). Call on any HttpURLConnection before it's used, alongside the usual headers.
     */
    fun applyDeviceHeaders(connection: HttpURLConnection) {
        connection.setRequestProperty("X-Platform", "Android")
        connection.setRequestProperty("X-Device-Model", "${Build.MANUFACTURER} ${Build.MODEL}")
        connection.setRequestProperty("X-App-Version", BuildConfig.VERSION_NAME)
    }
    
    // 📡 API ENDPOINTS
    object Endpoints {
        // Auth endpoints - Updated to match your backend
        const val LOGIN = "/api/users/login/"
        const val SIGNUP = "/api/users/create/"
        const val SOCIAL_LOGIN = "/api/users/social-login/"
        
        // Home endpoint
        const val HOME = "/api/home/"
        
        // Music endpoints - NEW DYNAMIC ENDPOINTS
        const val MUSIC_SONGS = "/api/music/songs/"
        const val MUSIC_RANDOM_SONGS = "/api/music/random-songs/"
        const val MUSIC_RELATED_SONGS = "/api/music/related/"
        const val MUSIC_SONG_DETAIL = "/api/music/songs/"  // + {track_id}/
        
        // Events endpoints - Updated to match Swagger documentation
        const val GET_EVENTS = "/api/events/"
        const val CREATE_EVENT = "/api/events/create/"
        const val ACCEPT_EVENT_INVITE = "/api/events/{event_id}/accept-invite/"
        const val DECLINE_EVENT_INVITE = "/api/events/{event_id}/decline-invite/"
        const val INVITE_TO_EVENT = "/api/events/{event_id}/invite/"
    }
    
    // ⚙️ NETWORK SETTINGS
    object Settings {
        const val CONNECT_TIMEOUT = 30_000L // 30 seconds
        const val READ_TIMEOUT = 30_000L    // 30 seconds
        const val WRITE_TIMEOUT = 30_000L   // 30 seconds
    }
    
    // 🔍 HELPER METHODS
    /**
     * Get full URL for an endpoint
     */
    fun getFullUrl(endpoint: String): String {
        return "$BASE_URL$endpoint"
    }
    
    /**
     * Check if we're using Codespaces
     */
    fun isCodespaces(): Boolean = DEPLOYMENT_TYPE == "CODESPACES"
    
    /**
     * Check if we're in development mode
     */
    fun isDevelopment(): Boolean = DEPLOYMENT_TYPE in listOf("CODESPACES", "LOCAL")
    
    /**
     * Check if we're in production mode
     */
    fun isProduction(): Boolean = DEPLOYMENT_TYPE == "PRODUCTION"
    
    /**
     * Get current deployment type
     */
    fun getDeploymentType(): String = DEPLOYMENT_TYPE
    
    /**
     * Get current base URL for debugging
     */
    fun getCurrentBaseUrl(): String = BASE_URL
}