package com.martin.storage

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import com.martin.storage.customUI.DisplayTabItem
import com.martin.storage.customUI.DisplayTabItem.Companion.tabToEdit
import com.martin.storage.customUI.RowItemUI
import com.martin.storage.customUI.RowItemUI.Companion.decrementByDays
import com.martin.storage.customUI.RowItemUI.Companion.itemToEdit
import com.martin.storage.data.STORAGEITEMPATH
import com.martin.storage.data.StashList
import com.martin.storage.data.saveImageFromUri
import com.martin.storage.data.stashLists
import com.martin.storage.data.updateStoredValue
import com.martin.storage.ui.theme.AppTheme
import com.martin.storage.ui.theme.BottomNavigation
import com.martin.storage.ui.theme.BottomPopUp
import com.martin.storage.ui.theme.DIALOGPADDING
import com.martin.storage.ui.theme.DropDownButton
import com.martin.storage.ui.theme.SimpleAlertDialog
import kotlinx.coroutines.launch
import java.io.File

// --- Constants ---
private const val TAG = "StorageActivity"
const val TEXTFONTSIZE = 16
const val ROWBORDERRADIUS = 53
const val EDGEPADDING = 20
const val TOPPADDING = 10
const val ICONSIZE = 26

// --- Filters by tags or searches ---
private val nameFilters = mutableStateListOf<String>()

// --- item data ---
// Displays items based on what page the user is on.
var currentListIndex = mutableIntStateOf(0)
private var currentTabIndex = mutableIntStateOf(0)
private val pageItems = mutableStateListOf<RowItemUI>()
private val displayTabs = mutableStateListOf<DisplayTabItem>()

/**
 * The primary activity for displaying and managing a user's inventory across different storage locations.
 * This activity sets up the main Compose UI, which includes a tabbed layout for different storage
 * areas (e.g., "Fridge", "Cabinet"), a list of items within each tab, and functionality for
 * adding, editing, and deleting items. It also handles lifecycle events to persist data.
 */
