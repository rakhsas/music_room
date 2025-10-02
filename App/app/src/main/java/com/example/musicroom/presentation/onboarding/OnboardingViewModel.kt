package com.example.musicroom.presentation.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : ViewModel() {
    private val _hasSeenOnboarding = MutableStateFlow(false)
    val hasSeenOnboarding = _hasSeenOnboarding.asStateFlow()

    fun completeOnboarding() {
        _hasSeenOnboarding.value = true
    }
}