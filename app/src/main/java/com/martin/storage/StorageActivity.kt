package com.martin.storage

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.rememberAsyncImagePainter
import com.martin.storage.data.DisplayTabItem
import com.martin.storage.data.LAST_OPENED_KEY
import com.martin.storage.data.LocalRowItem
import com.martin.storage.data.RowItem
import com.martin.storage.data.RowItem.Companion.itemToEdit
import com.martin.storage.data.STORAGEITEMPATH
import com.martin.storage.data.TABITEMSPATH
import com.martin.storage.data.TAG
import com.martin.storage.data.TabItem
import com.martin.storage.data.appendObjects
import com.martin.storage.data.readLocalData
import com.martin.storage.data.saveImageFromUri
import com.martin.storage.data.storageItems
import com.martin.storage.data.tabItems
import com.martin.storage.data.writeLocalData
import com.martin.storage.data.writeLocalObjects
import com.martin.storage.ui.theme.AppTheme
import com.martin.storage.ui.theme.BottomNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

// --- Constants ---
private const val TAG = "StorageActivity"
const val ITEMFONTSIZE = 16
const val ROWBORDERRADIUS = 20
const val EDGEPADDING = 20
const val TOPPADDING = 10

/**
 * The primary activity for displaying and managing a user's inventory across different storage locations.
 * This activity sets up the main Compose UI, which includes a tabbed layout for different storage
 * areas (e.g., "Fridge", "Cabinet"), a list of items within each tab, and functionality for
 * adding, editing, and deleting items. It also handles lifecycle events to persist data.
 */
class StorageActivity : ComponentActivity() {
    // A reactive list of `RowItem` objects that the UI will observe for changes.
    private val displayItems = mutableStateListOf<RowItem>()
    private val tabs = mutableStateListOf<DisplayTabItem>()


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- State Initialization ---
        // `mutableStateListOf` creates an observable list. Any composable that reads from this list
        // will automatically recompose when its contents change (items are added, removed, or updated).
        // The global `storageItems` list (from DataManagement.kt), loaded in MainActivity, is converted
        // to UI-specific `RowItem` objects. The same for other variables to save
        displayItems.addAll(storageItems.map { RowItem(it) })
        tabs.addAll(tabItems.map { DisplayTabItem(it) })

        Log.d(TAG, "onCreate: Displaying ${displayItems.size} items for storage.")
        Log.d(TAG, "onCreate: Displaying ${tabs.size} items for tabs.")

