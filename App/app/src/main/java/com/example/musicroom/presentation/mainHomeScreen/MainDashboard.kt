package com.example.musicroom.presentation.mainHomeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicroom.data.models.User
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.musicroom.presentation.events.EventsScreen
import com.example.musicroom.presentation.profile.ProfileScreen
import com.example.musicroom.presentation.theme.* 
import com.example.musicroom.presentation.room.*
import com.example.musicroom.presentation.playlist.PlaylistDetailsScreen
import com.example.musicroom.presentation.playlists.PublicPlaylistsScreen
import com.example.musicroom.presentation.home.DashboardScreen
import com.example.musicroom.presentation.player.NowPlayingScreen

/**
 * ========================================================================================
 * MAIN DASHBOARD - App Navigation Hub
 * ========================================================================================
 * 
 * Modernized main dashboard with bottom navigation and modern teal theme.
 * Central navigation hub for the entire application.
 * 
 * 🎯 KEY FEATURES:
 * ========================================================================================
 * ✅ Modern bottom navigation with teal accent
 * ✅ Inner navigation for main sections
 * ✅ Smooth transitions between tabs
 * ✅ Updated color scheme
 * ✅ Enhanced UX with modern design
 * 
 * 🎨 DESIGN UPDATES:
 * ========================================================================================
 * - New teal/cyan color scheme
 * - Modern bottom navigation design
 * - Enhanced navigation animations
 * - Glassmorphism navigation bar
 * ========================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(user: User, navController: NavController) {
    val innerNavController = rememberNavController()
    
    Scaffold(
        bottomBar = { 
            ModernBottomNav(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            NavHost(
                navController = innerNavController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") { 
                    DashboardScreen(navController = navController)
                }
                
                composable("events") { 
                    EventsScreen(navController = navController)
                }
                
                composable("playlist") { 
                    PublicPlaylistsScreen(navController = navController)
                }
                
                composable("profile") { 
                    ProfileScreen(
                        user = user,
                        onNavigateToLogin = {
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
                        navController = innerNavController
                    )
                }
            }
        }
    }
}

/**
 * Modern Bottom Navigation with Teal Theme
 */
@Composable
fun ModernBottomNav(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        // Home Tab
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Home",
                    fontSize = 12.sp
                )
            },
            selected = currentRoute == "home",
            onClick = { onTabSelected("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryTeal.copy(alpha = 0.2f)
            )
        )
        
        // Events Tab
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Event,
                    contentDescription = "Events",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Events",
                    fontSize = 12.sp
                )
            },
            selected = currentRoute == "events",
            onClick = { onTabSelected("events") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryTeal.copy(alpha = 0.2f)
            )
        )
        
        // Playlist Tab
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = "Playlist",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Playlists",
                    fontSize = 12.sp
                )
            },
            selected = currentRoute == "playlist",
            onClick = { onTabSelected("playlist") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryTeal.copy(alpha = 0.2f)
            )
        )
        
        // Profile Tab
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Profile",
                    fontSize = 12.sp
                )
            },
            selected = currentRoute == "profile",
            onClick = { onTabSelected("profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryTeal.copy(alpha = 0.2f)
            )
        )
    }
}

