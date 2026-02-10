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
import com.martin.storage.data.uidLocalPath
import com.martin.storage.data.write
import com.martin.storage.data.writeLocalData
import com.martin.storage.ui.theme.TestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "GoogleSignIn"

/**
 * This activity handles the entire user sign-in process using Google Sign-In.
 * It presents a simple UI and immediately triggers the sign-in flow.
 */
class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The `signIn` function is a regular (non-composable) function that initiates the sign-in process.
            // It's called here to trigger the sign-in flow as soon as the activity is displayed.
            signIn(LocalContext.current, rememberCoroutineScope())

            TestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Title(
                            string = "Sign in",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Handles the response from the Credential Manager after the user has interacted with the sign-in prompt.
 * @param result The `GetCredentialResponse` containing the user's credential.
 * @param context The application context for navigation and DataStore access.
 * @param scope The CoroutineScope for launching background tasks.
 */
fun handleSignIn(result: GetCredentialResponse, context: Context, scope: CoroutineScope) {
    when (val credential = result.credential) {
        // This case handles a successful sign-in with a Google ID token.
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    // Attempt to create a GoogleIdTokenCredential from the response data.
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d(TAG, "Got Google ID token.")

                    // --- Post-Sign-In Logic ---
                    // Take the first 20 chars of the token as a unique user ID.
                    val uid = googleIdTokenCredential.idToken.take(20)

                    // Write a welcome message to the remote database.
                    write("users/${uid}", "New user!!") { success, exception ->
                        if (success) {
                            Log.d(TAG, "Successfully wrote to remote database.")
                            val intent = Intent(context, MainActivity::class.java)
                            // Navigate the user to the main part of the app.
                            context.startActivity(intent)
                            // Save the user's UID to local DataStore for future sessions.
                            scope.launch {
                                writeLocalData(context, uidLocalPath, uid)
                            }
                        } else {
                            Log.e(TAG, "Failed to write to remote database", exception)
                        }
                    }
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Received an invalid google id token response", e)
                }
            } else {
                Log.e(TAG, "Unexpected type of custom credential: ${credential.type}")
            }
        }

        // This case handles any other unexpected credential types.
        else -> {
            Log.e(TAG, "Unexpected credential type: ${credential.type}")
        }
    }
}

/**
 * Configures the options specifically for the Google Sign-In request.
 * @return A `GetGoogleIdOption` object with the required settings.
 */
fun getGoogleIdOption(): GetGoogleIdOption {
    return GetGoogleIdOption.Builder()
        // `setFilterByAuthorizedAccounts(false)` ensures that all Google accounts on the device are shown in the prompt.
        .setFilterByAuthorizedAccounts(false)
        // The server client ID from your Google Cloud project, required for authenticating with your backend.
        .setServerClientId("104775339818-9b5ti4kua8strcmktvbe3i1s96ocf1ea.apps.googleusercontent.com")
        // `setAutoSelectEnabled(true)` allows for a streamlined sign-in flow if there's only one valid account.
        .setAutoSelectEnabled(true)
        .build()
}

/**
 * Builds the overall request object for the Credential Manager.
 * @return A `GetCredentialRequest` that includes the configured Google ID option.
 */
fun getRequest(): GetCredentialRequest {
    return GetCredentialRequest.Builder()
        .addCredentialOption(getGoogleIdOption())
        .build()
}

/**
 * Initiates the Google Sign-In process by calling the Credential Manager.
 * This function should be called from a Composable context to get the scope and context.
 * @param context The application context.
 * @param scope A CoroutineScope to launch the suspend function `getCredential`.
 */
fun signIn(context: Context, scope: CoroutineScope) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        try {
            // Launch the credential manager prompt.
            val result = credentialManager.getCredential(
                request = getRequest(),
                context = context,
            )
            // Handle the successful result.
            handleSignIn(result, context, scope)
        } catch (e: GetCredentialCancellationException) {
            // This block is executed if the user manually cancels the sign-in prompt.
            Log.e(TAG, "Sign-in was cancelled by the user.", e)
        } catch (e: GetCredentialException) {
            // This block catches other, more general errors from the Credential Manager.
            Log.e(TAG, "Sign-in failed with an unexpected error.", e)
        }
    }
}
