data class PlaylistResponse(
    val headers: PlaylistHeaders,
    val results: List<PlaylistDetails>
)

data class PlaylistHeaders(
    val status: String,
    val code: Int,
    val error_message: String,
    val warnings: String,
    val results_count: Int
)

data class PlaylistDetails(
    val id: String,
    val name: String,
    val creationdate: String,
    val user_id: String,
    val user_name: String,
    val zip: String,
    val tracks: List<PlaylistTrack>
)

data class PlaylistTrack(
    val id: String,
    val name: String,
    val album_id: String,
    val artist_id: String,
    val duration: String,
    val artist_name: String,
    val playlistadddate: String,
    val position: String,
    val license_ccurl: String,
    val album_image: String,
    val image: String,
    val audio: String,
    val audiodownload: String,
    val audiodownload_allowed: Boolean
)