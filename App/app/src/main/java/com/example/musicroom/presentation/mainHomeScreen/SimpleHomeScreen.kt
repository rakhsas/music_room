package com.example.musicroom.presentation.mainHomeScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.musicroom.data.models.User
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavController
import androidx.navigation.NavHostController  // Add this import
import com.example.musicroom.presentation.events.EventsScreen
import com.example.musicroom.presentation.profile.ProfileScreen
import com.example.musicroom.presentation.theme.* 
import com.example.musicroom.presentation.room.*
import com.example.musicroom.presentation.playlist.PlaylistDetailsScreen
import com.example.musicroom.presentation.playlists.PublicPlaylistsScreen
import com.example.musicroom.presentation.home.HomeScreen  // Updated import to use new HomeScreen
import com.example.musicroom.presentation.player.NowPlayingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleHomeScreen(user: User, navController: NavController) {
    val innerNavController = rememberNavController()  // Create a separate nav controller for inner navigation
    
    Scaffold(
        bottomBar = { 
            SimpleBottomNav(
                currentRoute = innerNavController.currentBackStackEntryAsState().value?.destination?.route,
                onTabSelected = { route -> 
                    innerNavController.navigate(route) {
                        popUpTo(innerNavController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { 
                HomeScreen(navController = navController)  // Pass the main navController for navigation
            }
            // Change the composable route
            composable("events") { 
                EventsScreen(navController = navController) // Pass navController for navigation
            }
            // Change the composable route
            composable("playlist") { 
                PublicPlaylistsScreen(navController = navController) // Use main navController for playlist navigation
            }
            composable("profile") { 
                ProfileScreen(
                    user = user,
                    onNavigateToLogin = {
                        // Navigate back to auth screen and clear the entire backstack
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) 
            }
            
            composable(
                route = "room/{roomId}",
                arguments = listOf(navArgument("roomId") { type = NavType.StringType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                RoomDetailScreen(
                    roomId = roomId,
                    navController = innerNavController  // Use correct parameter name
                )
            }
            
            // Remove the conflicting playlist route - handled in MainActivity
        }
    }
}

@Composable
fun SimpleBottomNav(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onTabSelected("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                selectedTextColor = PrimaryPurple,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPurple.copy(alpha = 0.2f)
            )
        )
        // Change the NavigationBarItem
        NavigationBarItem(
            icon = { Icon(Icons.Default.Event, contentDescription = "Events") }, // Changed to event icon
            label = { Text("Events") }, // Changed to "Events"
            selected = currentRoute == "events", // Changed route name
            onClick = { onTabSelected("events") }, // Changed route name
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                selectedTextColor = PrimaryPurple,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPurple.copy(alpha = 0.2f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlist") }, // Changed to playlist icon
            label = { Text("Playlist") }, // Changed to "Playlist"
            selected = currentRoute == "playlist", // Changed route name
            onClick = { onTabSelected("playlist") }, // Changed route name
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                selectedTextColor = PrimaryPurple,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPurple.copy(alpha = 0.2f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { onTabSelected("profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                selectedTextColor = PrimaryPurple,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPurple.copy(alpha = 0.2f)
            )
        )
    }
}
