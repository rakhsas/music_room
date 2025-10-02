/**
 * ========================================================================================
 * MAIN ACTIVITY - App Entry Point
 * ========================================================================================
 * 
 * This is the main activity that hosts the entire app with Jetpack Compose navigation.
 * Handles authentication flow, navigation, and app-wide state management.
 * 
 * 🎯 KEY RESPONSIBILITIES:
 * ========================================================================================
 * ✅ Navigation setup with Compose Navigation
 * ✅ Authentication flow management
 * ✅ Theme and surface configuration
 * ✅ Dependency injection with Hilt
 * ✅ Intent handling for app startup
 * 
 * 🗺️ NAVIGATION STRUCTURE:
 * ========================================================================================
 * /splash          → SplashScreen (app startup)
 * /onboarding      → OnboardingScreen (first-time user experience)  
 * /auth            → AuthContainer (login/signup flow)
 * /home            → SimpleHomeScreen (main app dashboard)
 * /music_search    → MusicSearchScreen (search for tracks)
 * /playlist/{id}   → PlaylistDetailsScreen (view playlist details)
 * /now_playing     → NowPlayingScreen (music player interface)
 * 
 * 🔄 AUTHENTICATION FLOW:
 * ========================================================================================
 * 1. App starts → SplashScreen
 * 2. Check if first launch → OnboardingScreen (optional)
 * 3. Check authentication → AuthContainer or HomeScreen
 * 4. User completes login → Navigate to HomeScreen
 * 5. All other screens accessible from HomeScreen
 * 
 * 🧩 DEPENDENCY INJECTION:
 * ========================================================================================
 * @AndroidEntryPoint enables Hilt dependency injection
 * All ViewModels, services, and repositories auto-injected
 * 
 * 💡 FOR DEVELOPERS:
 * ========================================================================================
 * - Add new screens by creating composable routes in NavHost
 * - Authentication state managed by AuthContainer
 * - Navigation arguments handled with type safety
 * - Deep linking can be added by configuring intent filters
 * ========================================================================================
 */

package com.example.musicroom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.musicroom.presentation.mainHomeScreen.SimpleHomeScreen
import com.example.musicroom.presentation.music.MusicSearchScreen
import com.example.musicroom.presentation.artist.ArtistDetailsScreen
import com.example.musicroom.presentation.theme.MusicRoomTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.musicroom.presentation.auth.AuthContainer
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import com.example.musicroom.presentation.splash.SplashScreen
import com.example.musicroom.presentation.onboarding.OnboardingScreen
import com.example.musicroom.presentation.playlist.PlaylistDetailsScreen
import com.example.musicroom.presentation.player.NowPlayingScreen
import com.example.musicroom.data.models.Track
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.example.musicroom.presentation.playlist.PlaylistTracksScreen
import com.example.musicroom.presentation.playlists.PublicPlaylistsScreen // Fixed: correct package path
import com.example.musicroom.presentation.events.EventDetailsScreen // Add this import with the other presentation imports
import com.example.musicroom.presentation.auth.ForgotPasswordScreen // Add import for ForgotPasswordScreen

@AndroidEntryPoint // Enable Hilt dependency injection for this activity
class MainActivity : ComponentActivity() {
    
    // ============================================================================
    // INTENT HANDLING - For future deep linking or external app launches
    // ============================================================================
    private var currentIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent = intent // Store initial intent for processing
        
        // ========================================================================
        // COMPOSE UI SETUP - Main app content with theme
        // ========================================================================
        setContent {
            MusicRoomTheme { // Apply app-wide theme and color scheme
                val navController = rememberNavController()
                var hasSeenOnboarding by remember { mutableStateOf(false) }

                // Handle deep links                // Deep link handling removed - using simplified authentication
                
                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            onNavigateToOnboarding = { navController.navigate("onboarding") }
                        )
                    }
                    
                    composable("onboarding") {
                        OnboardingScreen(
                            onFinish = {
                                hasSeenOnboarding = true
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    composable("auth") {
                        AuthContainer(
                            onLoginSuccess = {
                                // Simply navigate to home on successful login
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                      
                    composable("home") {
                        // Create a dummy user since we removed user management from auth
                        val dummyUser = com.example.musicroom.data.models.User(
                            id = "dummy_user",
                            name = "User",
                            username = "user",
                            photoUrl = "",
                            email = "user@example.com"
                        )
                        SimpleHomeScreen(user = dummyUser, navController = navController)
                    }
                      // Music Search Screen
                    composable("music_search") {
                        MusicSearchScreen(navController = navController)
                    }
                    
                    // Now Playing Screen
                    composable(
                        route = "now_playing/{trackId}/{trackTitle}/{trackArtist}/{trackThumbnailUrl}/{trackDuration}/{trackDescription}",
                        arguments = listOf(
                            navArgument("trackId") { type = NavType.StringType },
                            navArgument("trackTitle") { type = NavType.StringType },
                            navArgument("trackArtist") { type = NavType.StringType },
                            navArgument("trackThumbnailUrl") { type = NavType.StringType },
                            navArgument("trackDuration") { type = NavType.StringType },
                            navArgument("trackDescription") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
                        val trackTitle = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("trackTitle") ?: "", "UTF-8")
                        val trackArtist = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("trackArtist") ?: "", "UTF-8")
                        val trackThumbnailUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("trackThumbnailUrl") ?: "", "UTF-8")
                        val trackDuration = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("trackDuration") ?: "", "UTF-8")
                        val trackDescription = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("trackDescription") ?: "", "UTF-8")
                        
                        val track = com.example.musicroom.data.models.Track(
                            id = trackId,
                            title = trackTitle,
                            artist = trackArtist,
                            thumbnailUrl = trackThumbnailUrl,
                            duration = trackDuration,
                            description = trackDescription // This will contain the audio URL
                        )
                        
                        NowPlayingScreen(
                            track = track,
                            navController = navController
                        )
                    }
                      // Playlists List Screen
                    composable("playlists") {
                        PublicPlaylistsScreen(navController = navController)
                    }
                    
                    // Playlist Tracks Screen - Single route for viewing playlist songs
                    composable(
                        route = "playlist_tracks/{playlistId}",
                        arguments = listOf(
                            navArgument("playlistId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                        PlaylistTracksScreen(
                            playlistId = playlistId,
                            navController = navController
                        )
                    }
                    
                    // Artist Details Screen
                    composable(
                        route = "artist/{artistId}",
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                        ArtistDetailsScreen(
                            artistId = artistId,
                            navController = navController
                        )
                    }
                    
                    // Event Details Screen
                    composable(
                        route = "event_details/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                        EventDetailsScreen(
                            eventId = eventId,
                            navController = navController
                        )
                    }
                    
                    // Forgot Password Screen
                    composable("forgot_password") {
                        ForgotPasswordScreen(
                            onBackToLoginClick = {
                                navController.navigate("login") {
                                    popUpTo("forgot_password") { inclusive = true }
                                }
                            },
                            onPasswordResetComplete = {
                                // Navigate back to login with success message
                                navController.navigate("login") {
                                    popUpTo("forgot_password") { inclusive = true }
                                }
                                // You can add a success message here if needed
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        currentIntent = intent
        Log.d("MainActivity", "� Deep link handling removed - using simplified authentication")
    }
}