package com.martin.storage.ui.theme

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.martin.storage.EDGEPADDING
import com.martin.storage.MainActivity
import com.martin.storage.R
import com.martin.storage.SettingActivity
import com.martin.storage.StashActivity
import com.martin.storage.TOPPADDING
import kotlinx.coroutines.launch

const val TOPTABHEIGHT = 52
const val DIALOGPADDING = 13

@Composable
fun BottomNavigation(modifier: Modifier = Modifier, activeTab: Int) {
    var activeBottomTab by remember { mutableIntStateOf(activeTab) }
    val context = LocalContext.current
    val bottomTabs = listOf(
        { context.startActivity(Intent(context, MainActivity::class.java)) },
        { context.startActivity(Intent(context, StashActivity::class.java)) },
        { context.startActivity(Intent(context, SettingActivity::class.java)) }
    )
    val tabIcons = listOf(R.drawable.homeicon, R.drawable.storagelist, R.drawable.options)

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
            .height(TOPTABHEIGHT.dp)
            .padding(start = EDGEPADDING.dp, top = TOPPADDING.dp),
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
    buttons: List<DropDownButton>
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = {
                // Draws the default drag handle without the ripple effect.
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                )
            }
        ) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp
                )

                buttons.forEach { button ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    button.onClick()
                                    sheetState.hide()
                                }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        onDismissRequest()
                                    }
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (button.icon != 0) {
                            Icon(
                                modifier = Modifier.size(21.dp),
                                painter = painterResource(button.icon),
                                contentDescription = "search icon",
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = button.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleAlertDialog(title: String, message: String, onDismissRequest: () -> Unit) {
    // The AlertDialog composable
    AlertDialog(
        modifier = Modifier.padding(DIALOGPADDING.dp),
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
        modifier = Modifier.padding(DIALOGPADDING.dp),
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