class StashActivity : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge display for a more immersive UI.
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            // Decrease the count of the item based on what the user has set.
            LaunchedEffect(Unit) {
                decrementByDays(pageItems)
            }

            // This effect reloads the page's items and tabs whenever the currentPageIndex or the pg name changes.
            LaunchedEffect(
                currentListIndex.intValue, // For if the list is changed
                stashLists[currentListIndex.intValue].pgName, // For if the list is renamed
                stashLists.size // For if a list is removed
            ) {
                pageItems.clear()
                displayTabs.clear()
                pageItems.addAll(stashLists[currentListIndex.intValue].items.map { RowItemUI(it) })
                displayTabs.addAll(stashLists[currentListIndex.intValue].tabs.map {
                    DisplayTabItem(
                        it
                    )
                })
            }
            // This effect saves the data when the activity is paused.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        stashLists[currentListIndex.intValue].apply {
                            items.clear()
                            tabs.clear()
                            items.addAll(pageItems.map { it.toLocalRowItem() })
                            tabs.addAll(displayTabs.map { it.toTabItem() })
                        }
                        updateStoredValue(context, lifecycleScope, stashLists, STORAGEITEMPATH)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            AppTheme {
                // Scaffold provides a standard layout structure for Material Design apps.
                Scaffold(
                    bottomBar = {
                        BottomNavigation(activeTab = 1)
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TitleElements()
                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            // `Tabs` is the main composable for the screen's primary content.
                            Tabs(
                                allItems = pageItems,
                                tabs = displayTabs,
                                onNewTab = {
                                    displayTabs.add(
                                        DisplayTabItem(
                                            "Tab",
                                            displayTabs.size
                                        )
                                    )
                                },
                                onDeleteTab = { tab -> displayTabs.remove(tab) },
                                onEditTab = { tab -> tabToEdit.value = tab }
                            )
                            // This button provides a way for the user to add a new item.
                            NewRowItemBtn(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                onNewItem = {
                                    val newRowItemUI =
                                        RowItemUI(
                                            initialName = "New item",
                                            initialPgIndex = currentTabIndex.intValue
                                        )
                                    pageItems.add(newRowItemUI)
                                    itemToEdit.value = newRowItemUI
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Utility functions ---

/**
 * Filters a list of `RowItem`s based on a list of name filters.
 *
 * @param nameFilter A list of strings to filter by.
 * @param itemsToFilter The list of `RowItem`s to filter.
 * @return A new list of `RowItem`s that match the filter criteria.
 */
fun rowItemFilter(
    nameFilter: MutableList<String>,
    itemsToFilter: List<RowItemUI>
): List<RowItemUI> {
    if (nameFilter.isEmpty()) {
        return itemsToFilter
    }

    val filteredItems = mutableListOf<RowItemUI>()
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

/**
 * A button that creates a new `RowItem` and adds it to the list of all items.
 *
 * @param onNewItem A callback to be executed when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 */
@Composable
fun NewRowItemBtn(onNewItem: () -> Unit, modifier: Modifier) {
    Button(
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(5.dp),
        modifier = modifier
            .size(width = 90.dp, height = 90.dp)
            .padding(20.dp),
        onClick = onNewItem
    ) {
        Icon(
            painter = painterResource(R.drawable.plus),
            contentDescription = "Add new item"
        )
    }
}

/**
 * A composable that displays the screen's title, total item count, and icons for search and list management.
 * It delegates the handling of dialogs and popups to the `TitleElementsDialogs` composable.
 */
@Composable
fun TitleElements(
) {
    val totalItems = pageItems.size
    val currentPageIndex = currentListIndex.intValue

    Row(
        modifier = Modifier
            .padding(horizontal = EDGEPADDING.dp, vertical = TOPPADDING.dp)
            .fillMaxWidth()
    ) {
        Column {
            Text(
                text = stashLists[currentPageIndex].pgName,
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
        SearchIcon(onSearch = { query -> nameFilters.add(query) })
        Spacer(modifier = Modifier.width(10.dp))

        // Menu for the list
        ListOptions()
    }
}

@Composable
fun SearchIcon(onSearch: (String) -> Unit) {
    var showSearchBar by remember { mutableStateOf(false) }
    // Search button
    Icon(
        painter = painterResource(R.drawable.search_database),
        contentDescription = "search icon",
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { showSearchBar = true }
            }
            .size(ICONSIZE.dp)
    )
    if (showSearchBar) {
        SearchBarPopup(
            onDismiss = { showSearchBar = !showSearchBar },
            onSearch = { query ->
                onSearch(query)
                showSearchBar = !showSearchBar
            }
        )
    }
}

@Composable
fun ListOptions() {
    var showOptions by remember { mutableStateOf(false) }
    var showEditListDialog by remember { mutableStateOf(false) }
    var showSwitchListPopup by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopStart) {
        Icon(
            painter = painterResource(R.drawable.listmenu),
            contentDescription = "Tab icon",
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures { showOptions = !showOptions }
                }
                .size(ICONSIZE.dp)
        )
        DropdownMenu(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondary),
            expanded = showOptions,
            onDismissRequest = { showOptions = !showOptions }
        ) {
            DropdownMenuItem(
                text = { Text("Add list", color = MaterialTheme.colorScheme.onSecondary) },
                onClick = {
                    stashLists.add(StashList("New list"))
                    currentListIndex.intValue = (stashLists.size - 1)
                    showEditListDialog = true
                    showOptions = !showOptions
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Switch list",
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                },
                onClick = {
                    showSwitchListPopup = true
                    showOptions = !showOptions
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Edit list",
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                },
                onClick = {
                    showEditListDialog = true
                    showOptions = !showOptions
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Delete list",
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                },
                onClick = {
                    if (stashLists.size > 1) {
                        stashLists.removeAt(currentListIndex.intValue)
                        currentListIndex.intValue = 0
                    } else {
                        showAlert = true
                    }
                }
            )
        }
    }

    if (showAlert) {
        SimpleAlertDialog(
            title = "Error",
            message = "You must have at least one list.",
            onDismissRequest = { showAlert = !showAlert }
        )
    }

    if (showEditListDialog) {
        EditListDialogue(
            onDismiss = { showEditListDialog = !showEditListDialog },
            onSave = { newListName ->
                stashLists[currentListIndex.intValue].pgName = newListName
                showEditListDialog = !showEditListDialog
            }
        )
    }

    if (showSwitchListPopup) {
        val switchListButtons = stashLists.mapIndexed { index, stashList ->
            DropDownButton(text = stashList.pgName) {
                currentListIndex.intValue = index
                showSwitchListPopup = !showSwitchListPopup
            }
        }
        BottomPopUp(
            title = "Switch List",
            expanded = true,
            onDismissRequest = { showSwitchListPopup = !showSwitchListPopup },
            buttons = switchListButtons
        )
    }
}

/**
 * A popup dialog that allows the user to add a new list.
 *
 * @param onDismiss A callback to dismiss the dialog.
 * @param onSave A callback to be executed when the user saves the new list.
 */
@Composable
fun EditListDialogue(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var listName by remember { mutableStateOf(stashLists[currentListIndex.intValue].pgName) }

    AlertDialog(
        modifier = Modifier.padding(horizontal = 13.dp),
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit list") },
        text = {
            TextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("List name") },
                singleLine = true
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    onClick = {
                        onSave(listName)
                    }
                ) {
                    Text("Save")
                }
                Spacer(Modifier.width(15.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }

        }
    )
}

/**
 * A popup dialog that allows the user to enter a search query.
 *
 * @param onDismiss A callback to dismiss the dialog.
 * @param onSearch A callback to be executed when the user searches.
 */
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
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        onSearch(searchQuery)
                    }
                ) {
                    Text("Search")
                }
            }
        }
    )
}


