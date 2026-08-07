package com.example.securekeep.auth

import androidx.fragment.app.FragmentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import android.util.Log
import android.content.Context
import androidx.core.content.edit

class GoogleSignInManager(
    private val activity: FragmentActivity
) {

    private val credentialManager = CredentialManager.create(activity)

    private val prefs =
        activity.getSharedPreferences("drive_auth", Context.MODE_PRIVATE)

    suspend fun signIn(): Result<GoogleSignInResult> {

        Log.d("DriveAuth", "Entered GoogleSignInManager.signIn()")

        return try {
            Log.d("DriveAuth", "Creating GoogleIdOption")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d("DriveAuth", "Launching CredentialManager")
            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )
            Log.d("DriveAuth", "CredentialManager returned")

            val credential = response.credential

            if (credential !is CustomCredential) {
                return Result.failure(
                    Exception("Unexpected credential type.")
                )
            }

            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(
                    Exception("Unsupported credential.")
                )
            }

            val googleCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                return Result.failure(e)
            }

            val user = GoogleUser(
                id = googleCredential.id,
                name = googleCredential.displayName ?: "",
                email = googleCredential.id,
                profilePicture = googleCredential.profilePictureUri?.toString()
            )

            prefs.edit {
                putString("email", user.email)
                putString("name", user.name)
                putString("photo", user.profilePicture)
            }

            Result.success(
                GoogleSignInResult(
                    user = user,
                    idToken = googleCredential.idToken
                )
            )

        } catch (e: Exception) {

            Log.e("DriveAuth", "Exception during sign in", e)

            Result.failure(e)
        }
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
        prefs.edit {
            clear()
        }
    }

    companion object {

        /**
         * Replace this with your Web OAuth Client ID
         * NOT the Android client ID.
         */
        private const val WEB_CLIENT_ID =
            "221152172396-3eu17di5lbv7dge1fgt5rsup0dvio8f2.apps.googleusercontent.com"

    }
    fun restoreUser(): GoogleUser? {

        val email = prefs.getString("email", null) ?: return null

        return GoogleUser(
            id = email,
            email = email,
            name = prefs.getString("name", "") ?: "",
            profilePicture = prefs.getString("photo", null)
        )
    }
}