package com.example.musicroom.data.repository

import com.example.musicroom.data.models.ArtistsApiResponse
import retrofit2.http.GET

interface ArtistApiService {
    @GET("artists")
    suspend fun getPopularArtists(): ArtistsApiResponse
}