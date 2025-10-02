package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * ========================================================================================
 * AUTHENTICATION API SERVICE - READY FOR BACKEND INTEGRATION
 * ========================================================================================
 * 
 * This service provides complete authentication functionality with real API logic.
 * Currently using MOCK DATA for testing, but ready for backend integration.
 * 
 * 🚀 BACKEND INTEGRATION STEPS:
 * ========================================================================================
 * 1. Replace "YOUR_BACKEND_URL" constant with your actual backend URL
 * 2. Uncomment the real API call methods in each authentication function
 * 3. Remove the mock delay() and mock response generation code
 * 4. Test with your backend endpoints and adjust error handling as needed
 * 
 * 📡 BACKEND ENDPOINTS EXPECTED:
 * ========================================================================================
 * POST /auth/login        - Email/Password login
 * POST /auth/signup       - User registration  
 * POST /auth/forgot-password - Password reset request
 * POST /auth/google       - Google OAuth authentication
 * 
 * 🧪 MOCK DATA TESTING:
 * ========================================================================================
 * Login Success: test@example.com / password123
 * Login Failure: fail@example.com / any password
 * SignUp Success: Any valid email/password/name
 * Google Sign-In: Always succeeds with mock data
 * Forgot Password: Always succeeds with mock response
 * ========================================================================================
 */

// ========================================================================================
// DATA CLASSES - Request/Response models for authentication
// ========================================================================================

/**
 * Login request payload
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Sign up request payload  
 */
data class SignUpRequest(
    val name: String,
    val email: String,
    val password: String,
    val avatar: String? = null
)

/**
 * Forgot password request payload
 */
data class ForgotPasswordRequest(
    val email: String
)

/**
 * Google Sign-In request payload
 */
data class GoogleSignInRequest(
    val idToken: String,
    val accessToken: String? = null
)

/**
 * Login response model matching backend API
 */
data class LoginResponse(
    val success: Boolean,
    val message: String,
    // JWT token fields from backend
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserInfo? = null,
    // Legacy fields for backward compatibility
    val token: String? = null,
    val expires_in: Long? = null
)

/**
 * Sign up response model - Updated to match backend API
 */
data class SignUpResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserInfo? = null,
    // Backend specific fields
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val avatar: String? = null
)

/**
 * Forgot password response model
 */
data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

/**
 * Google Sign-In response model
 */
data class GoogleSignInResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserInfo? = null
)

/**
 * User information model - Updated to match backend API
 */
data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val username: String? = null,
    val avatar: String? = null
)

/**
 * JWT Tokens container
 */
data class JWTTokens(
    val access: String,
    val refresh: String
)

/**
 * Logout response model
 */
data class LogoutResponse(
    val success: Boolean,
    val message: String,
    val refreshToken: String? = null
)

// ========================================================================================
// AUTHENTICATION API SERVICE CLASS
// ========================================================================================

@Singleton
class AuthApiService @Inject constructor() {
    
    /**
     * SIGNUP API CALL TO BACKEND (Codespaces compatible)
     */
    suspend fun signUp(email: String, password: String, name: String): Result<SignUpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthAPI", "📝 Attempting signup for: $email")
                Log.d("AuthAPI", "🌐 Using base URL: ${NetworkConfig.getCurrentBaseUrl()}")
                Log.d("AuthAPI", "🚀 Deployment: ${NetworkConfig.getDeploymentType()}")
                
