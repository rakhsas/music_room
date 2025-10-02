package com.example.musicroom.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicroom.data.models.*
import com.example.musicroom.presentation.theme.*
import java.util.Locale

enum class ProfileSection {
    PUBLIC_INFO,
    FRIENDS_INFO,
    MUSIC_PREFERENCES
}

@Composable
fun ProfileScreen(
    user: User,
    onNavigateToLogin: () -> Unit = {}, // Keep this name for backward compatibility, even though it goes to auth
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<ProfileSection?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }
    
    // Handle logout success
    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onNavigateToLogin()
            viewModel.clearLogoutSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading profile",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadUserProfile() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.userProfile != null -> {
                ProfileContent(
                    userProfile = uiState.userProfile!!,
                    isUpdating = uiState.isUpdating,
                    isLoggingOut = uiState.isLoggingOut,
                    selectedSection = selectedSection,
                    onSectionSelected = { selectedSection = it },
                    onLogoutClick = { showLogoutDialog = true },
                    onUpdateProfile = { name, bio, dateOfBirth, phoneNumber, musicPreferences, likedArtists, likedAlbums, likedSongs, genres ->
                        viewModel.updateProfile(
                            name = name,
                            bio = bio,
                            dateOfBirth = dateOfBirth,
                            phoneNumber = phoneNumber,
                            profilePrivacy = null,
                            emailPrivacy = null,
                            phonePrivacy = null,
                            musicPreferences = musicPreferences,
                            likedArtists = likedArtists,
                            likedAlbums = likedAlbums,
                            likedSongs = likedSongs,
                            genres = genres
                        )
                    }
                )
            }
        }
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                viewModel.logout()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false },
            isLoggingOut = uiState.isLoggingOut
        )
    }
    
    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
}

@Composable
private fun ProfileContent(
    userProfile: UserProfile,
    isUpdating: Boolean,
    isLoggingOut: Boolean,
    selectedSection: ProfileSection?,
    onSectionSelected: (ProfileSection?) -> Unit,
    onLogoutClick: () -> Unit,
    onUpdateProfile: (
        name: String?,
        bio: String?,
        dateOfBirth: String?,
        phoneNumber: String?,
        musicPreferences: List<String>?,
        likedArtists: List<String>?,
        likedAlbums: List<String>?,
        likedSongs: List<String>?,
        genres: List<String>?,
    ) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = if (userProfile.avatar.isNotBlank() && userProfile.avatar != "default_avatar.png") {
                    "https://your-api-url.com/media/${userProfile.avatar}"
                } else {
                    null
                },
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(DarkSurface),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userProfile.name.ifBlank { "Unknown User" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = userProfile.email,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            if (userProfile.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userProfile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            
            // Premium Badge
            if (userProfile.isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                Badge(
                    containerColor = PrimaryPurple
                ) {
                    Text(
                        text = "Premium",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Sections
        ProfileSectionCard(
            title = "Public Information",
            description = "Name and bio",
            icon = Icons.Default.Person,
            onClick = { onSectionSelected(ProfileSection.PUBLIC_INFO) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileSectionCard(
            title = "Contact Information",
            description = "Phone and date of birth",
            icon = Icons.Default.ContactMail,
            onClick = { onSectionSelected(ProfileSection.FRIENDS_INFO) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileSectionCard(
            title = "Music Preferences",
            description = "Your favorite genres and artists",
            icon = Icons.Default.MusicNote,
            onClick = { onSectionSelected(ProfileSection.MUSIC_PREFERENCES) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Logout Button Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkError.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = DarkError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sign out of your account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onLogoutClick,
                    enabled = !isLoggingOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkError,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Signing Out...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }

    // Simple edit dialogs without external dependencies
    selectedSection?.let { section ->
        EditDialog(
            section = section,
            userProfile = userProfile,
            onDismiss = { onSectionSelected(null) },
            onSave = { updatedData ->
                when (section) {
                    ProfileSection.PUBLIC_INFO -> {
                        onUpdateProfile(
                            updatedData["name"], 
                            updatedData["bio"], 
                            null, null, null, null, null, null, null
                        )
                    }
                    ProfileSection.FRIENDS_INFO -> {
                        onUpdateProfile(
                            null, null, 
                            updatedData["dateOfBirth"], 
                            updatedData["phoneNumber"], 
                            null, null, null, null, null
                        )
                    }
                    ProfileSection.MUSIC_PREFERENCES -> {
                        val genres = updatedData["genres"]?.split(",")?.map { it.trim() }
                        val artists = updatedData["artists"]?.split(",")?.map { it.trim() }
                        onUpdateProfile(
                            null, null, null, null,
                            genres, artists, null, null, genres
                        )
                    }
                }
                onSectionSelected(null)
            }
        )
    }
}

@Composable
private fun EditDialog(
    section: ProfileSection,
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    var fields by remember { mutableStateOf(getInitialFields(section, userProfile)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (section) {
                    ProfileSection.PUBLIC_INFO -> "Edit Public Information"
                    ProfileSection.FRIENDS_INFO -> "Edit Contact Information"
                    ProfileSection.MUSIC_PREFERENCES -> "Edit Music Preferences"
                },
                color = TextPrimary
            )
        },
        text = {
            Column {
                when (section) {
                    ProfileSection.PUBLIC_INFO -> {
                        OutlinedTextField(
                            value = fields["name"] ?: "",
                            onValueChange = { fields = fields.toMutableMap().apply { put("name", it) } },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fields["bio"] ?: "",
                            onValueChange = { fields = fields.toMutableMap().apply { put("bio", it) } },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                    ProfileSection.FRIENDS_INFO -> {
                        OutlinedTextField(
                            value = fields["phoneNumber"] ?: "",
                            onValueChange = { fields = fields.toMutableMap().apply { put("phoneNumber", it) } },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fields["dateOfBirth"] ?: "",
                            onValueChange = { fields = fields.toMutableMap().apply { put("dateOfBirth", it) } },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ProfileSection.MUSIC_PREFERENCES -> {
                        OutlinedTextField(
                            value = fields["genres"] ?: "",
                            onValueChange = { fields = fields.toMutableMap().apply { put("genres", it) } },
                            label = { Text("Favorite Genres (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fields["artists"] ?: "",
                            onValueChange = { fields = fields.toMutableMap().apply { put("artists", it) } },
                            label = { Text("Favorite Artists (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fields) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

private fun getInitialFields(section: ProfileSection, userProfile: UserProfile): Map<String, String> {
    return when (section) {
        ProfileSection.PUBLIC_INFO -> mapOf(
            "name" to userProfile.name,
            "bio" to userProfile.bio
        )
        ProfileSection.FRIENDS_INFO -> mapOf(
            "phoneNumber" to userProfile.phoneNumber,
            "dateOfBirth" to (userProfile.dateOfBirth ?: "")
        )
        ProfileSection.MUSIC_PREFERENCES -> mapOf(
            "genres" to userProfile.genres.joinToString(", "),
            "artists" to userProfile.likedArtists.joinToString(", ")
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoggingOut: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isLoggingOut) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout",
                tint = DarkError,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Sign Out",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to sign out of your account? You'll need to log in again to access your music and playlists.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoggingOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkError,
                    contentColor = Color.White
                )
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signing Out...")
                } else {
                    Text("Sign Out")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoggingOut
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}