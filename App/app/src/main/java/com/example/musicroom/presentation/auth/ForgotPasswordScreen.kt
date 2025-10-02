package com.example.musicroom.presentation.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicroom.presentation.theme.*
enum class PasswordResetStep {
    EMAIL_INPUT,
    OTP_VERIFICATION,
    NEW_PASSWORD
}

@Composable
fun ForgotPasswordScreen(
    onBackToLoginClick: () -> Unit,
    onPasswordResetComplete: () -> Unit,
    initialStep: PasswordResetStep = PasswordResetStep.EMAIL_INPUT,
    onStepChange: (PasswordResetStep) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(initialStep) }

    LaunchedEffect(initialStep) {
        currentStep = initialStep
        Log.d("ForgotPasswordScreen", "Setting current step to $initialStep")
    }

    // Notify parent of step changes
    LaunchedEffect(currentStep) {
        onStepChange(currentStep)
        Log.d("ForgotPasswordScreen", "Current step changed to $currentStep")
    }

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()
    // Handle auth state changes
    LaunchedEffect(currentStep) {
        if (currentStep == PasswordResetStep.EMAIL_INPUT) {
            email = ""
            otp = ""
            newPassword = ""
            confirmPassword = ""
            passwordVisible = false
            confirmPasswordVisible = false
        }
    }
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.PasswordResetOTPSent -> {
                currentStep = PasswordResetStep.OTP_VERIFICATION
            }
            is AuthState.OTPVerified -> {
                currentStep = PasswordResetStep.NEW_PASSWORD
            }
            is AuthState.PasswordResetComplete -> {
                onPasswordResetComplete()
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Button
            IconButton(
                onClick = onBackToLoginClick,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            when (currentStep) {
                PasswordResetStep.EMAIL_INPUT -> {
                    EmailInputStep(
                        email = email,
                        onEmailChange = { email = it },
                        onContinue = { viewModel.requestPasswordResetOTP(email) },
                        authState = authState
                    )
                }
                PasswordResetStep.OTP_VERIFICATION -> {
                    OTPVerificationStep(
                        email = email,
                        otp = otp,
                        onOTPChange = { otp = it },
                        onVerify = { viewModel.verifyPasswordResetOTP(email, otp) },
                        onResendOTP = { viewModel.requestPasswordResetOTP(email) },
                        authState = authState
                    )
                }
                PasswordResetStep.NEW_PASSWORD -> {
                    NewPasswordStep(
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        passwordVisible = passwordVisible,
                        confirmPasswordVisible = confirmPasswordVisible,
                        onNewPasswordChange = { newPassword = it },
                        onConfirmPasswordChange = { confirmPassword = it },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        onResetPassword = { 
                            viewModel.resetPasswordWithOTP(email, otp, newPassword, confirmPassword)
                        },
                        authState = authState
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Back to Login
            TextButton(onClick = onBackToLoginClick) {
                Text(
                    "Retour",
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmailInputStep(
    email: String,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    authState: AuthState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Réinitialiser le mot de passe",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Saisissez votre adresse e-mail et nous vous enverrons un OTP à 6 chiffres pour réinitialiser votre mot de passe",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Email Input
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Email,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = TextSecondary
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Send OTP Button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryPurple
            ),
            enabled = email.isNotBlank() && authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Envoyer un OTP")
            }
        }
        
        // Show error message
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun OTPVerificationStep(
    email: String,
    otp: String,
    onOTPChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResendOTP: () -> Unit,
    authState: AuthState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Entrez OTP",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Nous avons envoyé un OTP à 6 chiffres à $email",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // OTP Input
        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) onOTPChange(it) },
            label = { Text("OTP à 6 chiffres") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Pin,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = TextSecondary
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Verify OTP Button
        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryPurple
            ),
            enabled = otp.length == 6 && authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Vérifier l'OTP")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resend OTP Button
        TextButton(
            onClick = onResendOTP,
            enabled = authState !is AuthState.Loading
        ) {
            Text(
                "Renvoyer l'OTP",
                color = PrimaryPurple
            )
        }
        
        // Show success message
        if (authState is AuthState.PasswordResetOTPSent) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Show error message
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NewPasswordStep(
    newPassword: String,
    confirmPassword: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onResetPassword: () -> Unit,
    authState: AuthState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Définir un nouveau mot de passe",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "nouveau mot de passe",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // New Password Input
        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = { Text("Nouveau mot de passe") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null
                )
            },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = TextSecondary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Confirm Password Input
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirmez le mot de passe") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null
                )
            },
            trailingIcon = {
                IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = TextSecondary
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Reset Password Button
        Button(
            onClick = onResetPassword,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryPurple
            ),
            enabled = newPassword.isNotBlank() && 
                     confirmPassword.isNotBlank() && 
                     newPassword == confirmPassword &&
                     newPassword.length >= 8 &&
                     authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Réinitialiser")
            }
        }
        
        // Show validation hints
        if (newPassword.isNotBlank() && newPassword.length < 8) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Le mot de passe doit comporter au moins 8 caractères",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        if (confirmPassword.isNotBlank() && newPassword != confirmPassword) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Les mots de passe ne correspondent pas",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Show error message
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}