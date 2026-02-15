package com.martin.storage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.ui.theme.AppTheme

// --- Constants ---
private const val TAG = "SettingActivity"

/**
 * An activity for displaying and managing user-configurable settings.
 * This screen provides the user with various options to customize the app's behavior,
 * manage their account, or view app-related information.
 */
class SettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge display for a more immersive UI.
        enableEdgeToEdge()
        setContent {
            AppTheme {
                var activeBottomTab by remember { mutableIntStateOf(1) }
                val context = LocalContext.current
                val bottomTabs = listOf(
                    { context.startActivity(Intent(context, MainActivity::class.java)) },
                    { context.startActivity(Intent(context, StorageActivity::class.java)) },
                    { context.startActivity(Intent(context, SettingActivity::class.java)) }
                )
                val tabIcons = listOf(R.drawable.homeicon, R.drawable.storage, R.drawable.options)

                // Scaffold provides a standard layout structure for Material Design apps.
                Scaffold(
                    bottomBar = {
                        NavigationBar(modifier = Modifier.height(60.dp)) {
                            bottomTabs.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(tabIcons[index]),
                                            contentDescription = "Bottom navigation icon"
                                        )
                                    },
                                    selected = activeBottomTab == index,
                                    onClick = {
                                        activeBottomTab = index
                                        item()
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Row(
                            modifier = Modifier
                                .padding(start = EDGEPADDING.dp, top = 10.dp)
                                .fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Settings",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(0.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSettings() {
    AppTheme {
        var activeBottomTab by remember { mutableIntStateOf(1) }
        val context = LocalContext.current
        val bottomTabs = listOf(
            { context.startActivity(Intent(context, MainActivity::class.java)) },
            { context.startActivity(Intent(context, StorageActivity::class.java)) },
            { context.startActivity(Intent(context, SettingActivity::class.java)) }
        )
        val tabIcons = listOf(R.drawable.homeicon, R.drawable.storage, R.drawable.options)

        // Scaffold provides a standard layout structure for Material Design apps.
        Scaffold(
            bottomBar = {
                NavigationBar(modifier = Modifier.height(60.dp)) {
                    bottomTabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(tabIcons[index]),
                                    contentDescription = "Bottom navigation icon"
                                )
                            },
                            selected = activeBottomTab == index,
                            onClick = {
                                activeBottomTab = index
                                item()
                            }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Row(
                    modifier = Modifier
                        .padding(start = EDGEPADDING.dp, top = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(0.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

}
