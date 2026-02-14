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
import com.martin.storage.ui.theme.AppTheme

// --- Constants ---
private const val TAG = "MainActivity"

/**
 * The main entry point of the application, acting as a gatekeeper. This activity's primary
 * responsibility is to verify the user's authentication status by checking for a stored UID in
 * DataStore. Based on this, it either navigates the user to the `SignInActivity` or pre-loads
 * their inventory data and presents them with an option to enter the main `StorageActivity`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display to allow the app to draw behind system bars.
        enableEdgeToEdge()
        setContent {
            AppTheme {
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
                            string = "Stash tracker",
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // The CheckUID composable encapsulates the core logic of this screen.
                        CheckUID()
                    }
                }
            }
        }
    }

    /**
     * A stateful composable that handles the core logic of the MainActivity. It observes the user's
     * UID and their stored data from DataStore. It then uses a `LaunchedEffect` to react to changes
     * in this data, either by navigating to the sign-in screen or by populating an in-memory cache
     * with the user's items.
     */
    @Composable
    fun CheckUID() {
        val context = LocalContext.current

        // --- State Observation ---

        // Observe the user's UID from DataStore. `collectAsState` converts the Flow into a
        // Compose State object, causing recomposition when the data changes.
        // The initial value "NOT SIGNED IN" is a sentinel to manage the initial loading state.
        val savedUID by readLocalData(
            context,
            uidLocalPath
        ).collectAsState(initial = "NOT SIGNED IN")

        // Observe the list of stored items. The type `<LocalRowItem>` is explicitly provided
        // because `readLocalObjects` is a generic function. The initial value is `null` to represent
        // the state where data has not yet been loaded.
        val savedData by readLocalObjects<LocalRowItem>(context, storageItemPath).collectAsState(initial = null)

        // --- Side Effects ---

        // `LaunchedEffect` is crucial for performing actions like navigation or data caching in response
        // to state changes, without blocking the UI thread. This effect will re-launch whenever
        // `savedUID` or `savedData` changes.
        LaunchedEffect(savedUID, savedData) {
            // If savedUID is null (after the initial read), it means the user has never signed in.
            if (savedUID == null) {
                Log.d(TAG, "User is not signed in. Navigating to SignInActivity.")
                val intent = Intent(context, SignInActivity::class.java)
                context.startActivity(intent)
            }

            // When `savedData` is successfully loaded (is not null), it updates the global
            // `storageItems` in-memory cache. This makes the data immediately available to other parts
            // of the app, like StorageActivity, without needing to read from disk again.
            savedData?.let { data ->
                Log.d(TAG, "Successfully read ${data.size} items from local storage. Updating in-memory cache.")
                storageItems = data.toMutableList()
            }
        }

        // --- UI Rendering ---

        // The main menu button is only displayed if the user is confirmed to be signed in.
        // The check prevents the button from appearing during the initial loading state.
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
 * A preview composable for visualizing the MainActivity UI in Android Studio's design pane.
 * This helps in rapidly iterating on the UI without needing to run the full app on a device.
 */
@Preview(showSystemUi = true)
@Composable
fun PagePreview() {
    AppTheme {
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
 * A simple, reusable composable for displaying a large, centered title.
 * @param string The text to be displayed.
 * @param modifier The modifier to be applied to the Text composable.
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
 * A standardized, reusable button composable for primary navigation actions.
 * @param callback The lambda function to be invoked when the button is clicked.
 * @param text The string to be displayed on the button.
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
                .size(125.dp, 50.dp),
            onClick = callback // The onClick action is passed in from the calling site.
        ) {
            Text(text = text, fontSize = 15.sp)
        }
    }
}
