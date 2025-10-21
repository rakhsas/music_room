package com.example.musicroom.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.log
import android.util.Log

sealed class AuthScreenState {
    object Login : AuthScreenState()
    object SignUp : AuthScreenState()
    object ForgotPassword : AuthScreenState()
}

@Composable
fun AuthContainer(onLoginSuccess: () -> Unit) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    var currentScreen by remember { mutableStateOf<AuthScreenState>(AuthScreenState.Login) }
    var forgotPasswordStep by remember { mutableStateOf(PasswordResetStep.EMAIL_INPUT) }
    // Handle successful authentication
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.LoginSuccess -> {
                onLoginSuccess()
                authViewModel.clearState()
            }
            is AuthState.SignUpSuccess -> {
                // Navigate back to login screen after successful signup
                currentScreen = AuthScreenState.Login
                authViewModel.clearState()
            }
            is AuthState.GoogleSignInSuccess -> {
                // Navigate to home after successful Google sign-in
                onLoginSuccess()
                authViewModel.clearState()
            }
            else -> { /* No action needed */ }
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == AuthScreenState.ForgotPassword) {
            forgotPasswordStep = PasswordResetStep.EMAIL_INPUT
            Log.d("AuthViewModel", "Resetting forgot password step to EMAIL_INPUT")
        }
    }
    
    when (currentScreen) {
        AuthScreenState.Login -> {
            LoginView(
                onLoginSuccess = onLoginSuccess,
                onSignUpClick = { 
                    currentScreen = AuthScreenState.SignUp 
                },
                onForgotPasswordClick = { 
                    currentScreen = AuthScreenState.ForgotPassword 
                },
                viewModel = authViewModel
            )
        }
        
        AuthScreenState.SignUp -> {
            RegistrationView(
                onBackToLoginClick = { 
                    currentScreen = AuthScreenState.Login 
                },
                viewModel = authViewModel
            )
        }
        
        AuthScreenState.ForgotPassword -> {
            ForgotPasswordScreen(
                onBackToLoginClick = { 
                    currentScreen = AuthScreenState.Login
                },
                onPasswordResetComplete = {
                    currentScreen = AuthScreenState.Login
                },
                initialStep = forgotPasswordStep, // Pass the current step
                onStepChange = { newStep -> forgotPasswordStep = newStep },
                viewModel = authViewModel
            )
        }
    }
}