                // Create the signup request matching your backend API
                val requestBody = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("password", password)
                }
                
                // Use NetworkConfig for URL construction
                val fullUrl = NetworkConfig.getFullUrl(NetworkConfig.Endpoints.SIGNUP)
                Log.d("AuthAPI", "📡 Full URL: $fullUrl")
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                // Handle HTTPS for Codespaces
                if (connection is HttpsURLConnection) {
                    Log.d("AuthAPI", "🔒 Using HTTPS connection")
                }
                
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")
                    
                    // Add CORS headers for Codespaces
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", "https://crispy-fishstick-v7x7p6vgj75hpxrx-8000.app.github.dev")
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                Log.d("AuthAPI", "📤 Sending request body: ${requestBody.toString()}")
                
                // Send the request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("AuthAPI", "📨 Response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("AuthAPI", "📨 Response body: $responseText")
                
                val response = when (responseCode) {
                    201 -> {
                        // Parse successful response - Updated for your backend
                        try {
                            val jsonResponse = JSONObject(responseText)
                            
                            // Your backend returns the user data directly in the response
                            SignUpResponse(
                                success = true,
                                message = jsonResponse.optString("message", "Account created successfully"),
                                token = null, // Your backend doesn't return token on signup
                                user = UserInfo(
                                    id = jsonResponse.optString("id", ""),
                                    email = jsonResponse.optString("email", email),
                                    name = jsonResponse.optString("name", name),
                                    avatar = jsonResponse.optString("avatar", "default_avatar.png")
                                ),
                                // Store backend response fields
                                id = jsonResponse.optInt("id"),
                                name = jsonResponse.optString("name"),
                                email = jsonResponse.optString("email"),
                                avatar = jsonResponse.optString("avatar")
                            )
                        } catch (e: Exception) {
                            Log.w("AuthAPI", "Could not parse success response as JSON: ${e.message}")
                            SignUpResponse(
                                success = true,
                                message = "Account created successfully",
                                token = null,
                                user = UserInfo(id = "", email = email, name = name, avatar = "default_avatar.png")
                            )
                        }
                    }
                    400 -> {
                        // Parse error response
                        val errorMessage = try {
                            val jsonResponse = JSONObject(responseText)
                            // Try different possible error message fields
                            jsonResponse.optString("message") 
                                ?: jsonResponse.optString("error")
                                ?: jsonResponse.optString("detail")
                                ?: parseValidationErrors(jsonResponse) // Handle field validation errors
                                ?: "Invalid input"
                        } catch (e: Exception) {
                            "Invalid input data"
                        }
                        
                        SignUpResponse(
                            success = false,
                            message = errorMessage
                        )
                    }
                    404 -> {
                        SignUpResponse(
                            success = false,
                            message = "API endpoint not found. Please check if backend is running."
                        )
                    }
                    500 -> {
                        SignUpResponse(
                            success = false,
                            message = "Server error. Please try again later."
                        )
                    }
                    else -> {
                        SignUpResponse(
                            success = false,
                            message = "Unexpected error occurred (Code: $responseCode)"
                        )
                    }
                }
                
                Log.d("AuthAPI", "✅ SignUp final response: success=${response.success}, message=${response.message}")
                Result.success(response)
                
            } catch (e: Exception) {
                Log.e("AuthAPI", "❌ SignUp error: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * LOGIN API CALL TO BACKEND (Codespaces compatible)
     */
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthAPI", "🔐 Attempting login for: $email")
                Log.d("AuthAPI", "🌐 Using base URL: ${NetworkConfig.getCurrentBaseUrl()}")
                
                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
                
                val fullUrl = NetworkConfig.getFullUrl(NetworkConfig.Endpoints.LOGIN)
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MusicRoom-Android-App")
                    
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                // Send the request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("AuthAPI", "Login response code: $responseCode")
                Log.d("AuthAPI", "📨 Login response body: $responseText")
                
                // Add JWT token parsing debug
                if (responseCode == 200) {
                    try {
                        val jsonResponse = JSONObject(responseText)
                        val tokensObject = jsonResponse.optJSONObject("tokens")
                        if (tokensObject != null) {
                            val accessToken = tokensObject.optString("access")
                            val refreshToken = tokensObject.optString("refresh")
                            Log.d("AuthAPI", "🎫 Access Token received: ${accessToken.take(50)}...")
                            Log.d("AuthAPI", "🔄 Refresh Token received: ${refreshToken.take(50)}...")
                        }
                    } catch (e: Exception) {
                        Log.w("AuthAPI", "Could not parse tokens for debugging: ${e.message}")
                    }
                }
                
                val response = when (responseCode) {
                    200 -> {
                        try {
                            val jsonResponse = JSONObject(responseText)
                            
                            // Parse user object
                            val userObject = jsonResponse.optJSONObject("user")
                            val user = if (userObject != null) {
                                UserInfo(
                                    id = userObject.optInt("id", 0).toString(),
                                    email = userObject.optString("email", email),
                                    name = userObject.optString("name", ""),
                                    username = userObject.optString("username"),
                                    avatar = userObject.optString("avatar", "default_avatar.png")
                                )
                            } else null
                            
                            // Parse tokens object
                            val tokensObject = jsonResponse.optJSONObject("tokens")
                            val accessToken = tokensObject?.optString("access")
                            val refreshToken = tokensObject?.optString("refresh")
                            
                            LoginResponse(
                                success = true,
                                message = jsonResponse.optString("message", "Login successful"),
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                user = user,
                                // Set legacy token field to access token for backward compatibility
                                token = accessToken,
                                expires_in = null // Your backend doesn't return this, but you could calculate it from JWT
                            )
                        } catch (e: Exception) {
                            Log.e("AuthAPI", "Error parsing login response: ${e.message}", e)
                            LoginResponse(
                                success = false,
                                message = "Failed to parse login response",
                                accessToken = null,
                                refreshToken = null,
                                user = null
                            )
                        }
                    }
                    400 -> {
                        // Parse error response
                        val errorMessage = try {
                            val jsonResponse = JSONObject(responseText)
                            jsonResponse.optString("message") 
                                ?: jsonResponse.optString("error")
                                ?: jsonResponse.optString("detail")
                                ?: "Invalid credentials"
                        } catch (e: Exception) {
                            "Invalid credentials"
                        }
                        
                        LoginResponse(
                            success = false,
                            message = errorMessage
                        )
                    }
                    401 -> {
                        LoginResponse(
                            success = false,
                            message = "Invalid email or password"
                        )
                    }
                    404 -> {
                        LoginResponse(
                            success = false,
                            message = "User not found"
                        )
                    }
                    500 -> {
                        LoginResponse(
                            success = false,
                            message = "Server error. Please try again later."
                        )
                    }
                    else -> {
                        LoginResponse(
                            success = false,
                            message = "Unexpected error occurred (Code: $responseCode)"
                        )
                    }
                }
                
                Result.success(response)
                
            } catch (e: Exception) {
                Log.e("AuthAPI", "❌ Login error: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * ========================================================================
     * FORGOT PASSWORD API
     * ========================================================================
     * 
     * Sends password reset email to user.
     * ========================================================================
     */
    suspend fun forgotPassword(email: String): Result<ForgotPasswordResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthAPI", "🔑 Sending password reset for: $email")
                
                // Mock data for testing - replace with real API call
                // Mock success response
                val response = ForgotPasswordResponse(
                    success = true,
                    message = "Password reset email sent to $email"
                )
                
                Log.d("AuthAPI", "✅ Password reset response: ${response.success}")
                Result.success(response)
                
            } catch (e: Exception) {
                Log.e("AuthAPI", "❌ Password reset error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * ========================================================================
     * GOOGLE SIGN-IN API
     * ========================================================================
     * 
     * Authenticates user with Google OAuth credentials.
     * ========================================================================
     */
    suspend fun signInWithGoogle(idToken: String, accessToken: String? = null): Result<GoogleSignInResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthAPI", "🔗 Attempting Google Sign-In")
                
                // Mock data for testing - replace with real API call
                // Mock success response
                val response = GoogleSignInResponse(
                    success = true,
                    message = "Google Sign-In successful",
                    token = "mock_jwt_token_${System.currentTimeMillis()}",
                    user = UserInfo(
                        id = "google_user_${idToken.hashCode()}",
                        email = "google.user@example.com",
                        name = "Google User"
                    )
                )
                
                Log.d("AuthAPI", "✅ Google Sign-In response: ${response.success}")
                Result.success(response)
                
            } catch (e: Exception) {
                Log.e("AuthAPI", "❌ Google Sign-In error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Logout user by blacklisting refresh token
     */
    suspend fun logout(refreshToken: String): Result<LogoutResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthAPI", "🚪 Starting user logout")
                
                val url = "${NetworkConfig.BASE_URL}/api/users/logout/"
                val requestBody = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }.toString()
                
                Log.d("AuthAPI", "📤 Logout request body: $requestBody")
                
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    
                    if (NetworkConfig.isCodespaces()) {
                        setRequestProperty("Origin", NetworkConfig.getCurrentBaseUrl())
                    }
                    
                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }
                
                // Write request body
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("AuthAPI", "📨 Logout response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("AuthAPI", "📨 Logout response: $responseText")
                
                when (responseCode) {
                    201, 200 -> {
                        try {
                            val jsonResponse = JSONObject(responseText)
                            val response = LogoutResponse(
                                success = true,
                                message = jsonResponse.optString("message", "Successfully logged out"),
                                refreshToken = jsonResponse.optString("refresh_token", refreshToken)
                            )
                            
                            Log.d("AuthAPI", "✅ Logout successful")
                            Result.success(response)
                        } catch (e: Exception) {
                            // If JSON parsing fails but status is success, still treat as success
                            Log.d("AuthAPI", "✅ Logout successful (simple response)")
                            val response = LogoutResponse(
                                success = true,
                                message = "Successfully logged out",
                                refreshToken = refreshToken
                            )
                            Result.success(response)
                        }
                    }
                    400 -> {
                        Log.e("AuthAPI", "❌ Bad request: $responseText")
                        Result.failure(Exception("Invalid refresh token"))
                    }
                    401 -> {
                        Log.e("AuthAPI", "❌ Unauthorized: $responseText")
                        Result.failure(Exception("Authentication failed"))
                    }
                    else -> {
                        Log.e("AuthAPI", "❌ Logout failed: $responseCode - $responseText")
                        Result.failure(Exception("Logout failed: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("AuthAPI", "❌ Network error during logout", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
    
    /**
     * Helper method to store JWT tokens securely
     * Call this after successful login to store tokens
     */
    fun storeTokens(accessToken: String?, refreshToken: String?) {
        // TODO: Implement secure token storage using EncryptedSharedPreferences
        // This is where you would store the tokens securely for future API calls
        Log.d("AuthAPI", "📱 Storing tokens - Access: ${accessToken?.take(20)}..., Refresh: ${refreshToken?.take(20)}...")
    }
    
    /**
     * Helper method to get stored access token
     */
    fun getStoredAccessToken(): String? {
        // TODO: Implement token retrieval from secure storage
        return null
    }
    
    /**
     * Helper method to get stored refresh token
     */
    fun getStoredRefreshToken(): String? {
        // TODO: Implement token retrieval from secure storage
        return null
    }
    
    /**
     * Parse validation errors from backend response
     */
    private fun parseValidationErrors(jsonResponse: JSONObject): String? {
        return try {
            val errors = mutableListOf<String>()
            
            // Check for field-specific errors
            if (jsonResponse.has("name")) {
                errors.add("Name: ${jsonResponse.optString("name")}")
            }
            if (jsonResponse.has("email")) {
                errors.add("Email: ${jsonResponse.optString("email")}")
            }
            if (jsonResponse.has("password")) {
                errors.add("Password: ${jsonResponse.optString("password")}")
            }
            
            if (errors.isNotEmpty()) {
                errors.joinToString(", ")
            } else null
        } catch (e: Exception) {
            null
        }
    }
}