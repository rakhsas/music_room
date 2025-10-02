package com.example.musicroom.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.models.UserProfile
import com.example.musicroom.data.repository.UserProfileRepository
import com.example.musicroom.data.service.AuthApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            userProfileRepository.getUserProfile()
                .onSuccess { userProfile ->
                    _uiState.value = _uiState.value.copy(
                        userProfile = userProfile,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load profile"
                    )
                }
        }
    }
    
    fun updateProfile(
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
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)
            
            userProfileRepository.updateUserProfile(
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
                .onSuccess { userProfile ->
                    _uiState.value = _uiState.value.copy(
                        userProfile = userProfile,
                        isUpdating = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = error.message ?: "Failed to update profile"
                    )
                }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, error = null)
            
            val refreshToken = tokenManager.getRefreshToken()
            
            if (refreshToken != null) {
                // Call logout API to blacklist refresh token
                authApiService.logout(refreshToken)
                    .onSuccess { response ->
                        // Clear all tokens from storage
                        tokenManager.clearTokens()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoggingOut = false,
                            logoutSuccess = true,
                            error = null
                        )
                    }
                    .onFailure { error ->
                        // Even if API fails, clear tokens locally
                        tokenManager.clearTokens()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoggingOut = false,
                            logoutSuccess = true, // Still consider it success since tokens are cleared
                            error = null
                        )
                    }
            } else {
                // No refresh token, just clear tokens
                tokenManager.clearTokens()
                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = true,
                    error = null
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearLogoutSuccess() {
        _uiState.value = _uiState.value.copy(logoutSuccess = false)
    }
}

data class ProfileUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val error: String? = null
)