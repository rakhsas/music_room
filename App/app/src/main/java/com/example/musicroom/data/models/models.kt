package com.example.musicroom.data.models

/**
 * Data class representing a user in the application
 *
 * @property id Unique identifier for the user
 * @property name Display name of the user
 * @property photoUrl URL to the user's profile photo
 * @property email User's email address
 */
data class User(
    val id: String,
    val name: String,
    val username: String,
    val photoUrl: String,
    val email: String = "",
)

/**
 * Data classes for API responses - keeping only non-conflicting ones
 */
data class ArtistsApiResponse(
    val popular_artists: PopularArtists
)

data class PopularArtists(
    val headers: com.example.musicroom.data.models.ApiHeaders, // Reference the one in HomeModels
    val results: List<com.example.musicroom.data.models.Artist> // Reference the one in HomeModels
)
