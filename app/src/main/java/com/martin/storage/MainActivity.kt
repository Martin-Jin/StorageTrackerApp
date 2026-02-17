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
import com.martin.storage.data.STORAGEITEMPATH
import com.martin.storage.data.TABITEMSPATH
import com.martin.storage.data.TabItem
import com.martin.storage.data.UIDLOCALPATH
import com.martin.storage.data.readLocalData
import com.martin.storage.data.readLocalObjects
import com.martin.storage.data.storageItems
import com.martin.storage.data.tabItems
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
                        Spacer(modifier = Modifier.height(14.dp))
                        // The ReadSavedValues composable encapsulates the core logic for this screen.
                        ReadSavedValues()
                    }
                }
            }
        }
    }
}

/**
 * A generic, reusable composable to load data from DataStore and cache it.
 * It observes a Flow of objects, and whenever the data is successfully loaded,
 * it calls the `onDataLoaded` lambda with the result.
 *
 * @param T The reified type of objects to load (e.g., LocalRowItem).
 * @param path The key for the data in DataStore.
 * @param onDataLoaded A callback function to execute when the data is loaded.
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
 * A stateful composable that handles the core logic of the MainActivity. It observes the user's
 * UID and their stored data from DataStore. It then uses a `LaunchedEffect` to react to changes
 * in this data, either by navigating to the sign-in screen or by populating an in-memory cache
 * with the user's items.
 */
@Composable
fun ReadSavedValues() {
    val context = LocalContext.current

    // --- State Observation ---

    // Observe the user's UID from DataStore. `collectAsState` converts the Flow into a
    // Compose State object, causing recomposition when the data changes.
    // The initial value "NOT SIGNED IN" is a sentinel to manage the initial loading state.
    val savedUID by readLocalData(
        context,
        UIDLOCALPATH
    ).collectAsState(initial = "NOT SIGNED IN")

    // --- Data Loading ---
    // By explicitly providing the type parameter <LocalRowItem>, we tell LoadAndCache exactly
    // what to deserialize. This avoids the type erasure crash.
    LoadAndCache<LocalRowItem>(path = STORAGEITEMPATH) { data ->
        Log.d(
            TAG,
            "Successfully read ${data.size} items from $STORAGEITEMPATH. Updating in-memory cache."
        )
        // Re-assign the global variable to update the in-memory cache.
        storageItems = data.toMutableList()
    }

    // A separate, explicit call is needed for each different type of data to be loaded.
    LoadAndCache<TabItem>(path = TABITEMSPATH) { data ->
        Log.d(
            TAG,
            "Successfully read ${data.size} tabs from $TABITEMSPATH. Updating in-memory cache."
        )
        tabItems = data.toMutableList()
    }

    // --- Side Effects ---

    // `LaunchedEffect` is crucial for performing actions like navigation in response to state changes.
    // This effect will re-launch whenever `savedUID` changes.
    LaunchedEffect(savedUID, tabItems) {
        // If savedUID is null (after the initial read), it means the user has never signed in.
        if (savedUID == null) {
            Log.d(TAG, "User is not signed in. Navigating to SignInActivity.")
            val intent = Intent(context, SignInActivity::class.java)
            context.startActivity(intent)
        }
        if (tabItems.isEmpty()) {
            tabItems = mutableListOf(
                TabItem("Fridge", 0),
                TabItem("Cabinet", 1), TabItem("Other", 2)
            )
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

    Spacer(modifier = Modifier.height(20.dp))
    Text(text = "Signed in as: $savedUID", fontSize = 16.sp)
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
                .size(125.dp, 45.dp),
            onClick = callback // The onClick action is passed in from the calling site.
        ) {
            Text(text = text, fontSize = 15.sp)
        }
    }
}
