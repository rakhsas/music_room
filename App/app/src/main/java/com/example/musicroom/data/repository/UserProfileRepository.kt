package com.example.musicroom.data.repository

import com.example.musicroom.data.models.UserProfile
import com.example.musicroom.data.service.UserProfileApiService
import com.example.musicroom.data.service.UserProfileResponse
import com.example.musicroom.data.service.UpdateUserProfileRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val userProfileApiService: UserProfileApiService
) {
    
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val result = userProfileApiService.getUserProfile()
            if (result.isSuccess) {
                val apiResponse = result.getOrThrow()
                val userProfile = mapApiResponseToUserProfile(apiResponse)
                Result.success(userProfile)
            } else {
                result.map { mapApiResponseToUserProfile(it) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(
        name: String? = null,
        bio: String? = null,
        dateOfBirth: String? = null,
        phoneNumber: String? = null,
        profilePrivacy: String? = null,
        emailPrivacy: String? = null,
        phonePrivacy: String? = null,
        musicPreferences: List<String>? = null,
        likedArtists: List<String>? = null,
        likedAlbums: List<String>? = null,
        likedSongs: List<String>? = null,
        genres: List<String>? = null
    ): Result<UserProfile> {
        return try {
            val request = UpdateUserProfileRequest(
                name = name,
                bio = bio,
                dateOfBirth = dateOfBirth,
                phoneNumber = phoneNumber,
                profilePrivacy = profilePrivacy,
                emailPrivacy = emailPrivacy,
                phonePrivacy = phonePrivacy,
                musicPreferences = musicPreferences,
                likedArtists = likedArtists,
                likedAlbums = likedAlbums,
                likedSongs = likedSongs,
                genres = genres
            )
            
            val result = userProfileApiService.updateUserProfile(request)
            if (result.isSuccess) {
                val apiResponse = result.getOrThrow()
                val userProfile = mapApiResponseToUserProfile(apiResponse)
                Result.success(userProfile)
            } else {
                result.map { mapApiResponseToUserProfile(it) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun mapApiResponseToUserProfile(apiResponse: UserProfileResponse): UserProfile {
        return UserProfile(
            id = apiResponse.id.toString(),
            email = apiResponse.email,
            name = apiResponse.name,
            avatar = apiResponse.avatar,
            bio = apiResponse.bio,
            dateOfBirth = apiResponse.dateOfBirth,
            phoneNumber = apiResponse.phoneNumber,
            profilePrivacy = apiResponse.profilePrivacy,
            emailPrivacy = apiResponse.emailPrivacy,
            phonePrivacy = apiResponse.phonePrivacy,
            subscriptionType = apiResponse.subscriptionType,
            isPremium = apiResponse.isPremium,
            isSubscribed = apiResponse.isSubscribed,
            musicPreferences = apiResponse.musicPreferences,
            likedArtists = apiResponse.likedArtists,
            likedAlbums = apiResponse.likedAlbums,
            likedSongs = apiResponse.likedSongs,
            genres = apiResponse.genres,
            createdAt = apiResponse.createdAt
        )
    }
}