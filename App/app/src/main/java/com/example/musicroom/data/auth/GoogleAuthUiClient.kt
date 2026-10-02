package com.example.musicroom.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.musicroom.R
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
) {
    // Must be the "Web application" OAuth client id from google-services.json (client_type 3),
    // auto-generated as R.string.default_web_client_id by the google-services Gradle plugin -
    // NOT the Android client id, and never hardcoded here (a mismatch is the #1 cause of
    // GoogleSignInStatusCodes.DEVELOPER_ERROR / code 10).
    private val webClientId = context.getString(R.string.default_web_client_id)

    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode(webClientId)
        .requestIdToken(webClientId)
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

            val message = if (e.statusCode == 10) {
                // DEVELOPER_ERROR: the app's SHA-1 signing certificate isn't registered for the
                // Android OAuth client in this Firebase/Google Cloud project (google-services.json),
                // or the package name doesn't match.
                "Google Sign-In is misconfigured (DEVELOPER_ERROR). Check that this app's SHA-1 " +
                    "fingerprint is registered for the Android OAuth client in the Google Cloud project."
            } else {
                "Google Sign-In failed: ${e.message}"
            }
            GoogleSignInResult(data = null, errorMessage = message)
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
