package com.martin.storage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.ui.theme.TestTheme

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
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Title(
                            string = "Storage app",
                            modifier = Modifier.padding(innerPadding)
                        )
                        MenuButton(
                            { context.startActivity(Intent(context, StorageActivity::class.java)) },
                            "Storage"
                        )
                        MenuButton(
                            { context.startActivity(Intent(context, FireBase::class.java)) },
                            "Sign in"
                        )
                    }
                }
            }
        }
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
            val context = LocalContext.current
            // Centering content
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title(
                    string = "Storage app",
                    modifier = Modifier.padding(innerPadding)
                )
                MenuButton(
                    { context.startActivity(Intent(context, StorageActivity::class.java)) },
                    "Storage"
                )
                Button(onClick = {
                }) {
                    Text("Sign In with Google")
                }
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
