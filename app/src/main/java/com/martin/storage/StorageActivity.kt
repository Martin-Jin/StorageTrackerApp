package com.martin.storage

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.data.RowItem
import com.martin.storage.data.storageItems
import com.martin.storage.data.updateStoredItems
import com.martin.storage.ui.theme.TestTheme

// --- Constants ---
private const val TAG = "StorageActivity"
const val imgWidth = 0.2f

/**
 * The main activity for displaying and managing storage items.
 * It sets up the Compose UI and initializes the list of items to be displayed.
 */
class StorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- State Initialization ---
        // `mutableStateListOf` creates an observable list. Any composable that reads this list
        // will automatically recompose when items are added or removed.
        val displayItems = mutableStateListOf<RowItem>()
        // The global `storageItems` list (from DataManagement.kt) is loaded in MainActivity.
        // We convert the `LocalRowItem` objects to `RowItem` objects suitable for the UI.
        for (item in storageItems) {
            displayItems.add(item.toRowItem())
        }

        Log.d(TAG, "onCreate: Displaying ${displayItems.size} items from local storage.")

        setContent {
            TestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // The TopTab composable is the main UI for this screen.
                        TopTab(displayItems = displayItems)
                    }
                }
            }
        }
    }
}


// --- Main UI Composable ---

/**
 * The main composable that organizes the UI with a tabbed layout.
 * It manages the currently selected tab and displays the corresponding items.
 * @param displayItems The observable list of items to display and manage.
 */
@Composable
fun TopTab(modifier: Modifier = Modifier, displayItems: SnapshotStateList<RowItem>) {

    // State for the currently selected tab index.
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fridge", "Cabinet", "Others")
    val context = LocalContext.current
    // `rememberCoroutineScope` provides a scope for launching background tasks that is tied to this composable's lifecycle.
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        // The TabRow UI component.
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Display the title based on the selected tab.
        when (selectedTab) {
            0 -> Title("Fridge")
            1 -> Title("Cabinet")
            2 -> Title("Others")
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display the list of items filtered by the current tab.
            DisplayRows(items = displayItems, pgIndex = selectedTab)
            // The button to add a new item.
            AddButton(onClick = {
                val newRowItem = RowItem(pgIndex = selectedTab)
                displayItems.add(newRowItem)
                // Save the updated list to DataStore.
                updateStoredItems(context, scope, displayItems, true)
            })
        }
    }
}

/**
 * Displays the rows of items in a scrollable `LazyColumn`.
 * It filters the master list to only show items for the currently selected page/tab.
 * @param items The master list of all `RowItem` objects.
 * @param pgIndex The page index to filter by.
 */
@Composable
fun DisplayRows(
    items: SnapshotStateList<RowItem>,
    pgIndex: Int
) {
    // Filter the list to get only the items for the current tab.
    val itemsToShow = items.filter { it.pgIndex == pgIndex }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // `items` is a key performance feature. It only composes and lays out the items that are currently visible.
        items(itemsToShow) { item ->
            ItemRow(rowItem = item, onDelete = { items.remove(item) }, allItems = items)
        }
    }
}

/**
 * A single row in the list, representing one storage item.
 * @param rowItem The item to display.
 * @param onDelete A callback to execute when this item should be deleted.
 * @param allItems The complete list of items, passed through for saving operations.
 */
@Composable
fun ItemRow(
    rowItem: RowItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    allItems: MutableList<RowItem>
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // If `showEditDialog` is true, display the dialog composable.
    if (showEditDialog) {
        EditItemDialog(
            displayItem = rowItem,
            onDismiss = { showEditDialog = false },
            onSave = {
                // When the dialog is saved, trigger an update for the entire list in storage.
                updateStoredItems(context, scope, allItems, true)
                showEditDialog = false
            }
        )
    }

    // The main Row layout for the item.
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A pointerInput modifier to detect long presses for showing the context menu.
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showMenu = true
                    }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .heightIn(max = 80.dp)
                .fillMaxWidth()
        ) {
            // Item image
            Image(
                modifier = Modifier.weight(imgWidth),
                painter = painterResource(id = rowItem.img),
                contentDescription = rowItem.name,
                contentScale = ContentScale.Crop
            )
            // Item name and count
            Row {
                Text(text = "${rowItem.name}: ", fontSize = 20.sp)
                Text(text = "${rowItem.count}", fontSize = 20.sp)
            }
            // Buttons to increase or decrease the count
            Row {
                Button(
                    onClick = {
                        rowItem.increaseCount()
                        // Persist the change to storage.
                        updateStoredItems(context, scope, allItems)
                    }
                ) { Text(text = "+", fontSize = 15.sp) }
                Button(
                    onClick = {
                        rowItem.decreaseCount()
                        // Persist the change to storage.
                        updateStoredItems(context, scope, allItems)
                    }
                ) { Text(text = "-", fontSize = 15.sp) }
            }
        }

        // Dropdown menu for Edit/Delete, shown on long press.
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete() // This removes the item from the local `displayItems` list.
                    // Persist the deletion to storage.
                    updateStoredItems(context, scope, allItems, true)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showEditDialog = true
                    showMenu = false
                }
            )
        }
    }
}

// --- Helper Composable & Previews ---

/**
 * A simple button for adding a new item.
 */
@Composable
fun AddButton(onClick: () -> Unit) {
    Button(modifier = Modifier.padding(16.dp), onClick = onClick) {
        Text(text = "Add item", fontSize = 15.sp)
    }
}

/**
 * A dialog for editing the name and count of an item.
 * @param displayItem The item being edited.
 * @param onDismiss Callback for when the dialog is dismissed.
 * @param onSave Callback for when the user confirms their edits.
 */
@Composable
fun EditItemDialog(
    displayItem: RowItem,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    // `remember` with keys: if the displayItem changes, the state for name/count will reset to the new item's values.
    var nameText by remember(displayItem) { mutableStateOf(displayItem.name) }
    var countText by remember(displayItem) { mutableStateOf(displayItem.count.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Item") },
        text = {
            Column {
                TextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Item Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Update the actual RowItem's properties.
                    // Because `RowItem` uses Compose State delegates, the UI will automatically update.
                    displayItem.name = nameText
                    displayItem.count = countText.toIntOrNull() ?: 0 // Safely parse to Int
                    onSave()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * A preview composable for visualizing the StorageActivity UI.
 */
@Preview(showSystemUi = true)
@Composable
fun StoragePreview() {
    TestTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // In a preview, we can provide a dummy, non-interactive list.
            }
        }
    }
}
