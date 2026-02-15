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
 * This activity manages the user authentication flow using Google Sign-In.
 * It leverages the Android Credential Manager API for a streamlined and modern sign-in experience.
 * The process is initiated as soon as the activity is created, and upon successful authentication,
 * the user's unique ID is stored both locally and remotely, before navigating to the MainActivity.
 */
class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for a modern, immersive UI.
        enableEdgeToEdge()
        setContent {
            // The `signIn` function is a non-composable function that kicks off the entire sign-in process.
            // It's called here to trigger the Google Sign-In prompt as soon as the UI is composed.
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
 * Processes the successful response from the Credential Manager after the user has signed in.
 * It validates the credential, extracts the Google ID token, saves the user's UID to remote and
 * local databases, and navigates to the main part of the application.
 *
 * @param result The `GetCredentialResponse` object containing the user's credential information.
 * @param context The application context, used for starting a new activity and accessing DataStore.
 * @param scope A CoroutineScope to launch asynchronous data-saving operations.
 */
fun handleSignIn(result: GetCredentialResponse, context: Context, scope: CoroutineScope) {
    when (val credential = result.credential) {
        // This block handles a successful sign-in with a Google ID token.
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    // Attempt to create a verifiable GoogleIdTokenCredential from the response data.
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d(TAG, "Google ID token successfully retrieved.")

                    // --- Post-Sign-In Logic ---
                    // Use the first 20 characters of the token as a stable, unique user ID.
                    val uid = googleIdTokenCredential.idToken.take(20)

                    // Write a welcome message to the remote Firebase database under the user's UID.
                    write("users/${uid}", "New user!!") { success, exception ->
                        if (success) {
                            Log.d(TAG, "Successfully wrote user data to remote database.")
                            val intent = Intent(context, MainActivity::class.java)
                            // On successful remote write, navigate the user to the main activity.
                            context.startActivity(intent)
                            // Asynchronously save the user's UID to the local DataStore for session persistence.
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

        // This block handles any other unexpected credential types.
        else -> {
            Log.e(TAG, "Unexpected credential type received: ${credential.type}")
        }
    }
}

/**
 * Configures the specific options for the Google Sign-In request.
 * This tells the Credential Manager how to handle the Google Sign-In prompt.
 *
 * @return A `GetGoogleIdOption` object containing the configured settings.
 */
fun getGoogleIdOption(): GetGoogleIdOption {
    return GetGoogleIdOption.Builder()
        // `setFilterByAuthorizedAccounts(false)` ensures that all Google accounts on the device are available to the user in the account selection prompt.
        .setFilterByAuthorizedAccounts(false)
        // This is the server client ID from your Google Cloud project, which is essential for authenticating your backend with Google.
        .setServerClientId("104775339818-9b5ti4kua8strcmktvbe3i1s96ocf1ea.apps.googleusercontent.com")
        // `setAutoSelectEnabled(true)` allows for a faster, one-tap sign-in experience if the user has previously used this app with a single Google account.
        .setAutoSelectEnabled(true)
        .build()
}

/**
 * Builds the final request object for the Credential Manager, bundling all authentication methods.
 *
 * @return A `GetCredentialRequest` that includes the configured Google ID option, ready to be sent.
 */
fun getRequest(): GetCredentialRequest {
    return GetCredentialRequest.Builder()
        .addCredentialOption(getGoogleIdOption())
        .build()
}

/**
 * The core function that initiates the Google Sign-In process by calling the Credential Manager API.
 * It launches the sign-in UI and handles the result or any exceptions that occur.
 *
 * @param context The application context.
 * @param scope A CoroutineScope is required to call the `getCredential` suspend function.
 */
fun signIn(context: Context, scope: CoroutineScope) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        try {
            // Launch the Credential Manager UI. This is a suspend function that waits for the user's action.
            val result = credentialManager.getCredential(
                request = getRequest(),
                context = context,
            )
            // On successful credential acquisition, pass the result to the handler function.
            handleSignIn(result, context, scope)
        } catch (e: GetCredentialCancellationException) {
            // This catch block handles the case where the user explicitly cancels the sign-in flow (e.g., by pressing the back button).
            Log.e(TAG, "Sign-in was cancelled by the user.", e)
        } catch (e: GetCredentialException) {
            // This is a general catch block for other potential errors during the sign-in process, such as network issues or misconfigurations.
            Log.e(TAG, "Sign-in failed with an unexpected credential error.", e)
        }
    }
}
