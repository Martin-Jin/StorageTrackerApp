package com.martin.storage

import android.annotation.SuppressLint
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Surface
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
import com.martin.storage.data.DisplayTabItem.Companion.tabToEdit
import com.martin.storage.data.LAST_OPENED_KEY
import com.martin.storage.data.LocalRowItem
import com.martin.storage.data.RowItem
import com.martin.storage.data.RowItem.Companion.itemToEdit
import com.martin.storage.data.STORAGEITEMPATH
import com.martin.storage.data.TABITEMSPATH
import com.martin.storage.data.TAG
import com.martin.storage.data.TabItem
import com.martin.storage.data.readLocalData
import com.martin.storage.data.saveImageFromUri
import com.martin.storage.data.storageItems
import com.martin.storage.data.tabItems
import com.martin.storage.data.updateStoredValue
import com.martin.storage.data.writeLocalData
import com.martin.storage.ui.theme.AppTheme
import com.martin.storage.ui.theme.BottomNavigation
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

// --- Constants ---
private const val TAG = "StorageActivity"
const val ITEMFONTSIZE = 16
const val ROWBORDERRADIUS = 20
const val EDGEPADDING = 20
const val TOPPADDING = 10

// --- Filters by tags or searches ---
private val nameFilters = mutableStateListOf<String>()

// --- item data ---
private var currentTabIndex = mutableIntStateOf(0)
private val allItems = mutableStateListOf<RowItem>()
private val tabs = mutableStateListOf<DisplayTabItem>()

/**
 * The primary activity for displaying and managing a user's inventory across different storage locations.
 * This activity sets up the main Compose UI, which includes a tabbed layout for different storage
 * areas (e.g., "Fridge", "Cabinet"), a list of items within each tab, and functionality for
 * adding, editing, and deleting items. It also handles lifecycle events to persist data.
 */
class StorageActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- State Initialization ---
        // `mutableStateListOf` creates an observable list. Any composable that reads from this list
        // will automatically recompose when its contents change (items are added, removed, or updated).
        // The global `storageItems` list (from DataManagement.kt), loaded in MainActivity, is converted
        // to UI-specific `RowItem` objects. The same for other variables to save
        allItems.addAll(storageItems.map { RowItem(it) })
        tabs.addAll(tabItems.map { DisplayTabItem(it) })

        Log.d(TAG, "onCreate: Displaying ${allItems.size} items for storage.")
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
                    for (item in allItems) {
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
                        TitleElements(allItems.size)
                        Box {
                            // `Tabs` is the main composable for the screen's primary content.
                            Tabs(allItems = allItems, tabs = tabs)
                            // This button provides a way for the user to add a new item.
                            NewRowItemBtn(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                allItems = allItems
                            )
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
        for (item in allItems) {
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

// --- Utility functions ---
fun rowItemFilter(
    nameFilter: MutableList<String>,
    itemsToFilter: List<RowItem>
): List<RowItem> {
    if (nameFilter.isEmpty()) {
        return itemsToFilter
    }

    val filteredItems = mutableListOf<RowItem>()
    for (item in itemsToFilter) {
        for (filter in nameFilter)
            if (!item.name.contains(other = filter, ignoreCase = true)) {
                break
            } else {
                filteredItems.add(item)
            }
    }
    return filteredItems
}

// --- Main UI Composable ---
@Composable
fun NewRowItemBtn(allItems: SnapshotStateList<RowItem>, modifier: Modifier) {
    Button(
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(5.dp),
        modifier = modifier
            .size(width = 90.dp, height = 90.dp)
            .padding(20.dp),
        onClick = {
            val newRowItem = RowItem(initialName = "New item", initialPgIndex = currentTabIndex.intValue)
            allItems.add(newRowItem)
            itemToEdit.value = newRowItem
        }) {
        Icon(
            painter = painterResource(R.drawable.plus),
            contentDescription = "Add new item"
        )
    }
}

@Composable
fun SearchBarPopup(onDismiss: () -> Unit, onSearch: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) { Text(text = "Search") }
        },
        text = {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search query") },
                singleLine = true
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        onSearch(searchQuery)
                        nameFilters.add(searchQuery)
                        onDismiss()
                    }
                ) {
                    Text("Search")
                }
            }
        }
    )
}

