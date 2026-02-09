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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database
import com.martin.storage.ui.theme.TestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "FireBase"
private lateinit var auth: FirebaseAuth

class FireBase : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    signIn(context, scope)
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

    // General write function
    fun write(path: String, data: Any?, callback: (Boolean, Exception?) -> Unit) {
        val database = Firebase.database
        val myRef = database.getReference(path)
        myRef.setValue(data)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { exception ->
                callback(false, exception)
            }
    }

    // General read function
    fun read(path: String, callback: (DataSnapshot?, Exception?) -> Unit) {
        val database = Firebase.database
        database.getReference(path).get()
            .addOnSuccessListener { dataSnapshot ->
                callback(dataSnapshot, null)
            }
            .addOnFailureListener { exception ->
                callback(null, exception)
            }
    }

    //    // Firebase auth code
//    private fun firebaseAuthWithGoogle(idToken: String) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
//            if (task.isSuccessful) {
//                // Sign in success, update UI with the signed-in user's information
//                Log.d(TAG, "signInWithCredential:success")
//                val user = auth.currentUser
//                // Function here after sign in
//            } else {
//                // If sign in fails, display a message to the user
//                Log.w(TAG, "signInWithCredential:failure", task.exception)
//                // Function here after sign in failed
//            }
//        }
//    }

    fun handleSignIn(result: GetCredentialResponse, context: Context) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Got Google ID token: ${googleIdTokenCredential.idToken}")

                        // Example of using the write function
                        write(
                            "users/${googleIdTokenCredential.idToken.take(20)}",
                            "New user!!"
                        ) { success, exception ->
                            if (success) {
                                Log.d(TAG, "Successfully wrote to database")
                                val intent = Intent(context, StorageActivity::class.java)
                                context.startActivity(intent)
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
                handleSignIn(result, context)
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
