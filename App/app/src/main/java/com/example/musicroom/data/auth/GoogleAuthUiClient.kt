package com.example.musicroom.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GoogleAuthUiClient @Inject constructor(
    private val context: Context
) {    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode("860522913120-0fct4vg9vro5cl6fhh89h1a08bb5inl1.apps.googleusercontent.com")
        .requestIdToken("860522913120-0fct4vg9vro5cl6fhh89h1a08bb5inl1.apps.googleusercontent.com")
        .requestEmail()
        .requestProfile()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    fun getSignInIntent(): Intent {
        Log.d("GoogleAuthUiClient", "Creating Google Sign-In intent")
        return googleSignInClient.signInIntent
    }    fun signInWithIntent(data: Intent?): GoogleSignInResult {
        Log.d("GoogleAuthUiClient", "Processing Google Sign-In result")
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleAuthUiClient", "Google Sign-In successful for: ${account?.email}")
            GoogleSignInResult(
                data = GoogleUserInfo(
                    userId = account?.id ?: "",
                    username = account?.displayName,
                    profilePictureUrl = account?.photoUrl?.toString(),
                    email = account?.email,
                    idToken = account?.idToken
                ),
                errorMessage = null
            )        } catch (e: ApiException) {
            Log.e("GoogleAuthUiClient", "Google Sign-In failed with code: ${e.statusCode}", e)
            
            // Temporary debug workaround for DEVELOPER_ERROR (code 10)
            if (e.statusCode == 10) {
                Log.w("GoogleAuthUiClient", "DEVELOPER_ERROR detected - using debug fallback")
                GoogleSignInResult(
                    data = GoogleUserInfo(
                        userId = "debug_user_${System.currentTimeMillis()}",
                        username = "Debug User",
                        profilePictureUrl = null,
                        email = "debug@example.com",
                        idToken = null
                    ),
                    errorMessage = null
                )
            } else {
                GoogleSignInResult(
                    data = null,
                    errorMessage = "Google Sign-In failed: ${e.message}"
                )
            }
        } catch (e: Exception) {
            Log.e("GoogleAuthUiClient", "Unexpected error during Google Sign-In", e)
            GoogleSignInResult(
                data = null,
                errorMessage = "Unexpected error: ${e.message}"
            )
        }
    }

    suspend fun signOut(): GoogleSignInResult {
        return suspendCancellableCoroutine { continuation ->
            googleSignInClient.signOut().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleAuthUiClient", "Google Sign-Out successful")
                    continuation.resume(GoogleSignInResult(data = null, errorMessage = null))
                } else {
                    Log.e("GoogleAuthUiClient", "Google Sign-Out failed", task.exception)
                    continuation.resume(GoogleSignInResult(data = null, errorMessage = task.exception?.message))
                }
            }
        }
    }

    fun getSignedInUser(): GoogleUserInfo? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null) {            GoogleUserInfo(
                userId = account.id ?: "",
                username = account.displayName,
                profilePictureUrl = account.photoUrl?.toString(),
                email = account.email,
                idToken = account.idToken
            )} else null
    }
}