@Composable
fun TitleElements(totalItems: Int) {
    var showSearchBar by remember { mutableStateOf(false) }

    if (showSearchBar) {
        SearchBarPopup(
            onDismiss = { showSearchBar = false },
            onSearch = { query ->
                Log.d("TitleElements", "Search for: $query")
                showSearchBar = false
            }
        )
    }
    Row(
        modifier = Modifier
            .padding(start = EDGEPADDING.dp, end = EDGEPADDING.dp, top = TOPPADDING.dp)
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
                text = "Total: $totalItems items", fontSize = 15.sp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.search_database),
            contentDescription = "search icon",
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures { showSearchBar = true }
            }
        )
    }
}


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
    tabs: SnapshotStateList<DisplayTabItem>,
) {
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    currentTabIndex.intValue = pagerState.currentPage

    // The dialog for editing an item is defined here but only shown when `itemToEdit.value` is not null.
    EditItemDialog(
        itemToEdit = itemToEdit.value,
        onDismiss = { itemToEdit.value = null },
        onSave = {
            itemToEdit.value = null
        }
    )

    // The dialog for editing a tab is defined here but only shown when `tabToEdit.value` is not null.
    EditTabDialogue(
        tabToEdit.value,
        onDismiss = { tabToEdit.value = null },
        onSave = { itemToEdit.value = null })

    Column(modifier.fillMaxSize()) {
        // The `SecondaryScrollableTabRow` provides the tab navigation UI.
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = { },
            tabs = {
                for (tab in tabs) {
                    var showMenu by remember { mutableStateOf(false) }
                    Tab(
                        modifier = Modifier
                            .heightIn(max = 40.dp),
                        selected = pagerState.currentPage == tab.index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.index)
                            }
                        },
                        text = {
                            Text(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { showMenu = true },
                                        onTap = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(tab.index)
                                            }
                                        }
                                    )
                                }, text = tab.name, fontSize = 15.sp
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        tabs.remove(tab)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        tabToEdit.value = tab
                                        showMenu = false
                                    }
                                )
                            }
                        },
                    )
                }
                // A button for adding a new tab category in the future.
                Button(
                    modifier = Modifier
                        .heightIn(max = 15.dp)
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(ROWBORDERRADIUS.dp)),
                    contentPadding = PaddingValues(5.dp),
                    onClick = {
                        tabs.add(DisplayTabItem("Tab", tabs.size))
                    },
                )
                {
                    Text(text = "New")
                }
            }
        )
        if (nameFilters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            DisplayFilters()
        }
        Spacer(modifier = Modifier.height(18.dp))

        // `HorizontalPager` provides the swipeable content area for each tab.
        HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
            // `derivedStateOf` is a performance optimization. The filter operation only
            // re-runs if `allItems` or `page` changes.
            val itemsToShow by remember { derivedStateOf { allItems.filter { it.pgIndex == page } } }
            val filteredItems = rowItemFilter(nameFilters, itemsToShow)
            StorageScreen(
                allItems = allItems,
                itemsToShow = filteredItems,
            )
        }
    }
}

@Composable
fun DisplayFilters() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EDGEPADDING.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (nameFilters.isNotEmpty()) {
            Text("Filters: ", fontSize = 14.sp)
        }
        for (filter in nameFilters) {
            Surface(
                modifier = Modifier.padding(horizontal = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = filter, fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "x",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { nameFilters.remove(filter) }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditTabDialogue(tabToEdit: DisplayTabItem?, onDismiss: () -> Unit, onSave: () -> Unit) {

    if (tabToEdit == null) return
    var tabName by remember { mutableStateOf(tabToEdit.name) }

    AlertDialog(
        modifier = Modifier.padding(horizontal = 13.dp),
        onDismissRequest = { onDismiss() },
        title = { Text(text = "Edit tab") },
        text = {
            Column {
                TextField(
                    value = tabName,
                    onValueChange = { tabName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Button(
                    onClick = {
                        // When saved, update the original `RowItem` with the new values from the dialog's state.
                        tabToEdit.name = tabName
                        onSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) { Text(text = "Save") }
                Spacer(Modifier.width(15.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) { Text(text = "Cancel") }
            }
        }
    )
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
            RowItemUI(
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
fun RowItemUI(
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
                        .width((ROWBORDERRADIUS * 3.9).dp)
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
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
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
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
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
    val pages = tabItems.map { it.index }

    AlertDialog(
        modifier = Modifier.padding(horizontal = 13.dp),
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        modifier = Modifier.weight(0.5f),
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        modifier = Modifier.weight(0.5f),
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Unit") },
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = decrementText,
                        onValueChange = { decrementText = it },
                        label = { Text("Decrement") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(130.dp)
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
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Page:")
                    Spacer(modifier = Modifier.width(13.dp))
                    Column {
                        Button(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(5.dp),
                            modifier = Modifier
                                .width(65.dp)
                                .height(30.dp)
                        ) { Text(pgIndex.toString()) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            pages.forEach { page ->
                                DropdownMenuItem(text = { Text(page.toString()) }, onClick = {
                                    pgIndex = pages.indexOf(page)
                                    expanded = false
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Image:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        contentPadding = PaddingValues(5.dp),
                        modifier = Modifier
                            .width(65.dp)
                            .height(30.dp)
                    ) {
                        Text("Upload")
                    }
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
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) { Text(text = "Save") }
                Spacer(Modifier.width(15.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) { Text(text = "Cancel") }
            }
        }
    )
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
    @SuppressLint("UnrememberedMutableState")
    val allItems = mutableStateListOf(RowItem("Test"))

    @SuppressLint("UnrememberedMutableState")
    val tabs = mutableStateListOf(DisplayTabItem("Tab1", 0))
    AppTheme {
        // Scaffold provides a standard layout structure for Material Design apps.
        Scaffold(
            bottomBar = {
                BottomNavigation(activeTab = 1)
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                TitleElements(allItems.size)
                Box {
                    // `Tabs` is the main composable for the screen's primary content.
                    Tabs(allItems = allItems, tabs = tabs)
                    // This button provides a way for the user to add a new item.
                    NewRowItemBtn(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        allItems = allItems
                    )
                }
            }
        }
    }
}
