package com.martin.storage

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.martin.storage.data.RowItem
import com.martin.storage.data.saveImageFromUri
import com.martin.storage.data.storageItems
import com.martin.storage.data.updateStoredItems
import com.martin.storage.ui.theme.TestTheme
import java.io.File

// --- Constants ---
private const val TAG = "StorageActivity"
const val ITEMFONTSIZE = 17

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
                        Tabs(displayItems = displayItems)
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
fun Tabs(modifier: Modifier = Modifier, displayItems: SnapshotStateList<RowItem>) {

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
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            var titleText by remember { mutableStateOf("Fridge") }
            when (selectedTab) {
                0 -> titleText = "Fridge"
                1 -> titleText = "Cabinet"
                2 -> titleText = "Others"
            }
            Title(titleText)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display the list of items filtered by the current tab.
            DisplayRows(items = displayItems, pgIndex = selectedTab)

            // The button to add a new item.
            Button(modifier = Modifier.padding(16.dp), onClick = {
                val newRowItem = RowItem(initialPgIndex = selectedTab, initialName = "New item")
                displayItems.add(newRowItem)
                // Save the updated list to DataStore.
                updateStoredItems(context, scope, displayItems, true)
            }) {
                Text(text = "Add item", fontSize = 15.sp)
            }
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
        // By providing a key, we help Compose understand which items are which, improving performance.
        items(
            items = itemsToShow,
            key = { item -> item.id }
        ) { item ->
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    rowItem: RowItem,
    onDelete: () -> Unit,
    allItems: MutableList<RowItem>
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showEditDialog || rowItem.name.contains("New item")) {
        EditItemDialog(
            displayItem = rowItem,
            onDismiss = { showEditDialog = false },
            onSave = {
                updateStoredItems(context, scope, allItems, true)
                showEditDialog = false
            }
        )
    }


    Card(
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .heightIn(0.dp, 70.dp)
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showMenu = true
                    }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = rowItem.img.toIntOrNull() ?: File(rowItem.img)
                ),
                contentDescription = rowItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp))
            )

            Spacer(Modifier.width(16.dp))

            Row(
                modifier = Modifier.weight(0.7f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${rowItem.name}: ${rowItem.count} ${rowItem.unit}",
                    fontSize = ITEMFONTSIZE.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        rowItem.increaseCount()
                        updateStoredItems(context, scope, allItems)
                    },
                    modifier = Modifier.size(30.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(text = "+") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        rowItem.decreaseCount()
                        updateStoredItems(context, scope, allItems)
                    },
                    modifier = Modifier.size(30.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(text = "-") }
            }
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
    ) {
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDelete()
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

// --- Helper Composable & Previews ---
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
    val context = LocalContext.current
    // `remember` with keys: if the displayItem changes, the state for name/count will reset to the new item's values.
    var nameText by remember { mutableStateOf(displayItem.name) }
    var countText by remember { mutableStateOf(displayItem.count.toString()) }
    var unitText by remember { mutableStateOf(displayItem.unit) }
    var pgIndex by remember { mutableIntStateOf(displayItem.pgIndex) }
    var imgText by remember { mutableStateOf(displayItem.img) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val imagePath = saveImageFromUri(context, it)
                if (imagePath != null) {
                    imgText = imagePath
                }
            }
        }
    )

    var expanded by remember { mutableStateOf(false) }
    val pages = listOf(0, 1, 2)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Item") },
        text = {
            Column {
                TextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Name (Don't use New item)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = unitText,
                    onValueChange = { unitText = it },
                    label = { Text("Unit") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Page:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Button(onClick = { expanded = true }) {
                            Text(pgIndex.toString())
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            pages.forEach { page ->
                                DropdownMenuItem(
                                    text = { Text(page.toString()) },
                                    onClick = {
                                        pgIndex = page
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = imgText,
                    onValueChange = { imgText = it },
                    label = { Text("Image URI / Resource ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Button(onClick = { imagePicker.launch("image/*") }) {
                    Text("Upload Image")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    displayItem.name = nameText
                    displayItem.count = countText.toIntOrNull() ?: 0
                    displayItem.unit = unitText
                    displayItem.pgIndex = pgIndex
                    displayItem.img = imgText
                    onSave()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}