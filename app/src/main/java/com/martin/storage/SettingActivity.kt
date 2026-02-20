package com.martin.storage

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.ui.theme.AppTheme
import com.martin.storage.ui.theme.BottomNavigation
import com.martin.storage.ui.theme.TopTitle

// --- Constants ---
private const val TAG = "SettingActivity"
const val BUTTONHEIGHT = 2

// --- Data Models for Settings ---
sealed class SettingItem
data class ButtonItem(val text: String, val icon: Int, val onClick: () -> Unit) : SettingItem()
object DividerItem : SettingItem()
data class SubtitleItem(val text: String) : SettingItem()

val settingsItems = listOf(
    ButtonItem("Account", R.drawable.account_circle, {}),
    ButtonItem("Security", R.drawable.privacy, {}),
    ButtonItem("Notifications", R.drawable.notifications, {}),
    DividerItem,
    SubtitleItem("Customization"),
    ButtonItem("Appearance", R.drawable.brush, {}),
    ButtonItem("Placeholder", R.drawable.placeholder, {}),
    ButtonItem("Placeholder", R.drawable.placeholder, {}),
    DividerItem,
    SubtitleItem("Utility"),
    ButtonItem("Low stock items", R.drawable.checklist, {}),
    ButtonItem("Placeholder", R.drawable.placeholder, {}),
    ButtonItem("Placeholder", R.drawable.placeholder, {}),
    DividerItem,
    ButtonItem("About", R.drawable.copyright, {})
)

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

                                    is DividerItem -> HorizontalDivider()
                                    is SubtitleItem -> {
                                        SettingSubTitle(item.text)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingButton(modifier: Modifier = Modifier, text: String, icon: Int, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = BUTTONHEIGHT.dp, horizontal = 0.dp),
        onClick = onClick,
        contentPadding = PaddingValues(start = EDGEPADDING.dp, bottom = 0.dp, top = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                textAlign = TextAlign.Start,
                fontSize = ITEMFONTSIZE.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingSubTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(
            text = text,
            fontSize = (ITEMFONTSIZE - 1).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(
                    start = EDGEPADDING.dp,
                )
                .fillMaxSize()
        )
    }
}

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

                            is DividerItem -> HorizontalDivider()
                            is SubtitleItem -> {
                                SettingSubTitle(item.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

