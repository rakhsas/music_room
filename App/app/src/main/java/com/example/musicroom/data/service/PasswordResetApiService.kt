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

/**
 * API Service for password reset operations
 */
class PasswordResetApiService {
    
    /**
     * Step 1: Request password reset OTP
     */
    suspend fun requestPasswordResetOTP(email: String): Result<PasswordResetResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("PasswordResetAPI", "📧 Requesting password reset OTP for: $email")
                
                val requestBody = JSONObject().apply {
                    put("email", email)
                }.toString()
                
                val url = URL("${NetworkConfig.BASE_URL}/api/users/password-reset/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
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
                Log.d("PasswordResetAPI", "📨 Request OTP response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("PasswordResetAPI", "📨 Request OTP response: $responseText")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val json = JSONObject(responseText)
                            val response = PasswordResetResponse(
                                success = true,
                                message = json.optString("message", "OTP sent successfully")
                            )
                            Result.success(response)
                        } catch (e: Exception) {
                            Log.e("PasswordResetAPI", "❌ Error parsing OTP request response", e)
                            Result.failure(Exception("Failed to parse response"))
                        }
                    }
                    400 -> {
                        val errorMessage = try {
                            val json = JSONObject(responseText)
                            json.optString("message") 
                                ?: json.optString("error")
                                ?: json.optString("detail")
                                ?: "Invalid email or user not found"
                        } catch (e: Exception) {
                            "Invalid email or user not found"
                        }
                        Result.success(PasswordResetResponse(false, errorMessage))
                    }
                    else -> {
                        Result.failure(Exception("Failed to request OTP: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("PasswordResetAPI", "❌ Network error requesting OTP", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Step 2: Verify password reset OTP
     */
    suspend fun verifyPasswordResetOTP(email: String, otp: String): Result<OTPVerificationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("PasswordResetAPI", "🔢 Verifying OTP for: $email")
                
                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("otp", otp)
                }.toString()
                
                val url = URL("${NetworkConfig.BASE_URL}/api/users/password-reset-verify-otp/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
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
                Log.d("PasswordResetAPI", "📨 Verify OTP response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("PasswordResetAPI", "📨 Verify OTP response: $responseText")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val json = JSONObject(responseText)
                            val response = OTPVerificationResponse(
                                success = true,
                                message = json.optString("message", "OTP verified successfully")
                            )
                            Result.success(response)
                        } catch (e: Exception) {
                            Log.e("PasswordResetAPI", "❌ Error parsing OTP verification response", e)
                            Result.failure(Exception("Failed to parse response"))
                        }
                    }
                    400 -> {
                        val errorMessage = try {
                            val json = JSONObject(responseText)
                            json.optString("message") 
                                ?: json.optString("error")
                                ?: json.optString("detail")
                                ?: "Invalid or expired OTP"
                        } catch (e: Exception) {
                            "Invalid or expired OTP"
                        }
                        Result.success(OTPVerificationResponse(false, errorMessage))
                    }
                    else -> {
                        Result.failure(Exception("Failed to verify OTP: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("PasswordResetAPI", "❌ Network error verifying OTP", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Step 3: Reset password with verified OTP
     */
    suspend fun resetPasswordWithOTP(
        email: String, 
        otp: String, 
        password: String, 
        passwordConfirm: String
    ): Result<PasswordResetConfirmResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("PasswordResetAPI", "🔐 Resetting password for: $email")
                
                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("otp", otp)
                    put("password", password)
                    put("password_confirm", passwordConfirm)
                }.toString()
                
                val url = URL("${NetworkConfig.BASE_URL}/api/users/password-reset-confirm/")
                val connection = (url.openConnection() as HttpURLConnection).apply {
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
                Log.d("PasswordResetAPI", "📨 Reset password response code: $responseCode")
                
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                Log.d("PasswordResetAPI", "📨 Reset password response: $responseText")
                
                when (responseCode) {
                    200 -> {
                        try {
                            val json = JSONObject(responseText)
                            val response = PasswordResetConfirmResponse(
                                success = true,
                                message = json.optString("message", "Password reset successful")
                            )
                            Result.success(response)
                        } catch (e: Exception) {
                            Log.e("PasswordResetAPI", "❌ Error parsing password reset response", e)
                            Result.failure(Exception("Failed to parse response"))
                        }
                    }
                    400 -> {
                        val errorMessage = try {
                            val json = JSONObject(responseText)
                            json.optString("message") 
                                ?: json.optString("error")
                                ?: json.optString("detail")
                                ?: "Invalid OTP, passwords don't match, or other validation error"
                        } catch (e: Exception) {
                            "Invalid OTP, passwords don't match, or other validation error"
                        }
                        Result.success(PasswordResetConfirmResponse(false, errorMessage))
                    }
                    else -> {
                        Result.failure(Exception("Failed to reset password: HTTP $responseCode"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("PasswordResetAPI", "❌ Network error resetting password", e)
                Result.failure(e)
            }
        }
    }
}

/**
 * Data classes for password reset API responses
 */
data class PasswordResetResponse(
    val success: Boolean,
    val message: String
)

data class OTPVerificationResponse(
    val success: Boolean,
    val message: String
)

data class PasswordResetConfirmResponse(
    val success: Boolean,
    val message: String
)

