package com.martin.storage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.data.stashLists
import com.martin.storage.ui.theme.AppTheme
import com.martin.storage.ui.theme.BottomNavigation
import com.martin.storage.ui.theme.TopTitle

// --- Constants ---
private const val TAG = "SettingActivity"

// --- Data Models for Settings ---

/**
 * A sealed class representing a setting item.
 * Using a sealed class allows for a restricted set of types that can be a `SettingItem`.
 */
sealed class SettingItem

/**
 * A data class representing a button in the settings screen.
 *
 * @param text The text to display on the button.
 * @param icon The drawable resource ID for the icon.
 * @param onClick The lambda to execute when the button is clicked.
 */
data class ButtonItem(val text: String, val icon: Int = 0, val onClick: () -> Unit) : SettingItem()

/**
 * A singleton object representing a divider in the settings list.
 */
object DividerItem : SettingItem()

/**
 * A data class representing a subtitle in the settings screen.
 *
 * @param text The text to display as a subtitle.
 */
data class SubtitleItem(val text: String) : SettingItem()

/**
 * A data class representing a list item in the settings screen.
 *
 * @param text The text to display on the button.
 */
data class ListItem(val text: String) : SettingItem()

var settingsItems = listOf<SettingItem>()

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
            val context = LocalContext.current
            val intent = Intent(context, EmptyActivity::class.java)

            settingsItems = remember {
                listOf(
                    ButtonItem("Account", R.drawable.account_circle) {
                        context.startActivity(intent)
                    },
                    ButtonItem("Security", R.drawable.privacy) {
                        context.startActivity(intent)
                    },
                    ButtonItem("Notifications", R.drawable.notifications) {
                        context.startActivity(intent)
                    },
                    DividerItem,
                    SubtitleItem("Customization"),
                    ButtonItem("Appearance", R.drawable.brush) {
                        context.startActivity(intent)
                    },
                    DividerItem,
                    SubtitleItem("Utility"),
                    ButtonItem("Low stock items", R.drawable.checklist) {
                        context.startActivity(intent)
                    },
                    DividerItem,
                    SubtitleItem("Lists"),
                    ListItem("List"),
                    DividerItem,
                    ButtonItem("About", R.drawable.copyright) {
                        context.startActivity(intent)
                    }
                )
            }

            AppTheme {
                // Scaffold provides a standard layout structure for Material Design apps.
                Scaffold(
                    bottomBar = {
                        BottomNavigation(activeTab = 2)
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Column(modifier = Modifier.padding(innerPadding)) {
                        TopTitle(text = "Settings")
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            items(settingsItems) { item ->
                                when (item) {
                                    is ButtonItem -> SettingButton(
                                        text = item.text,
                                        icon = item.icon,
                                        onClick = item.onClick
                                    )

                                    is ListItem ->
                                        for (list in stashLists) {
                                            SettingButton(
                                                text = list.pgName,
                                                icon = R.drawable.label,
                                                onClick = {
                                                    currentListIndex.intValue =
                                                        stashLists.indexOf(list)
                                                    context.startActivity(
                                                        Intent(
                                                            context,
                                                            StashActivity::class.java
                                                        )
                                                    )
                                                }
                                            )
                                        }

                                    is DividerItem -> HorizontalDivider()
                                    is SubtitleItem -> SettingSubTitle(item.text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A composable that displays a setting button with an icon and text.
 *
 * @param modifier The modifier to be applied to the button.
 * @param text The text to display on the button.
 * @param icon The drawable resource ID for the icon.
 * @param onClick The lambda to execute when the button is clicked.
 */
@Composable
fun SettingButton(modifier: Modifier = Modifier, text: String, icon: Int, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(start = EDGEPADDING.dp, bottom = 0.dp, top = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != 0) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                textAlign = TextAlign.Start,
                fontSize = TEXTFONTSIZE.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * A composable that displays a subtitle for a group of settings.
 *
 * @param text The text to display as the subtitle.
 */
@Composable
fun SettingSubTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Text(
            text = text,
            fontSize = (TEXTFONTSIZE - 1).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false,
                ),
            ),
            modifier = Modifier
                .padding(
                    start = EDGEPADDING.dp,
                )
                .fillMaxWidth()
        )
    }
}

/**
 * A preview for the settings screen.
 */
@Preview
@Composable
fun PreviewSettings() {
    AppTheme {
        // Scaffold provides a standard layout structure for Material Design apps.
        Scaffold(
            bottomBar = {
                BottomNavigation(activeTab = 2)
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->

            Column(modifier = Modifier.padding(innerPadding)) {
                TopTitle(text = "Features")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(settingsItems) { item ->
                        when (item) {
                            is ButtonItem -> SettingButton(
                                text = item.text,
                                icon = item.icon,
                                onClick = item.onClick
                            )

                            is ListItem ->
                                for (list in stashLists) {
                                    SettingButton(
                                        text = list.pgName,
                                        icon = 0,
                                        onClick = { }
                                    )
                                }

                            is DividerItem -> HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp))
                            is SubtitleItem -> SettingSubTitle(item.text)
                        }
                    }
                }
            }
        }
    }
}
