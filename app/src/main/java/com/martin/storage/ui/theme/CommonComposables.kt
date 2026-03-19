package com.martin.storage.ui.theme

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.MainActivity
import com.martin.storage.R
import com.martin.storage.SettingActivity
import com.martin.storage.StashActivity

const val EDGEPADDING = 20
const val TEXTFONTSIZE = 16

@Composable
fun BottomNavigation(modifier: Modifier = Modifier, activeTab: Int) {
    var activeBottomTab by remember { mutableIntStateOf(activeTab) }
    val context = LocalContext.current
    val bottomTabs = listOf(
        { context.startActivity(Intent(context, MainActivity::class.java)) },
        { context.startActivity(Intent(context, StashActivity::class.java)) },
        { context.startActivity(Intent(context, SettingActivity::class.java)) }
    )
    val tabIcons = listOf(R.drawable.homeicon, R.drawable.checklist, R.drawable.options)

    NavigationBar(modifier = modifier.height(75.dp)) {
        bottomTabs.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(tabIcons[index]),
                        contentDescription = "Bottom navigation icon",
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
}

@Composable
fun TopTitle(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = EDGEPADDING.dp, top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            modifier = Modifier.padding(0.dp),
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false,
                ),
            ),
        )
    }
}

data class DropDownButton(val text: String, val icon: Int = 0, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPopUp(
    title: String,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    buttons: List<DropDownButton> = emptyList(),
    fullScreen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {}
) {

    if (!expanded) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )
    ModalBottomSheet(
        modifier = Modifier.windowInsetsPadding(WindowInsets(0)),
                onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fullScreen) Modifier.fillMaxHeight()
                    else Modifier.wrapContentHeight()
                )
        ) {

            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (buttons.isNotEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    buttons.forEach { button ->

                        // Pre-measure touch target for smoother click handling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    button.onClick()
                                }
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                if (button.icon != 0) {
                                    Icon(
                                        painter = painterResource(button.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(Modifier.width(14.dp))
                                }

                                Text(
                                    text = button.text,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            content()
        }
    }

    // Force sheet dismissal cleanup after animation
    LaunchedEffect(expanded) {
        if (!expanded && sheetState.isVisible) {
            sheetState.hide()
        }
    }
}

@Composable
fun SimpleAlertDialog(title: String, message: String, onDismissRequest: () -> Unit) {
    // The AlertDialog composable
    AlertDialog(
        modifier = Modifier.padding(13.dp),
        onDismissRequest = onDismissRequest,
        title = {
            Text(title)
        },
        text = {
            Text(message)
        },
        confirmButton = {
        }
    )
}

@Preview(showBackground = true)
@Composable
fun Preview(title: String = "wasd", message: String = "wasd", onDismissRequest: () -> Unit = {}) {
    // The AlertDialog composable
    AlertDialog(
        modifier = Modifier.padding(13.dp),
        onDismissRequest = onDismissRequest,
        title = {
            Text(title)
        },
        text = {
            Text(message)
        },
        confirmButton = {
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BottomPopUpPreview() {
    var showSheet by remember { mutableStateOf(true) }
    val buttons = listOf(
        DropDownButton("Edit", icon = R.drawable.outline_edit_24) { /* Handle Edit */ },
        DropDownButton("Delete", icon = R.drawable.outline_edit_24) { /* Handle Delete */ },
        DropDownButton("Share", icon = R.drawable.outline_edit_24) { /* Handle Share */ }
    )
    AppTheme {
        BottomPopUp(
            title = "Options",
            expanded = true,
            onDismissRequest = { showSheet = !showSheet },
            buttons = buttons
        )
    }
}