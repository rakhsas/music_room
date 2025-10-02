package com.example.musicroom.data.models

/**
 * Updated UserProfile data classes to match backend API
 */
data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String,
    val bio: String,
    val dateOfBirth: String?,
    val phoneNumber: String,
    val profilePrivacy: String, // "public", "friends", "private"
    val emailPrivacy: String,   // "public", "friends", "private"
    val phonePrivacy: String,   // "public", "friends", "private"
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

// Privacy options
enum class PrivacyLevel(val value: String, val displayName: String) {
    PUBLIC("public", "Public"),
    FRIENDS("friends", "Friends Only"),
    PRIVATE("private", "Private")
}

// Keep these for UI organization but map to the API structure
data class PublicInfo(
    val displayName: String,
    val username: String,
    val bio: String,
    val profilePictureUrl: String?
)

data class FriendsInfo(
    val email: String,
    val realName: String,
    val location: String
)

data class PrivateInfo(
    val phoneNumber: String,
    val birthDate: String?,
    val notes: String
)

data class MusicPreferences(
    val favoriteGenres: List<String>,
    val favoriteArtists: List<String>,
    val musicMood: String,
    val explicitContent: Boolean
)