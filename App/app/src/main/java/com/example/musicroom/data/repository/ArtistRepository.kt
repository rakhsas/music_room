package com.example.musicroom.data.repository

import com.example.musicroom.data.models.Artist
import com.example.musicroom.data.models.ArtistsApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ArtistRepository {
    // For simplicity, using mock data instead of actual API calls
    suspend fun getPopularArtists(): List<Artist> = withContext(Dispatchers.IO) {
        // In a real app, you would use Retrofit to make the API call
        // val apiService = createApiService()
        // val response = apiService.getPopularArtists()
        // return@withContext response.popular_artists.results
        
        // For now, return mock data to avoid API setup
        listOf(
            Artist(
                id = "5",
                name = "Both",
                website = "http://www.both-world.com",
                joindate = "2004-07-04",
                image = "https://usercontent.jamendo.com?type=artist&id=5&width=300",
                shorturl = "https://jamen.do/a/5",
                shareurl = "https://www.jamendo.com/artist/5"
            ),
            Artist(
                id = "6",
                name = "Westing*House",
                website = "http://web.archive.org/web/20010814230248/westinghouse.free.fr/800/index.htm",
                joindate = "2004-07-08",
                image = "",
                shorturl = "https://jamen.do/a/6",
                shareurl = "https://www.jamendo.com/artist/6"
            ),
            Artist(
                id = "7",
                name = "TriFace",
                website = "http://www.triface.net/",
                joindate = "2004-07-19",
                image = "https://usercontent.jamendo.com?type=artist&id=7&width=300",
                shorturl = "https://jamen.do/a/7",
                shareurl = "https://www.jamendo.com/artist/7"
            ),
            Artist(
                id = "9",
                name = "Skaut",
                website = "http://www.skaut.fr.st",
                joindate = "2004-07-28",
                image = "",
                shorturl = "https://jamen.do/a/9",
                shareurl = "https://www.jamendo.com/artist/9"
            ),
            Artist(
                id = "13",
                name = "echo lali",
                website = "http://www.echolali.fr",
                joindate = "2004-12-21",
                image = "https://usercontent.jamendo.com?type=artist&id=13&width=300",
                shorturl = "https://jamen.do/a/13",
                shareurl = "https://www.jamendo.com/artist/13"
            )
        )
    }
    
    // This method would be used in a real implementation
    private fun createApiService(): ArtistApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ArtistApiService::class.java)
    }
}