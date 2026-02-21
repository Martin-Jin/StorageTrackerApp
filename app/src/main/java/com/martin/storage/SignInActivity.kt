package com.martin.storage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.martin.storage.data.UIDLOCALPATH
import com.martin.storage.data.write
import com.martin.storage.data.writeLocalData
import com.martin.storage.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// --- Constants ---
private const val TAG = "GoogleSignIn"

/**
 * This activity manages user authentication via Google Sign-In, using the Android Credential Manager API
 * for a streamlined experience. The sign-in process is initiated upon creation, and on successful
 * authentication, the user's unique ID is stored locally and remotely before navigating to `MainActivity`.
 */
class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for an immersive UI.
        enableEdgeToEdge()
        setContent {
            // The `signIn` function initiates the sign-in process as soon as the UI is composed.
            signIn(LocalContext.current, rememberCoroutineScope())

            AppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sign in",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Processes the successful response from the Credential Manager after sign-in.
 * It validates the credential, extracts the Google ID token, saves the user's UID, and navigates
 * to the main application.
 *
 * @param result The `GetCredentialResponse` object with the user's credential.
 * @param context The application context for starting a new activity and accessing DataStore.
 * @param scope A `CoroutineScope` to launch asynchronous data-saving operations.
 */
fun handleSignIn(result: GetCredentialResponse, context: Context, scope: CoroutineScope) {
    when (val credential = result.credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d(TAG, "Google ID token successfully retrieved.")

                    val uid = googleIdTokenCredential.idToken.take(20)

                    write("users/${uid}", "New user!!") { success, exception ->
                        if (success) {
                            Log.d(TAG, "Successfully wrote user data to remote database.")
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            scope.launch {
                                writeLocalData(context, UIDLOCALPATH, uid)
                            }
                        } else {
                            Log.e(TAG, "Failed to write to remote database", exception)
                        }
                    }
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Received an invalid Google ID token response", e)
                }
            } else {
                Log.e(TAG, "Unexpected type of custom credential received: ${credential.type}")
            }
        }

        else -> {
            Log.e(TAG, "Unexpected credential type received: ${credential.type}")
        }
    }
}

/**
 * Configures the options for the Google Sign-In request.
 *
 * @return A `GetGoogleIdOption` object with the configured settings.
 */
fun getGoogleIdOption(): GetGoogleIdOption {
    return GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId("104775339818-9b5ti4kua8strcmktvbe3i1s96ocf1ea.apps.googleusercontent.com")
        .setAutoSelectEnabled(true)
        .build()
}

/**
 * Builds the final request object for the Credential Manager.
 *
 * @return A `GetCredentialRequest` ready to be sent.
 */
fun getRequest(): GetCredentialRequest {
    return GetCredentialRequest.Builder()
        .addCredentialOption(getGoogleIdOption())
        .build()
}

/**
 * Initiates the Google Sign-In process by calling the Credential Manager API.
 * It launches the sign-in UI and handles the result or any exceptions.
 *
 * @param context The application context.
 * @param scope A `CoroutineScope` for calling the `getCredential` suspend function.
 */
fun signIn(context: Context, scope: CoroutineScope) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        try {
            val result = credentialManager.getCredential(
                request = getRequest(),
                context = context,
            )
            handleSignIn(result, context, scope)
        } catch (e: GetCredentialCancellationException) {
            Log.e(TAG, "Sign-in was cancelled by the user.", e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in failed with an unexpected credential error.", e)
        }
    }
}