/**
 * The core composable that organizes the UI using a tabbed layout. It manages the state
 * of the currently selected tab and uses a `HorizontalPager` to allow for swipeable
 * navigation between the different storage locations.
 *
 * @param modifier A `Modifier` passed from the parent.
 * @param allItems The observable list of all storage items, which will be filtered for each tab.
 * @param tabs The observable list of all tabs.
 * @param onNewTab A callback to be executed when a new tab is added.
 * @param onDeleteTab A callback to be executed when a tab is deleted.
 * @param onEditTab A callback to be executed when a tab is edited.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    allItems: SnapshotStateList<RowItemUI>,
    tabs: SnapshotStateList<DisplayTabItem>,
    onNewTab: () -> Unit,
    onDeleteTab: (DisplayTabItem) -> Unit,
    onEditTab: (DisplayTabItem) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        currentTabIndex.intValue = pagerState.currentPage
    }

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
                tabs.forEach { tab ->
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
                            BottomPopUp(
                                title = "Editing tab",
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                buttons = listOf(
                                    DropDownButton(
                                        text = "Delete",
                                        icon = R.drawable.outline_delete_24,
                                        onClick = {
                                            onDeleteTab(tab)
                                            showMenu = false
                                        }
                                    ),
                                    DropDownButton(
                                        text = "Edit",
                                        icon = R.drawable.outline_edit_24,
                                        onClick = {
                                            onEditTab(tab)
                                            showMenu = false
                                        }
                                    )
                                )
                            )
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
                    onClick = onNewTab,
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

/**
 * A composable that displays the current search filters as dismissible pills.
 */
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

/**
 * A dialog that allows the user to edit the name of a tab.
 *
 * @param tabToEdit The `DisplayTabItem` to be edited. If `null`, the dialog is not shown.
 * @param onDismiss A callback to dismiss the dialog.
 * @param onSave A callback to save the changes.
 */
