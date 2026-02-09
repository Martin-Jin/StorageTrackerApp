package com.martin.storage

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.martin.storage.ui.theme.TestTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance for storing data locally
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val context = LocalContext.current
                    // Centering content
                    Column(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Title(
                            string = "Storage app",
                        )
                        MenuButton(
                            { context.startActivity(Intent(context, SignInActivity::class.java)) },
                            "Sign in"
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CheckUID()
                    }
                }
            }
        }
    }

    // Check id locally so users don't have to sign in again
    @Composable
    fun CheckUID() {
        val context = LocalContext.current
        // Read the saved value from DataStore.
        // `collectAsState` converts the Flow into a State object that Compose can observe.
        val savedUID by readLocalData(
            context,
            uidLocalPath
        ).collectAsState(initial = "NOT SIGNED IN")
        if (savedUID == null) {
            val intent = Intent(context, SignInActivity::class.java)
            // Send user to sign in page
            context.startActivity(intent)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Signed in as: $savedUID", fontSize = 16.sp)
    }
}

// Write function for local data (suspend function)
suspend fun writeLocalData(context: Context, key: String, value: String) {
    val dataStoreKey = stringPreferencesKey(key)
    context.dataStore.edit { settings ->
        settings[dataStoreKey] = value
    }
}

// Read function for local data (returns a Flow)
fun readLocalData(context: Context, key: String): Flow<String?> {
    val dataStoreKey = stringPreferencesKey(key)
    return context.dataStore.data.map { preferences ->
        preferences[dataStoreKey]
    }
}

// Page preview
@Preview(showSystemUi = true)
@Composable
fun PagePreview() {
    TestTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            // Centering content
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

// Buttons and title
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

// Menu button
// Switches activity to activity passed in.
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
            onClick = { callback() }
        ) {
            Text(text = text, fontSize = 15.sp)
        }
    }

}
