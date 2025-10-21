package com.example.musicroom.presentation.auth

import android.util.Patterns
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicroom.presentation.theme.*
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * ========================================================================================
 * REGISTRATION VIEW - Modern Sign Up Screen
 * ========================================================================================
 * 
 * Modernized registration screen with teal/cyan theme and enhanced UX.
 * Features glassmorphism design, password strength indicator, and smooth animations.
 * 
 * 🎯 KEY FEATURES:
 * ========================================================================================
 * ✅ User registration with full name, email, and password
 * ✅ Real-time form validation
 * ✅ Password strength indicator
 * ✅ Modern teal/cyan gradient theme
 * ✅ Glassmorphism card design
 * ✅ Enhanced error handling
 * 
 * 🎨 DESIGN UPDATES:
 * ========================================================================================
 * - New teal/cyan color scheme
 * - Glass morphism effects
 * - Enhanced spacing and typography
 * - Smooth gradient backgrounds
 * - Modern input fields with icons
 * ========================================================================================
 */
@Composable
fun RegistrationView(
    onBackToLoginClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.SignUpSuccess -> {
                showSuccessMessage = true
            }
            is AuthState.Error -> {
                showSuccessMessage = false
            }
            else -> {
                showSuccessMessage = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // ================================================================
            // HEADER SECTION
            // ================================================================
            RegistrationHeader()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ================================================================
            // GLASS MORPHISM REGISTRATION CARD
            // ================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GlassWhite
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    // ========================================================
                    // FULL NAME INPUT FIELD
                    // ========================================================
                    ModernTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            nameError = ""
                        },
                        label = "Full Name",
                        icon = Icons.Default.Person,
                        errorMessage = nameError
                    )
                    
                    // ========================================================
                    // EMAIL INPUT FIELD
                    // ========================================================
                    ModernTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = ""
                        },
                        label = "Email",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        errorMessage = emailError
                    )
                    
                    // ========================================================
                    // PASSWORD INPUT FIELD
                    // ========================================================
                    ModernTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = ""
                        },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        errorMessage = passwordError,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) 
                                        Icons.Default.Visibility 
                                    else 
                                        Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    )
                    
                    // ========================================================
                    // PASSWORD STRENGTH INDICATOR
                    // ========================================================
                    ModernPasswordStrengthIndicator(password)
                    
                    // ========================================================
                    // SIGN UP BUTTON
                    // ========================================================
                    Button(
                        onClick = { 
                            // Clear previous errors
                            nameError = ""
                            emailError = ""
                            passwordError = ""
                            
                            // Validate inputs
                            var isValid = true
                            
                            if (name.isBlank()) {
                                nameError = "Name is required"
                                isValid = false
                            }
                            
                            if (email.isBlank()) {
                                emailError = "Email is required"
                                isValid = false
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                emailError = "Please enter a valid email"
                                isValid = false
                            }
                            
                            if (password.isBlank()) {
                                passwordError = "Password is required"
                                isValid = false
                            } else if (password.length < 6) {
                                passwordError = "Password must be at least 6 characters"
                                isValid = false
                            }
                            
                            if (isValid) {
                                Log.d("RegistrationView", "Calling signup API for: $email")
                                viewModel.signUp(email, password, name)
                            }
                        },
                        enabled = authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.White
                        )
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // ================================================================
            // ERROR DISPLAY SECTION
            // ================================================================
            if (authState is AuthState.Error) {
                val errorState = authState as AuthState.Error
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkError.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            "Error",
                            tint = DarkError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Registration Error",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorState.message,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ================================================================
            // BACK TO LOGIN SECTION
            // ================================================================
            ModernBackToLoginSection(onBackToLoginClick)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Modern Registration Header
 */
@Composable
private fun RegistrationHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    brush = primaryGradient,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Sign Up",
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Join Music Room",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Create an account to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

/**
 * Modern Password Strength Indicator
 */
@Composable
private fun ModernPasswordStrengthIndicator(password: String) {
    val strength = when {
        password.length > 8 && password.any { it.isDigit() } && 
        password.any { it.isUpperCase() } -> 3
        password.length > 6 -> 2
        password.isNotEmpty() -> 1
        else -> 0
    }
    
    val strengthText = when (strength) {
        3 -> "Strong"
        2 -> "Medium"
        1 -> "Weak"
        else -> ""
    }
    
    val strengthColor = when (strength) {
        3 -> PrimaryTeal
        2 -> AccentCyan
        1 -> CoralPink
        else -> TextSecondary.copy(alpha = 0.2f)
    }
    
    if (password.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = if (index < strength) strengthColor 
                                       else TextSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            
            if (strengthText.isNotEmpty()) {
                Text(
                    text = "Password strength: $strengthText",
                    color = strengthColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Modern Text Field Component
 */
@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorMessage: String = "",
    isError: Boolean = errorMessage.isNotEmpty()
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { 
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isError) DarkError else PrimaryTeal
                )
            },
            trailingIcon = trailingIcon,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryTeal,
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = TextSecondary,
                errorBorderColor = DarkError,
                focusedLabelColor = PrimaryTeal,
                unfocusedLabelColor = TextSecondary,
                errorLabelColor = DarkError
            )
        )
        
        if (isError) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = DarkError
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    color = DarkError,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Modern Back to Login Section
 */
@Composable
private fun ModernBackToLoginSection(onBackToLoginClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account?",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        TextButton(onClick = onBackToLoginClick) {
            Text(
                "Log In",
                color = PrimaryTeal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

