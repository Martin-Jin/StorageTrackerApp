package com.martin.storage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.data.LocalRowItem
import com.martin.storage.data.readLocalData
import com.martin.storage.data.readLocalObjects
import com.martin.storage.data.storageItemPath
import com.martin.storage.data.storageItems
import com.martin.storage.data.uidLocalPath
import com.martin.storage.ui.theme.TestTheme

// --- Constants ---
private const val TAG = "MainActivity"

/**
 * The main entry point of the application.
 * This activity serves as the initial screen, responsible for checking the user's sign-in status
 * and navigating them to the appropriate screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge display for a more immersive UI.
        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Main layout column, centered both vertically and horizontally.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Title(
                            string = "Storage app",
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // The CheckUID composable handles the core logic for this screen.
                        CheckUID()
                    }
                }
            }
        }
    }

    /**
     * A composable function that checks the user's sign-in status and local data.
     * It observes DataStore and decides whether to show the main menu or navigate to the sign-in screen.
     */
    @Composable
    fun CheckUID() {
        val context = LocalContext.current

        // --- State Observation ---

        // Observe the user's UID from DataStore.
        // `collectAsState` transforms the Flow from DataStore into a Compose State.
        // The composable will automatically recompose whenever the UID changes.
        // "NOT SIGNED IN" is a temporary initial value before the first value is read from disk.
        val savedUID by readLocalData(
            context,
            uidLocalPath
        ).collectAsState(initial = "NOT SIGNED IN")

        // Observe the user's stored items from DataStore.
        // We explicitly provide the type <LocalRowItem> because readLocalObjects is a generic function.
        // The initial value is `null` because the data might not exist yet when the app starts.
        val savedData by readLocalObjects<LocalRowItem>(context, storageItemPath).collectAsState(initial = null)

        // --- Side Effects ---

        // `LaunchedEffect` is used to perform side effects (like navigation or logging) in response to state changes.
        // This block will run once when the composable enters the composition, a   nd again anytime `savedUID` or `savedData` changes.
        LaunchedEffect(savedUID, savedData) {
            // If savedUID is null, it means the user has never signed in.
            if (savedUID == null) {
                Log.d(TAG, "User is not signed in. Navigating to SignInActivity.")
                val intent = Intent(context, SignInActivity::class.java)
                context.startActivity(intent)
            }

            // If savedData is not null, update the global in-memory cache.
            // This is a side effect and must be done inside a LaunchedEffect.
            savedData?.let { data ->
                Log.d(TAG, "Successfully read ${data.size} items from local storage. Updating in-memory cache.")
                storageItems = data.toMutableList()
            }
        }


        // --- UI Rendering ---

        // Only show the "Open" button if the user is properly signed in.
        // We check against "NOT SIGNED IN" to avoid showing the button during the brief initial loading state.
        if (savedUID != null && savedUID != "NOT SIGNED IN") {
            MenuButton(
                callback = { context.startActivity(Intent(context, StorageActivity::class.java)) },
                text = "Open"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Signed in as: $savedUID", fontSize = 16.sp)
    }
}

// --- Reusable UI Components ---

/**
 * A preview composable for visualizing the MainActivity UI in Android Studio.
 */
@Preview(showSystemUi = true)
@Composable
fun PagePreview() {
    TestTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title(
                    string = "Storage app",
                )
                MenuButton(
                    { },
                    "Open"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * A simple composable for displaying a large, centered title.
 * @param string The text to display in the title.
 */
@Composable
fun Title(string: String, modifier: Modifier = Modifier) {
    Text(
        text = string,
        fontSize = 50.sp,
        lineHeight = 50.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(bottom = 24.dp),
    )
}

/**
 * A standardized button for navigating between activities.
 * @param callback The lambda function to execute when the button is clicked.
 * @param text The text to display on the button.
 */
@Composable
fun MenuButton(callback: () -> Unit, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier
                .size(175.dp, 50.dp),
            onClick = callback // Directly use the passed-in callback.
        ) {
            Text(text = text, fontSize = 15.sp)
        }
    }
}
