package com.martin.storage

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.rememberAsyncImagePainter
import com.martin.storage.data.LAST_OPENED_KEY
import com.martin.storage.data.RowItem
import com.martin.storage.data.RowItem.Companion.itemToEdit
import com.martin.storage.data.readLocalData
import com.martin.storage.data.saveImageFromUri
import com.martin.storage.data.storageItems
import com.martin.storage.data.updateStoredItems
import com.martin.storage.data.writeLocalData
import com.martin.storage.ui.theme.AppTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

// --- Constants ---
private const val TAG = "StorageActivity"
const val ITEMFONTSIZE = 17
const val ROWBORDERRADIUS = 20

/**
 * The main activity for displaying and managing storage items.
 * It sets up the Compose UI and initializes the list of items to be displayed.
 */
class StorageActivity : ComponentActivity() {
    private val displayItems = mutableStateListOf<RowItem>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- State Initialization ---
        // `mutableStateListOf` creates an observable list. Any composable that reads this list
        // will automatically recompose when items are added or removed.
        // The global `storageItems` list (from DataManagement.kt) is loaded in MainActivity.
        // We convert the `LocalRowItem` objects to `RowItem` objects suitable for the UI.
        displayItems.addAll(storageItems.map { it.toRowItem() })

        Log.d(TAG, "onCreate: Displaying ${displayItems.size} items from local storage.")

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val last = readLocalData(this@StorageActivity, LAST_OPENED_KEY).firstOrNull()
                val now = System.currentTimeMillis().toString()
                if (last != null) {
                    for (item in displayItems) {
                        item.updateDecrement(last = last, now = now)
                    }
                } else {
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
                var activeBottomTab by remember { mutableIntStateOf(1) }
                val context = LocalContext.current
                val bottomTabs = listOf(
                    { context.startActivity(Intent(context, MainActivity::class.java)) },
                    { context.startActivity(Intent(context, StorageActivity::class.java)) },
                    { context.startActivity(Intent(context, StorageActivity::class.java)) }
                )
                val tabIcons = listOf(R.drawable.homeicon, R.drawable.storage, R.drawable.options)
                Scaffold(
                    bottomBar = {
                        NavigationBar(modifier = Modifier.height(60.dp)) {
                            bottomTabs.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(tabIcons[index]),
                                            contentDescription = "icon"
                                        )
                                    },
                                    selected = activeBottomTab == index,
                                    onClick = {
                                        item()
                                    }
                                )
                            }

                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // The TopTab composable is the main UI for this screen.
                        Box {
                            Tabs(allItems = displayItems)
                            // The button to add a new item.
                            Button(
                                shape = RoundedCornerShape(15.dp),
                                contentPadding = PaddingValues(5.dp),
                                modifier = Modifier
                                    .size(width = 90.dp, height = 90.dp)
                                    .padding(20.dp)
                                    .align(Alignment.BottomEnd), onClick = {
                                    val newRowItem = RowItem(initialName = "New item")
                                    displayItems.add(newRowItem)
                                    itemToEdit.value = newRowItem
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
    }

    override fun onPause() {
        super.onPause()
        updateStoredItems(this, lifecycleScope, displayItems)
    }
}

// --- Main UI Composable ---
/**
 * The main composable that organizes the UI with a tabbed layout.
 * It manages the currently selected tab and displays the corresponding items.
 * @param allItems The observable list of items to display and manage.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    allItems: SnapshotStateList<RowItem>,
) {
    val tabs = listOf("Fridge", "Cabinet", "Others")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    val coroutineScope = rememberCoroutineScope()

    EditItemDialog(
        itemToEdit = itemToEdit.value,
        onDismiss = { itemToEdit.value = null },
        onSave = {
            itemToEdit.value = null
        }
    )

    Column(modifier.fillMaxSize()) {
        // The TabRow UI component.
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = { },
            tabs = {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                    )
                }
                Button(
                    modifier = Modifier
                        .height(15.dp)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(ROWBORDERRADIUS.dp)),
                    contentPadding = PaddingValues(5.dp),
                    onClick = {},
                )
                {
                    Text(
                        text = "New tab",
                    )
                }

            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // HorizontalPager for swipeable screens
        HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
            val itemsToShow by remember { derivedStateOf { allItems.filter { it.pgIndex == page } } }
            StorageScreen(
                allItems = allItems,
                itemsToShow = itemsToShow,
            )
        }
    }
}

@Composable
fun StorageScreen(
    itemsToShow: List<RowItem>,
    allItems: SnapshotStateList<RowItem>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // By providing a key, we help Compose understand which items are which, improving performance.
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
 * A single row in the list, representing one storage item.
 * @param rowItem The item to display.
 * @param onDelete A callback to execute when this item should be deleted.
 * @param onEdit A callback to set the item to be edited.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    rowItem: RowItem,
    onDelete: () -> Unit,
    onEdit: (RowItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val imageModel = remember(rowItem.img) {
        rowItem.img.toIntOrNull() ?: File(rowItem.img)
    }

    Box {
        Card(
            shape = RoundedCornerShape(ROWBORDERRADIUS.dp),
            modifier = Modifier
                .heightIn(0.dp, 70.dp)
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 25.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showMenu = true
                        }
                    )
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Text(
                        text = rowItem.name,
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
 * A dialog for editing the name and count of an item.
 * @param itemToEdit The item being edited.
 * @param onDismiss Callback for when the dialog is dismissed.
 * @param onSave Callback for when the user confirms their edits.
 */
@Composable
fun EditItemDialog(
    itemToEdit: RowItem?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    if (itemToEdit == null) {
        return
    }
    val context = LocalContext.current
    var nameText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.name) }
    var countText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.count.toString()) }
    var unitText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.unit) }
    var pgIndex by remember(itemToEdit.id) { mutableIntStateOf(itemToEdit.pgIndex) }
    var imgText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.img) }
    var decrementText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.decrement.toString()) }
    var decrementIntervalText by remember(itemToEdit.id) { mutableStateOf(itemToEdit.decrementInterval.toString()) }

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Button(
                    onClick = {
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

@Preview(showBackground = true)
@Composable
fun StorageScreenPreview() {
    AppTheme {
        var activeBottomTab by remember { mutableIntStateOf(1) }
        val context = LocalContext.current
        val bottomTabs = listOf(
            { context.startActivity(Intent(context, MainActivity::class.java)) },
            { context.startActivity(Intent(context, StorageActivity::class.java)) },
            { context.startActivity(Intent(context, StorageActivity::class.java)) }
        )
        val tabIcons = listOf(R.drawable.homeicon, R.drawable.storage, R.drawable.options)
        Scaffold(
            bottomBar = {
                NavigationBar(modifier = Modifier.height(60.dp)) {
                    bottomTabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(tabIcons[index]),
                                    contentDescription = "icon"
                                )
                            },
                            selected = activeBottomTab == index,
                            onClick = {
                                item()
                            }
                        )
                    }

                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->

            Column(modifier = Modifier.padding(innerPadding)) {
                Box {
                    // The TopTab composable is the main UI for this screen.
                    @SuppressLint("UnrememberedMutableState")
                    Tabs(allItems = mutableStateListOf(RowItem()))

                    Button(
                        shape = RoundedCornerShape(15.dp),
                        contentPadding = PaddingValues(5.dp),
                        modifier = Modifier
                            .size(width = 90.dp, height = 90.dp)
                            .padding(20.dp)
                            .align(Alignment.BottomEnd), onClick = {
                            val newRowItem = RowItem(initialName = "New item")
                            itemToEdit.value = newRowItem
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
