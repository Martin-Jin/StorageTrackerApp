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
import com.martin.storage.ui.theme.TestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "GoogleSignIn"
const val uidLocalPath = "UID"

// Sign in page
class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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

    // Google sign in code
    fun handleSignIn(result: GetCredentialResponse, context: Context, scope: CoroutineScope) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        Log.d(TAG, "Got Google ID token: ${googleIdTokenCredential.idToken}")

                        // What happens after the user signs in
                        val uid = googleIdTokenCredential.idToken.take(20)
                        write(
                            "users/${uid}",
                            "New user!!"
                        ) { success, exception ->
                            if (success) {
                                Log.d(TAG, "Successfully wrote to database")
                                val intent = Intent(context, StorageActivity::class.java)
                                // Send user to new page and save uid
                                context.startActivity(intent)
                                scope.launch {
                                    writeLocalData(context, uidLocalPath, uid)
                                }

                            } else {
                                Log.e(TAG, "Failed to write to database", exception)
                            }
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected type of custom credential: ${credential.type}")
                }
            }

            else -> {
                Log.e(TAG, "Unexpected type of credential: ${credential.type}")
            }
        }
    }

    fun getGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("104775339818-9b5ti4kua8strcmktvbe3i1s96ocf1ea.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .build()
    }

    fun getRequest(): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(getGoogleIdOption())
            .build()
    }

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
                Log.e(
                    TAG,
                    "Sign-in was cancelled by the user. If you did not cancel, check your Google Cloud project configuration.",
                    e
                )
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Sign-in failed with an unexpected error.", e)
            }
        }
    }
}