        // This lifecycle scope automatically handles starting and stopping the coroutine
        // in sync with the activity's lifecycle, preventing resource leaks.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                // Read the timestamp of the last time the app was opened.
                val last = readLocalData(this@StorageActivity, LAST_OPENED_KEY).firstOrNull()
                val now = System.currentTimeMillis().toString()
                if (last != null) {
                    // If a last-opened time exists, update the decrement for each item.
                    for (item in displayItems) {
                        item.updateDecrement(last = last, now = now)
                    }
                } else {
                    // If it's the first time, just record the current time.
                    writeLocalData(
                        this@StorageActivity,
                        LAST_OPENED_KEY,
                        System.currentTimeMillis().toString()
                    )
                }
            }
        }
        // Enables edge-to-edge display for a more immersive UI.
        enableEdgeToEdge()
        setContent {
            AppTheme {
                // Scaffold provides a standard layout structure for Material Design apps.
                Scaffold(
                    bottomBar = {
                        BottomNavigation(activeTab = 1)
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Row(
                            modifier = Modifier
                                .padding(start = EDGEPADDING.dp, top = TOPPADDING.dp)
                                .fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Stash",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false,
                                        ),
                                    ),
                                )
                                Text(
                                    text = "Total: ${displayItems.size} items", fontSize = 15.sp,
                                )
                            }

                        }
                        Box {
                            // `Tabs` is the main composable for the screen's primary content.
                            Tabs(allItems = displayItems, tabs = tabs)
                            // This button provides a way for the user to add a new item.
                            Button(
                                shape = RoundedCornerShape(15.dp),
                                contentPadding = PaddingValues(5.dp),
                                modifier = Modifier
                                    .size(width = 90.dp, height = 90.dp)
                                    .padding(20.dp)
                                    .align(Alignment.BottomEnd),
                                onClick = {
                                    val newRowItem = RowItem(initialName = "New item")
                                    displayItems.add(newRowItem)
                                    itemToEdit.value = newRowItem
                                }) {
                                Icon(
                                    painter = painterResource(R.drawable.plus),
                                    contentDescription = "Add new item"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * When the activity is paused (e.g., user navigates away), this function is called
     * to save user data.
     */
    override fun onPause() {
        super.onPause()
        // Converting to the appropriate types
        val localRowItems = mutableListOf<LocalRowItem>()
        for (item in displayItems) {
            localRowItems.add(item.toLocalRowItem())
        }

        val tabsToSave = mutableListOf<TabItem>()
        for (tab in tabs) {
            tabsToSave.add(tab.toTabItem())
        }

        // Saving values
        updateStoredValue(this, lifecycleScope, localRowItems, STORAGEITEMPATH)
        updateStoredValue(this, lifecycleScope, tabsToSave, TABITEMSPATH)
    }
}

// --- Main UI Composable ---
/**
 * The core composable that organizes the UI using a tabbed layout. It manages the state
 * of the currently selected tab and uses a `HorizontalPager` to allow for swipeable
 * navigation between the different storage locations.
 *
 * @param modifier A `Modifier` passed from the parent.
 * @param allItems The observable list of all storage items, which will be filtered for each tab.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    allItems: SnapshotStateList<RowItem>,
    tabs: SnapshotStateList<DisplayTabItem>
) {
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // The dialog for editing an item is defined here but only shown when `itemToEdit.value` is not null.
    EditItemDialog(
        itemToEdit = itemToEdit.value,
        onDismiss = { itemToEdit.value = null },
        onSave = {
            itemToEdit.value = null
        }
    )

    Column(modifier.fillMaxSize()) {
        // The `SecondaryScrollableTabRow` provides the tab navigation UI.
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = { },
            tabs = {
                for (tab in tabs) {
                    Tab(
                        modifier = Modifier.heightIn(max = 40.dp),
                        selected = pagerState.currentPage == tab.index,
                        onClick = {
                            // Animate to the selected tab when clicked.
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.index)
                            }
                        },
                        text = { Text(text = tab.name, fontSize = 15.sp) },
                    )
                }
                // A button for adding a new tab category in the future.
                Button(
                    modifier = Modifier
                        .heightIn(max = 15.dp)
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(ROWBORDERRADIUS.dp)),
                    contentPadding = PaddingValues(5.dp),
                    onClick = {},
                )
                {
                    Text(text = "New")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // `HorizontalPager` provides the swipeable content area for each tab.
        HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
            // `derivedStateOf` is a performance optimization. The filter operation only
            // re-runs if `allItems` or `page` changes.
            val itemsToShow by remember { derivedStateOf { allItems.filter { it.pgIndex == page } } }
            StorageScreen(
                allItems = allItems,
                itemsToShow = itemsToShow,
            )
        }
    }
}

/**
 * A composable that displays a list of storage items for a single tab.
 * It uses a `LazyColumn` for efficiently displaying a potentially long, scrollable list.
 *
 * @param itemsToShow The filtered list of items to display on this screen.
 * @param allItems The complete list of all items, passed down to be modified by child composables.
 */
@Composable
fun StorageScreen(
    itemsToShow: List<RowItem>,
    allItems: SnapshotStateList<RowItem>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Providing a unique `key` for each item helps Compose optimize recompositions,
        // especially when the list changes (items are added, removed, or reordered).
        items(
            items = itemsToShow,
            key = { item -> item.id }
        ) { item ->
            ItemRow(
                rowItem = item,
                onDelete = { allItems.remove(item) },
                onEdit = { itemToEdit.value = it }
            )
        }
    }
}

/**
 * A composable representing a single row in the storage item list. It displays the item's
 * image, name, and quantity, and provides controls for modifying the count and accessing
 * a dropdown menu for editing or deleting the item.
 *
 * @param rowItem The `RowItem` data to display.
 * @param onDelete A callback lambda to be executed when the user chooses to delete this item.
 * @param onEdit A callback lambda to set the globally observed `itemToEdit`, triggering the edit dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    rowItem: RowItem,
    onDelete: () -> Unit,
    onEdit: (RowItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // `remember` with `rowItem.img` as a key ensures the image model is re-evaluated
    // only when the image path for this specific item changes.
    val imageModel = remember(rowItem.img) {
        // Handle both local file paths and drawable resource IDs.
        rowItem.img.toIntOrNull() ?: File(rowItem.img)
    }

    Box {
        Card(
            shape = RoundedCornerShape(ROWBORDERRADIUS.dp),
            modifier = Modifier
                .heightIn(0.dp, 62.5.dp)
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = EDGEPADDING.dp)
                .pointerInput(Unit) {
                    // Open the dropdown menu on a simple tap.
                    detectTapGestures(onTap = { showMenu = true })
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Asynchronously load and display the item's image using Coil.
                Image(
                    painter = rememberAsyncImagePainter(model = imageModel),
                    contentDescription = rowItem.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width((ROWBORDERRADIUS * 3.5).dp)
                        .fillMaxHeight()
                        .clip(
                            RoundedCornerShape(
                                topStart = ROWBORDERRADIUS.dp,
                                bottomStart = ROWBORDERRADIUS.dp
                            )
                        )
                )

                Row(
                    modifier = Modifier.weight(0.7f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // `basicMarquee` provides a scrolling animation if the item name is too long.
                    Text(
                        text = rowItem.name,
                        fontSize = ITEMFONTSIZE.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Buttons to increment and decrement the item count.
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        onClick = { rowItem.increaseCount() },
                        modifier = Modifier.size(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(text = "+") }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${rowItem.count} ${rowItem.unit}",
                        maxLines = 1,
                        fontSize = (ITEMFONTSIZE * 0.9).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        onClick = { rowItem.decreaseCount() },
                        modifier = Modifier.size(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(text = "-") }
                }
            }
        }

        // The dropdown menu is anchored to the `Box` and is only expanded when `showMenu` is true.
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onEdit(rowItem)
                    showMenu = false
                }
            )
        }
    }
}

// --- Helper Composable & Previews ---
/**
 * A comprehensive dialog for creating or editing a storage item. It provides text fields for all
 * item properties, a dropdown for selecting the page/tab, and an image picker.
 *
 * @param itemToEdit The `RowItem` to be edited. If `null`, the dialog is not shown.
 * @param onDismiss A callback to dismiss the dialog without saving changes.
 * @param onSave A callback to save the changes and dismiss the dialog.
 */
@Composable
fun EditItemDialog(
    itemToEdit: RowItem?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    if (itemToEdit == null) return

    val context = LocalContext.current
    // Using `remember` with `itemToEdit.id` as a key ensures that the dialog's state
    // resets correctly when a different item is selected for editing.
    var nameText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.name) }
    var countText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.count.toString()) }
    var unitText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.unit) }
    var pgIndex by remember(itemToEdit.id) { mutableIntStateOf(itemToEdit.pgIndex) }
    var imgText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.img) }
    var decrementText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.decrement.toString()) }
    var decrementIntervalText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.decrementInterval.toString()) }

    // `rememberLauncherForActivityResult` is the modern way to handle activity results in Compose.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Save the selected image to internal storage and update the image path state.
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
                    label = { Text("Name") },
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
                    TextField(
                        value = decrementText,
                        onValueChange = { decrementText = it },
                        label = { Text("Decrement") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(150.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("every")
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = decrementIntervalText,
                        onValueChange = { decrementIntervalText = it },
                        label = { Text("Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Page:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Button(onClick = { expanded = true }) { Text(pgIndex.toString()) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            pages.forEach { page ->
                                DropdownMenuItem(text = { Text(page.toString()) }, onClick = {
                                    pgIndex = page
                                    expanded = false
                                })
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Button(
                    onClick = {
                        // When saved, update the original `RowItem` with the new values from the dialog's state.
                        itemToEdit.name = nameText
                        itemToEdit.count = countText.toIntOrNull() ?: 0
                        itemToEdit.unit = unitText
                        itemToEdit.pgIndex = pgIndex
                        itemToEdit.img = imgText
                        itemToEdit.decrement = decrementText.toIntOrNull() ?: 1
                        itemToEdit.decrementInterval = decrementIntervalText.toIntOrNull() ?: 1
                        onSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    )
                ) { Text(text = "Save") }
                Spacer(Modifier.width(15.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    )
                ) { Text(text = "Cancel") }
            }
        }
    )
}

/**
 * Creates a coroutine scope to write data to storage.
 *
 * @param context The context required for DataStore access.
 * @param scope A `CoroutineScope` to launch the asynchronous save operation.
 * @param itemsToSave The list of objects from the UI to be saved.
 * @param overWrite If `true`, the entire existing list in storage is replaced.
 *                  If `false`, the new items are appended to the existing list.
 */
inline fun <reified T> updateStoredValue(
    context: Context,
    scope: CoroutineScope,
    itemsToSave: MutableList<T>,
    key: String,
    overWrite: Boolean = true,
) {
    scope.launch {
        if (overWrite) {
            Log.i(TAG, "Updating stored items with OVERWRITE.")
            writeLocalObjects(
                context,
                key,
                itemsToSave
            )
        } else {
            Log.i(TAG, "Updating stored items with APPEND.")
            appendObjects(
                context,
                key,
                itemsToSave
            )
        }
    }
}

/**
 * A preview for the `StorageScreen` composable, allowing for quick UI iteration in Android Studio.
 * This preview simulates the main layout of the activity, including the bottom navigation and a
 * sample set of items, without needing to run the full application.
 */
@Preview(showBackground = true)
@OptIn(ExperimentalTextApi::class)
@Composable
fun StorageScreenPreview() {
    AppTheme {
        Scaffold(
            bottomBar = {
                BottomNavigation(activeTab = 1)
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
                            text = "Stash",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false,
                                ),
                            ),
                        )
                        Text(
                            text = "Total: 1 items", fontSize = 15.sp,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false,
                                ),
                            ),
                        )
                    }

                }
                Box {
                    // The `Tabs` composable is the main UI for this screen.
                    // `@SuppressLint` is used here because this is a preview, and we don't need to remember the state.
                    @SuppressLint("UnrememberedMutableState")
                    Tabs(
                        allItems = mutableStateListOf(RowItem()),
                        tabs = mutableStateListOf(
                            DisplayTabItem("Fridge", 0),
                            DisplayTabItem("Fridge", 0), DisplayTabItem("Fridge", 0)
                        )
                    )

                    Button(
                        shape = RoundedCornerShape(15.dp),
                        contentPadding = PaddingValues(5.dp),
                        modifier = Modifier
                            .size(width = 90.dp, height = 90.dp)
                            .padding(20.dp)
                            .align(Alignment.BottomEnd),
                        onClick = {
                        }) {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            contentDescription = "icon"
                        )
                    }
                }
            }
        }
    }
}