@Composable
fun EditTabDialogue(tabToEdit: DisplayTabItem?, onDismiss: () -> Unit, onSave: () -> Unit) {

    if (tabToEdit == null) return
    var tabName by remember(tabToEdit.identifier) { mutableStateOf(tabToEdit.name) }

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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) { Text(text = "Save") }
                Spacer(Modifier.width(15.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
    itemsToShow: List<RowItemUI>,
    allItems: SnapshotStateList<RowItemUI>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Providing a unique `key` for each item helps Compose optimize recompositions,
        // especially when the list changes (items are added, removed, or reordered).
        items(
            items = itemsToShow,
            key = { item -> item.identifier }
        ) { item ->
            RowItemUI(
                rowItemUI = item,
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
 * @param rowItemUI The `RowItem` data to display.
 * @param onDelete A callback lambda to be executed when the user chooses to delete this item.
 * @param onEdit A callback lambda to set the globally observed `itemToEdit`, triggering the edit dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowItemUI(
    rowItemUI: RowItemUI,
    onDelete: () -> Unit,
    onEdit: (RowItemUI) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // `remember` with `rowItem.img` as a key ensures the image model is re-evaluated
    // only when the image path for this specific item changes.
    val imageModel = remember(rowItemUI.img) {
        // Handle both local file paths and drawable resource IDs.
        rowItemUI.img.toIntOrNull() ?: File(rowItemUI.img)
    }

    Box {
        Card(
            modifier = Modifier
                .padding(horizontal = (EDGEPADDING + 20).dp)
                .pointerInput(Unit) {
                    // Open the dropdown menu on a simple tap.
                    detectTapGestures(onTap = { showMenu = true })
                },
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Asynchronously load and display the item's image using Coil.
                Image(
                    painter = rememberAsyncImagePainter(model = imageModel),
                    contentDescription = rowItemUI.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier.widthIn(max = 135.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // `basicMarquee` provides a scrolling animation if the item name is too long.
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(),
                        textAlign = TextAlign.Left,
                        text = rowItemUI.name,
                        fontSize = (TEXTFONTSIZE + 1).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                // Row for plus and minus buttons
                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Buttons to increment and decrement the item count.
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
                        ),
                        onClick = { rowItemUI.increaseCount() },
                        modifier = Modifier.size(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(text = "+", color = MaterialTheme.colorScheme.onSecondaryContainer) }

                    Spacer(modifier = Modifier.width(7.dp))

                    // Only show unit if given one
                    var unitText = "${rowItemUI.count}"
                    if (rowItemUI.unit != "") {
                        unitText = "${rowItemUI.count} ${rowItemUI.unit}"
                    }

                    Text(
                        textAlign = TextAlign.Left,
                        text = unitText,
                        fontSize = TEXTFONTSIZE.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
                        ),
                        onClick = { rowItemUI.decreaseCount() },
                        modifier = Modifier.size(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(text = "-", color = MaterialTheme.colorScheme.onSecondaryContainer) }
                }
            }
        }

        // The dropdown menu is anchored to the `Box` and is only expanded when `showMenu` is true.
        BottomPopUp(
            title = "Editing item: ${rowItemUI.name}",
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            buttons = listOf(
                DropDownButton(
                    text = "Delete",
                    icon = R.drawable.outline_delete_24,
                    onClick = {
                        onDelete()
                        showMenu = false
                    }),
                DropDownButton(
                    text = "Edit",
                    icon = R.drawable.outline_edit_24,
                    onClick = {
                        onEdit(rowItemUI)
                        showMenu = false
                    }
                )
            )
        )
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
    itemToEdit: RowItemUI?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    if (itemToEdit == null) return

    val context = LocalContext.current
    // Using `remember` with `itemToEdit.identifier` as a key ensures that the dialog's state
    // resets correctly when a different item is selected for editing.
    var nameText by remember(itemToEdit.identifier) { mutableStateOf(itemToEdit.name) }
    var countText by remember(itemToEdit.identifier) { mutableStateOf(itemToEdit.count.toString()) }
    var unitText by remember(itemToEdit.identifier) { mutableStateOf(itemToEdit.unit) }
    var pgIndex by remember(itemToEdit.identifier) { mutableIntStateOf(itemToEdit.pgIndex) }
    var imgText by remember(itemToEdit.identifier) { mutableStateOf(itemToEdit.img) }
    var decrementText by remember(itemToEdit.identifier) { mutableStateOf(itemToEdit.decrement.toString()) }
    var decrementIntervalText by remember(itemToEdit.identifier) { mutableStateOf(itemToEdit.decrementInterval.toString()) }

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

    AlertDialog(
        modifier = Modifier.padding(horizontal = DIALOGPADDING.dp),
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
                Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
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
                        ) { Text(stashLists[currentListIndex.intValue].tabs[pgIndex].name) }
                        val dropDownBtns = mutableListOf<DropDownButton>()
                        stashLists[currentListIndex.intValue].tabs.forEach { tab ->
                            dropDownBtns.add(DropDownButton(text = tab.name, onClick = {
                                pgIndex = tab.index
                                expanded = false
                            }))
                        }
                        BottomPopUp(
                            title = "Editing: ${itemToEdit.name} tab",
                            expanded = expanded,
                            onDismissRequest = { expanded = false }, buttons = dropDownBtns
                        )
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
                        itemToEdit.decrementInterval =
                            decrementIntervalText.toIntOrNull() ?: 1
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
    val allItems = mutableStateListOf(RowItemUI("wasd"))

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
                TitleElements(
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    // `Tabs` is the main composable for the screen's primary content.
                    Tabs(
                        allItems = allItems,
                        tabs = tabs,
                        onNewTab = {},
                        onDeleteTab = {},
                        onEditTab = {})
                    // This button provides a way for the user to add a new item.
                    NewRowItemBtn(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        onNewItem = {}
                    )
                }
            }
        }
    }
}