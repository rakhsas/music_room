package com.example.musicroom.data.auth

data class GoogleUserInfo(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val email: String?,
    val idToken: String? = null
)
