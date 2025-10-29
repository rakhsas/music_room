package com.example.musicroom.data.network

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
    private const val LOCAL_BASE_URL_PHYSICAL = "http://10.49.84.49:8000"
    private const val STAGING_BASE_URL = "https://staging-api.musicroom.com"
    private const val PRODUCTION_BASE_URL = "https://api.musicroom.com"
    
    // 🎯 DEPLOYMENT TYPE
    // Choose your deployment: "CODESPACES", "LOCAL", "STAGING", "PRODUCTION"
    private const val DEPLOYMENT_TYPE = "LOCAL"
    
    /**
     * Get the base URL based on deployment type
     */
    val BASE_URL: String
        get() = when (DEPLOYMENT_TYPE) {
            "CODESPACES" -> CODESPACES_BASE_URL
            "LOCAL" -> LOCAL_BASE_URL_PHYSICAL // or LOCAL_BASE_URL_EMULATOR for emulator
            "STAGING" -> STAGING_BASE_URL
            "PRODUCTION" -> PRODUCTION_BASE_URL
            else -> CODESPACES_BASE_URL
        }
    
    // 📡 API ENDPOINTS
    object Endpoints {
        // Auth endpoints - Updated to match your backend
        const val LOGIN = "/api/users/login/"
        const val SIGNUP = "/api/users/create/"
        const val FORGOT_PASSWORD = "/auth/forgot-password/"
        const val GOOGLE_SIGNIN = "/auth/google/"
        
        // Home endpoint
        const val HOME = "/api/home/"
        
        // User endpoints
        const val USER_PROFILE = "/users/profile/"
        const val UPDATE_PROFILE = "/users/update/"
        
        // Music endpoints - NEW DYNAMIC ENDPOINTS
        const val MUSIC_SONGS = "/api/music/songs/"
        const val MUSIC_RANDOM_SONGS = "/api/music/random-songs/"
        const val MUSIC_RELATED_SONGS = "/api/music/related/"
        const val MUSIC_SONG_DETAIL = "/api/music/songs/"  // + {track_id}/
        
        // Legacy endpoints
        const val SEARCH_MUSIC = "/music/search/"
        const val GET_PLAYLISTS = "/playlists/"
        const val CREATE_PLAYLIST = "/playlists/create/"
        
        // Events endpoints - Updated to match Swagger documentation
        const val GET_EVENTS = "/api/events/"
        const val CREATE_EVENT = "/api/events/create/"
        const val JOIN_EVENT = "/events/join/"
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
