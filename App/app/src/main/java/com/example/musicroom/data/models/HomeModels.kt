package com.example.musicroom.data.models

/**
 * Data models for the home screen API response
 * Based on the backend /home/ endpoint structure
 */

/**
 * Main response wrapper for home data
 */
data class HomeResponse(
    val user_playlists: PlaylistsSection,
    val recommended_songs: SongsSection,
    val popular_songs: SongsSection,
    val recently_listened: SongsSection,
    val popular_artists: ArtistsSection,
    val events: List<Event>,
    val notifications: NotificationsSection
)

/**
 * Common headers structure for API responses
 */
data class ApiHeaders(
    val status: String,
    val code: Int,
    val error_message: String,
    val warnings: String,
    val results_count: Int,
    val next: String?
)

/**
 * Notifications section
 */
data class NotificationsSection(
    val event_notifications: List<EventNotification>,
    val playlist_notifications: List<PlaylistNotification>
)

/**
 * Event notification model
 */
data class EventNotification(
    val event_id: String,
    val inviter_name: String,
    val message: String,
    val event_title: String? = null
)

/**
 * Playlist notification model
 */
data class PlaylistNotification(
    val playlist_id: String,
    val inviter_name: String,
    val message: String,
    val playlist_name: String? = null
)

/**
 * Playlists section
 */
data class PlaylistsSection(
    val headers: ApiHeaders?,
    val results: List<Playlist>
)

/**
 * Playlist model
 */
data class Playlist(
    val id: String,
    val name: String,
    val creationdate: String,
    val user_id: String,
    val user_name: String,
    val zip: String?,
    val shorturl: String,
    val shareurl: String
)

/**
 * Songs section
 */
data class SongsSection(
    val headers: ApiHeaders?,
    val results: List<Song>
)

/**
 * Song model
 */
data class Song(
    val id: String,
    val name: String,
    val duration: Int,
    val artist_id: String,
    val artist_name: String,
    val artist_idstr: String,
    val album_name: String,
    val album_id: String,
    val license_ccurl: String?,
    val position: Int,
    val releasedate: String,
    val album_image: String?,
    val audio: String,
    val audiodownload: String?,
    val prourl: String?,
    val shorturl: String,
    val shareurl: String,
    val waveform: String?,
    val image: String?,
    val audiodownload_allowed: Boolean?
)

/**
 * Artists section
 */
data class ArtistsSection(
    val headers: ApiHeaders?,
    val results: List<Artist>
)

/**
 * Artist model
 */
data class Artist(
    val id: String,
    val name: String,
    val website: String?,
    val joindate: String,
    val image: String?,
    val shorturl: String,
    val shareurl: String
)

/**
 * Event model - Updated to match backend Events model
 */
data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val location: String,
    val organizer: EventOrganizer,
    val attendee_count: Int,
    val track_count: Int,
    val is_public: Boolean,
    val event_start_time: String,
    val event_end_time: String?,
    val image_url: String?,
    val created_at: String,
    val current_user_role: String? = null
)

/**
 * Event organizer info
 */
data class EventOrganizer(
    val id: String,
    val name: String,
    val avatar: String?
)

/**
 * Events section for home screen
 */
data class EventsSection(
    val headers: ApiHeaders?,
    val results: List<Event>
)