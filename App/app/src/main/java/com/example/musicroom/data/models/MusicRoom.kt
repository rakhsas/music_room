package com.example.musicroom.data.models

data class MusicRoom(
    val id: String,
    val name: String,
    val host: String,
    val hostName: String = host, // Add this for compatibility
    val currentTrack: String?,
    val listeners: Int,
    val isLive: Boolean,
    val tags: List<String> = emptyList(),
    val description: String = "",
    val createdAt: String = ""
)

val activeRooms = listOf(
    MusicRoom(
        id = "1",
        name = "Chill Vibes",
        host = "Alex",
        hostName = "Alex",
        currentTrack = "Lofi Hip Hop",
        listeners = 12,
        isLive = true,
        tags = listOf("chill", "lofi", "ambient"),
        description = "Relaxing music for study and work",
        createdAt = "2024-01-15"
    ),
    MusicRoom(
        id = "2", 
        name = "Electronic Dance",
        host = "Sarah",
        hostName = "Sarah",
        currentTrack = "Progressive House Mix",
        listeners = 25,
        isLive = true,
        tags = listOf("electronic", "dance", "house"),
        description = "High energy electronic beats",
        createdAt = "2024-01-14"
    ),
    MusicRoom(
        id = "3",
        name = "Jazz Classics",
        host = "Mike",
        hostName = "Mike", 
        currentTrack = "Blue Note Sessions",
        listeners = 8,
        isLive = false,
        tags = listOf("jazz", "classic", "instrumental"),
        description = "Timeless jazz recordings",
        createdAt = "2024-01-13"
    )
)