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
import com.martin.storage.data.STORAGEITEMPATH
import com.martin.storage.data.StashList
import com.martin.storage.data.UIDLOCALPATH
import com.martin.storage.data.readLocalData
import com.martin.storage.data.readLocalObjects
import com.martin.storage.data.stashLists
import com.martin.storage.ui.theme.AppTheme

// --- Constants ---
private const val TAG = "MainActivity"

/**
 * The main entry point of the application, responsible for checking the user's authentication status.
 * This activity verifies if a user ID (UID) is stored in DataStore and, based on that, either
 * navigates to `SignInActivity` or pre-loads inventory data and provides an entry point to the main
 * `StorageActivity`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display to allow content to draw behind system bars.
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
                        Spacer(modifier = Modifier.height(14.dp))
                        // This composable handles the core logic for this screen.
                        ReadSavedValues()
                    }
                }
            }
        }
    }
}

/**
 * A generic, reusable composable for loading and caching data from DataStore.
 * It observes a Flow of objects and invokes the `onDataLoaded` callback when data is successfully loaded.
 *
 * @param T The reified type of objects to load (e.g., `LocalRowItem`).
 * @param path The key for the data in DataStore.
 * @param onDataLoaded A callback function to execute when data is loaded.
 */
@Composable
inline fun <reified T : Any> LoadAndCache(
    path: String,
    crossinline onDataLoaded: (List<T>) -> Unit
) {
    val context = LocalContext.current
    val dataState by readLocalObjects<T>(context, path).collectAsState(initial = null)

    LaunchedEffect(dataState) {
        dataState?.let(onDataLoaded)
    }
}

/**
 * A stateful composable that manages the core logic of `MainActivity`.
 * It observes the user's UID and stored data from DataStore and uses a `LaunchedEffect` to react to
 * changes, either by navigating to the sign-in screen or by populating the in-memory cache.
 */
@Composable
fun ReadSavedValues() {
    val context = LocalContext.current

    // Observe the user's UID from DataStore. `collectAsState` converts the Flow into a
    // Compose State, causing recomposition when the data changes.
    val savedUID by readLocalData(
        context,
        UIDLOCALPATH
    ).collectAsState(initial = "NOT SIGNED IN")

    // Load and cache users lists.
    LoadAndCache<StashList>(path = STORAGEITEMPATH) { data ->
        Log.d(
            TAG,
            "Successfully read ${data.size} items from $STORAGEITEMPATH. Updating in-memory cache."
        )
        stashLists.removeAll { true }
        stashLists.addAll(data.toMutableList())
    }

    // `LaunchedEffect` performs side effects like navigation in response to state changes.
    LaunchedEffect(savedUID, stashLists[0]) {
        if (savedUID == null) {
            Log.d(TAG, "User is not signed in. Navigating to SignInActivity.")
            val intent = Intent(context, SignInActivity::class.java)
            context.startActivity(intent)
        }
    }

    // The main menu button is displayed only if the user is signed in.
    if (savedUID != null && savedUID != "NOT SIGNED IN") {
        MenuButton(
            callback = { context.startActivity(Intent(context, StashActivity::class.java)) },
            text = "Open"
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(text = "Signed in as: $savedUID", fontSize = 16.sp)
}

// --- Reusable UI Components ---
/**
 * A preview composable for visualizing the `MainActivity` UI in Android Studio's design pane.
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
 *
 * @param string The text to be displayed.
 * @param modifier The modifier to be applied to the `Text` composable.
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
 * A standardized, reusable button for primary navigation actions.
 *
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
                .size(125.dp, 45.dp),
            onClick = callback
        ) {
            Text(text = text, fontSize = 15.sp)
        }
    }
}
