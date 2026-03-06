package com.martin.storage

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.martin.storage.customUI.RowItemUI
import com.martin.storage.customUI.RowItemUI.Companion.itemToEdit
import com.martin.storage.customUI.TabItemUI
import com.martin.storage.customUI.TabItemUI.Companion.tabToEdit
import com.martin.storage.data.LoadAndCache
import com.martin.storage.data.STORAGEITEMPATH
import com.martin.storage.data.StashList
import com.martin.storage.data.StashListUI
import com.martin.storage.data.saveImageFromUri
import com.martin.storage.data.updateStoredValue
import com.martin.storage.ui.theme.AppTheme
import com.martin.storage.ui.theme.BottomNavigation
import com.martin.storage.ui.theme.BottomPopUp
import com.martin.storage.ui.theme.DropDownButton
import com.martin.storage.ui.theme.EDGEPADDING
import com.martin.storage.ui.theme.SimpleAlertDialog
import com.martin.storage.ui.theme.TEXTFONTSIZE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// --- Constants ---
private const val TAG = "StashActivity"
private const val ICONSIZE = 26

// --- Filters by tags or searches ---
private val nameFilters = mutableStateListOf<String>()

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var readData = remember { true }
            val stashLists = remember {
                mutableStateListOf(StashListUI(mutableStateOf("List")))
            }

            // Load and cache users lists.
            LoadAndCache<StashList>(path = STORAGEITEMPATH) { data ->
                if (readData) {
                    Log.d(
                        TAG,
                        "Successfully read ${data.size} items from $STORAGEITEMPATH. Updating in-memory cache."
                    )
                    val readList = data.map { StashListUI(it) }.toMutableStateList()
                    stashLists.removeAll { true }
                    stashLists.addAll(readList)
                    readData = false
                }
            }

            val currentListIndex = remember { mutableIntStateOf(0) }
            val currentTabIndex = remember { mutableIntStateOf(0) }
            val context = LocalContext.current
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            /**
             * TODO: Add decrement amount overtime
             * TODO: Update edit menu
             */

            // This effect saves the data when the activity is paused.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        val listsToSave = stashLists.map { it.toStashList() }
                        Log.d(
                            TAG,
                            "Data saved to $STORAGEITEMPATH as ${stashLists[currentListIndex.intValue].items}"
                        )
                        updateStoredValue(
                            context,
                            lifecycleScope,
                            listsToSave.toMutableList(),
                            STORAGEITEMPATH
                        )
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Menu for editing row items
            var showEditMenu by remember { mutableStateOf(false) }

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
                            stashLists[currentListIndex.intValue].items.size,
                            stashLists[currentListIndex.intValue].listName.value,
                            currentListIndex,
                            stashLists
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            // `Tabs` is the main composable for the screen's primary content.
                            Tabs(
                                currentListItems = stashLists[currentListIndex.intValue].items,
                                tabs = stashLists[currentListIndex.intValue].tabs,
                                onNewTab = {
                                    stashLists[currentListIndex.intValue].tabs.add(
                                        TabItemUI(
                                            "Tab",
                                            stashLists[currentListIndex.intValue].tabs.size
                                        )
                                    )
                                },
                                onSwitchTab = { currentTabIndex.intValue = it },
                                onDeleteTab = { tab ->
                                    stashLists[currentListIndex.intValue].tabs.remove(
                                        tab
                                    )
                                },
                                onEditTab = { tab -> tabToEdit.value = tab },
                                showEditMenu = showEditMenu,
                                onDismissEditMenu = { showEditMenu = false },
                                currentListIndex = currentListIndex.intValue,
                                stashLists = stashLists
                            )
                            // This button provides a way for the user to add a new item.
                            NewRowItemBtn(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                onClick = {
                                    showEditMenu = true

                                    val newRowItemUI =
                                        RowItemUI(
                                            initialName = "New item",
                                            initialTabIndex = currentTabIndex.intValue
                                        )
                                    stashLists[currentListIndex.intValue].items.add(newRowItemUI)
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

    // Use the .filter{} and .all{} collection functions for a more
    // concise and correct implementation.
    return itemsToFilter.filter { item ->
        nameFilter.all { filter ->
            item.name.contains(filter, ignoreCase = true)
        }
    }
}

// --- Main UI Composable ---

/**
 * A button that creates a new `RowItem` and adds it to the list of all items.
 * @param modifier The modifier to be applied to the button.
 */
@Composable
fun NewRowItemBtn(modifier: Modifier, onClick: () -> Unit = {}) {
    Button(
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(5.dp),
        modifier = modifier
            .size(width = 90.dp, height = 90.dp)
            .padding(20.dp),
        onClick = onClick
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
    totalItems: Int,
    listName: String,
    currentListIndex: MutableIntState,
    stashLists: SnapshotStateList<StashListUI>
) {
    Row(
        modifier = Modifier
            .padding(horizontal = EDGEPADDING.dp, vertical = 10.dp)
            .fillMaxWidth()
    ) {
        Column {
            Text(
                text = listName,
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
        ListOptions(currentListIndex = currentListIndex, stashLists = stashLists)
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
            .clickable {
                showSearchBar = true
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
fun ListOptions(currentListIndex: MutableIntState, stashLists: SnapshotStateList<StashListUI>) {
    var showOptions by remember { mutableStateOf(false) }
    var showEditListDialog by remember { mutableStateOf(false) }
    var showSwitchListPopup by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopStart) {
        Icon(
            painter = painterResource(R.drawable.listmenu),
            contentDescription = "Tab icon",
            modifier = Modifier
                .clickable {
                    showOptions = !showOptions
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
                    stashLists.add(StashListUI(listName = mutableStateOf("New list")))
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
                        showOptions = !showOptions
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
                stashLists[currentListIndex.intValue].listName.value = newListName
                showEditListDialog = !showEditListDialog
            },
            currentListIndex = currentListIndex.intValue, stashLists = stashLists
        )
    }

    if (showSwitchListPopup) {
        val switchListButtons = stashLists.mapIndexed { index, stashList ->
            DropDownButton(text = stashList.listName.value) {
                currentListIndex.intValue = index
                showSwitchListPopup = !showSwitchListPopup
            }
        }
        BottomPopUp(
            title = "Switch List",
            expanded = true,
            onDismissRequest = { showSwitchListPopup = !showSwitchListPopup },
            buttons = switchListButtons,
            fullScreen = false
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
fun EditListDialogue(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    currentListIndex: Int,
    stashLists: SnapshotStateList<StashListUI>
) {
    var listName by remember { mutableStateOf(stashLists[currentListIndex].listName.value) }

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
 * @param onDismiss A callback to dismiss the dialog.
 * @param onSearch A callback to be executed when the user searches.
 */
@Composable
fun SearchBarPopup(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {

    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        title = null,
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(14.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search items...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                )
            }
        },

        confirmButton = {

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        onSearch(searchQuery)
                        onDismiss()
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
 * @param currentListItems The observable list of all storage items, which will be filtered for each tab.
 * @param tabs The observable list of all tabs.
 * @param onNewTab A callback to be executed when a new tab is added.
 * @param onDeleteTab A callback to be executed when a tab is deleted.
 * @param onEditTab A callback to be executed when a tab is edited.
 * @param showEditMenu Whether to show the row edit menu.
 * @param onDismissEditMenu A callback to be executed when row editing is finished.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    stashLists: SnapshotStateList<StashListUI>,
    currentListItems: SnapshotStateList<RowItemUI>,
    tabs: SnapshotStateList<TabItemUI>,
    currentListIndex: Int,
    onSwitchTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onDeleteTab: (TabItemUI) -> Unit,
    onEditTab: (TabItemUI) -> Unit,
    showEditMenu: Boolean = false,
    onDismissEditMenu: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    LaunchedEffect(tabs.size) {
        if (pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage(maxOf(0, tabs.lastIndex))
        }
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onSwitchTab(pagerState.currentPage)
    }

    // The dialog for editing a tab is defined here but only shown when `tabToEdit.value` is not null.
    EditTabDialogue(
        tabToEdit.value,
        onDismiss = { tabToEdit.value = null },
        onSave = { itemToEdit.value = null }
    )

    Column(modifier.fillMaxSize()) {
        // The `SecondaryScrollableTabRow` provides the tab navigation UI.
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = { },
            tabs = {
                tabs.forEach { tab ->
                    key(tab.identifier) {
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
                                    ),
                                    fullScreen = false
                                )
                            },
                        )
                    }
                }
                // A button for adding a new tab category in the future.
                Button(
                    modifier = Modifier
                        .heightIn(max = 15.dp)
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(53.dp)),
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

            val filteredItems by remember(currentListItems, nameFilters, page) {
                derivedStateOf {
                    currentListItems
                        .asSequence()
                        .filter { it.tabIndex == page }
                        .filter { rowItemFilter(nameFilters, listOf(it)).isNotEmpty() }
                        .toList()
                }
            }
            StorageScreen(
                currentListItems = currentListItems,
                itemsToShow = filteredItems,
                showEditMenu = showEditMenu,
                onDismissEditMenu = onDismissEditMenu,
                currentListIndex = currentListIndex,
                stashLists = stashLists
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
fun EditTabDialogue(tabToEdit: TabItemUI?, onDismiss: () -> Unit, onSave: () -> Unit) {

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
                        onDismiss()
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
 * @param currentListItems The complete list of all items, passed down to be modified by child composables.
 * @param showEditMenu Triggers the menu from external composable functions, such as the add row item button.
 * @param onDismissEditMenu Dismiss the menu from external composables.
 */
@Composable
fun StorageScreen(
    stashLists: SnapshotStateList<StashListUI>,
    itemsToShow: List<RowItemUI>,
    currentListItems: SnapshotStateList<RowItemUI>,
    showEditMenu: Boolean = false,
    onDismissEditMenu: () -> Unit,
    currentListIndex: Int,
) {
    val editRow = remember { mutableStateOf(false) }
    val showEditor = remember(showEditMenu, editRow.value) {
        showEditMenu || editRow.value
    }

    if (showEditor) {
        val item = itemToEdit.value ?: return
        RowItemFullEditor(
            currentListItems = currentListItems,
            item = item,
            expanded = true,
            onDismiss = {
                itemToEdit.value = null
                editRow.value = false
                onDismissEditMenu()
            },
            currentListIndex = currentListIndex,
            stashLists = stashLists
        )
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0
    )
    key(currentListIndex) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState
        ) {
            // Providing a unique `key` for each item helps Compose optimize recompositions,
            // especially when the list changes (items are added, removed, or reordered).
            items(
                items = itemsToShow,
                key = { item -> item.identifier }
            ) { item ->
                RowItemUI(
                    name = item.name,
                    count = item.count,
                    unit = item.unit,
                    img = item.img,
                    onIncrease = { item.increaseCount() },
                    onDecrease = { item.decreaseCount() },
                    onEdit = {
                        itemToEdit.value = item
                        editRow.value = true
                    }
                )
            }
        }
    }
}

/**
 * A composable representing a single row in the storage item list. It displays the item's
 * image, name, and quantity, and provides controls for modifying the count and accessing
 * a dropdown menu for editing or deleting the item.
 *
 * @param name The name of the item to display.
 * @param count The count of the item to display.
 * @param unit The unit of the item to display.
 * @param img The image of the item to display.
 * @param onIncrease A callback lambda to be executed when the user chooses to increase the count of this item.
 * @param onDecrease A callback lambda to be executed when the user chooses to decrease the count of this item.
 * @param onEdit A callback lambda to set the globally observed `itemToEdit`, triggering the edit dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowItemUI(
    name: String,
    count: Int,
    unit: String,
    img: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onEdit: () -> Unit
) {
    // `remember` with `rowItem.img` as a key ensures the image model is re-evaluated
    // only when the image path for this specific item changes.
    val imageModel = remember(img) {
        // Handle both local file paths and drawable resource IDs.
        img.toIntOrNull() ?: File(img)
    }
    Box {
        Card(
            modifier = Modifier
                .padding(horizontal = EDGEPADDING.dp)
                .clickable {
                    // Open the dropdown menu on a simple tap.
                    onEdit()
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageModel)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // `basicMarquee` provides a scrolling animation if the item name is too long.
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(),
                        textAlign = TextAlign.Left,
                        text = name,
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
                        onClick = { onIncrease() },
                        modifier = Modifier.size(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(text = "+", color = MaterialTheme.colorScheme.onSecondaryContainer) }

                    Spacer(modifier = Modifier.width(7.dp))

                    // Only show unit if given one
                    var unitText = "$count"
                    if (unit != "") {
                        unitText = "$count $unit"
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
                        onClick = onDecrease,
                        modifier = Modifier.size(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(text = "-", color = MaterialTheme.colorScheme.onSecondaryContainer) }
                }
            }
        }
    }
}

@Stable
class RowItemEditorState(
    item: RowItemUI,
    currentListIndex: Int
) {
    var nameText by mutableStateOf(item.name)
    var countText by mutableStateOf(item.count.toString())
    var unitText by mutableStateOf(item.unit)

    var tabIndex by mutableIntStateOf(item.tabIndex)
    var listIndex by mutableIntStateOf(currentListIndex)

    var imgPath by mutableStateOf(item.img)

    var tabExpanded by mutableStateOf(false)
    var listExpanded by mutableStateOf(false)
}


@Composable
fun rememberRowItemEditorState(
    item: RowItemUI,
    currentListIndex: Int
): RowItemEditorState {
    return remember(item.identifier) {
        RowItemEditorState(item, currentListIndex)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowItemFullEditor(
    stashLists: SnapshotStateList<StashListUI>,
    currentListItems: SnapshotStateList<RowItemUI>,
    item: RowItemUI,
    expanded: Boolean,
    onDismiss: () -> Unit,
    currentListIndex: Int
) {

    val context = LocalContext.current
    val editorState = rememberRowItemEditorState(item, currentListIndex)

    // Freeze UI data to prevent SnapshotStateList recompositions
    val tabs = remember(currentListIndex) {
        stashLists[currentListIndex].tabs.toList()
    }

    val listNames by remember {
        derivedStateOf {
            stashLists.map { it.listName.value }
        }
    }

    val imageModel = remember(editorState.imgPath) {
        editorState.imgPath.toIntOrNull() ?: File(editorState.imgPath)
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        saveImageFromUri(context, uri)?.let {
            editorState.imgPath = it
        }
    }

    BottomPopUp(
        title = "",
        expanded = expanded,
        onDismissRequest = onDismiss,
        fullScreen = true
    ) {
        val focusRequester = remember { FocusRequester() }
        var isTitleFocused by remember { mutableStateOf(false) }
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            TextField(
                value = editorState.nameText,
                onValueChange = { editorState.nameText = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isTitleFocused = it.isFocused
                    },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )

            LaunchedEffect(Unit) {
                delay(50)
                focusRequester.requestFocus()
            }
        }

        EditorLayout(
            editorState = editorState,
            imageModel = imageModel,
            tabs = tabs,
            listNames = listNames,
            onPickImage = { imagePicker.launch("image/*") },
            onSave = {

                val count = editorState.countText.toIntOrNull() ?: 0

                item.name = editorState.nameText
                item.count = count
                item.unit = editorState.unitText
                item.tabIndex = editorState.tabIndex
                item.img = editorState.imgPath

                if (editorState.listIndex != currentListIndex) {
                    currentListItems.remove(item)
                    stashLists[editorState.listIndex].items.add(item)
                }

                onDismiss()
            },
            onDelete = {
                stashLists[currentListIndex].items.remove(itemToEdit.value)
                onDismiss()
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditorLayout(
    editorState: RowItemEditorState,
    imageModel: Any,
    tabs: List<TabItemUI>,
    listNames: List<String>,
    onPickImage: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageModel)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { onPickImage() }
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = editorState.countText,
                onValueChange = { editorState.countText = it },
                label = { Text("Count") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = editorState.unitText,
                onValueChange = { editorState.unitText = it },
                label = { Text("Unit") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        TabDropdown(
            tabs = tabs,
            selectedTabIndex = editorState.tabIndex,
            expanded = editorState.tabExpanded,
            onExpandedChange = { editorState.tabExpanded = it },
            onTabSelected = { editorState.tabIndex = it }
        )

        Spacer(Modifier.height(20.dp))

        ListDropdown(
            listNames = listNames,
            selectedIndex = editorState.listIndex,
            expanded = editorState.listExpanded,
            onExpandedChange = { editorState.listExpanded = it },
            onSelected = { editorState.listIndex = it }
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabDropdown(
    tabs: List<TabItemUI>,
    selectedTabIndex: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTabSelected: (Int) -> Unit
) {

    val selectedName = remember(selectedTabIndex, tabs) {
        tabs.firstOrNull { it.index == selectedTabIndex }?.name ?: ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {

        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tab") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {

            tabs.forEach { tab ->
                DropdownMenuItem(
                    text = { Text(tab.name) },
                    onClick = {
                        onTabSelected(tab.index)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDropdown(
    listNames: List<String>,
    selectedIndex: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (Int) -> Unit
) {

    val selectedName = listNames.getOrElse(selectedIndex) { "" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {

        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Move to list") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {

            listNames.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(index)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}



// --- Helper Composable & Previews ---
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
    val stashLists = mutableStateListOf(StashListUI(mutableStateOf("List1")))

    @SuppressLint("UnrememberedMutableState")
    val tabs = mutableStateListOf(TabItemUI("Tab1", 0))
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
                    allItems.size,
                    "Test List",
                    remember { mutableIntStateOf(0) },
                    stashLists
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    // `Tabs` is the main composable for the screen's primary content.
                    Tabs(
                        currentListItems = allItems,
                        tabs = tabs,
                        onNewTab = {},
                        onDeleteTab = {},
                        onEditTab = {},
                        onSwitchTab = {},
                        currentListIndex = 0,
                        stashLists = stashLists
                    ) {}
                    // This button provides a way for the user to add a new item.
                    NewRowItemBtn(
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}