package com.martin.storage

import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.data.RowItem
import com.martin.storage.data.saveStorageItem
import com.martin.storage.data.storageItems
import com.martin.storage.ui.theme.TestTheme

// Variables for sizes
const val imgWidth = 0.2f

// Storage activity
class StorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TopTab()
                    }
                }
            }
        }
    }
}

// Storage preview
@Preview(showSystemUi = true)
@Composable
fun StoragePreview() {
    TestTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                TopTab()
            }
        }
    }
}

// Top tab
@Composable
fun TopTab(modifier: Modifier = Modifier) {

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fridge", "Cabinet", "Others")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val items = remember { storageItems }
    Column(modifier.fillMaxSize()) {

        SecondaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> Title("Fridge")
            1 -> Title("Cabinet")
            2 -> Title("Others")
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DisplayRows(storageItems[0])
            AddButton(onClick = {
                val newRowItem = RowItem(pgIndex = selectedTab)
                items[0].add(newRowItem)
                saveStorageItem(context, scope, newRowItem)
            })
        }
    }
}

// Displays rows of the current section
@Composable
fun DisplayRows(
    list: MutableList<RowItem>
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Each time a new row is composed, one object is used for displayed and another for
        // storage / json conversion is used. 
        items(list) { item ->
            ItemRow(rowItem = item, onDelete = { list.remove(item) })
        }
    }
}

// Button to create new rows
@Composable
fun AddButton(onClick: () -> Unit) {
    Button(modifier = Modifier.padding(16.dp), onClick = onClick) {
        Text(text = "Add item", fontSize = 15.sp)
    }
}

// A single row that displays an item
@Composable
fun ItemRow(
    rowItem: RowItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showEditDialog) {
        EditItemDialog(
            displayItem = rowItem,
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showEditDialog = false
            },
            onSave = { name, count, displayItem ->
                displayItem.name = name
                displayItem.count = count.toInt()
                saveStorageItem(context, scope, displayItem)
                @Suppress("AssignedValueIsNeverRead")
                showEditDialog = false
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
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
            Image(
                modifier = Modifier.weight(imgWidth),
                painter = painterResource(id = rowItem.img),
                contentDescription = rowItem.name,
                contentScale = ContentScale.Crop
            )
            Row {
                // Item name
                Text(
                    text = "${rowItem.name}: ",
                    fontSize = 20.sp,
                    lineHeight = 50.sp,
                    textAlign = TextAlign.Center,
                )
                // Num of items
                Text(
                    text = "${rowItem.count}",
                    fontSize = 20.sp,
                    lineHeight = 50.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Row {
                // Add button
                Button(
                    onClick = {
                        rowItem.increaseCount()
                        saveStorageItem(context, scope, rowItem)
                    }
                ) {
                    Text(text = "+", fontSize = 15.sp)
                }
                // Decrease button
                Button(
                    onClick = {
                        rowItem.decreaseCount()
                        saveStorageItem(context, scope, rowItem)
                    }
                ) {
                    Text(text = "-", fontSize = 15.sp)
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
                    @Suppress("AssignedValueIsNeverRead")
                    showEditDialog = true
                    showMenu = false
                }
            )
        }
    }
}

// Display row used to change UI
// item to edit is a temporary object used to store the edited data as json locally
@Composable
fun EditItemDialog(
    displayItem: RowItem,
    onDismiss: () -> Unit,
    onSave: (String, String, RowItem) -> Unit
) {
    var nameText by remember { mutableStateOf(displayItem.name) }
    var countText by remember { mutableStateOf(displayItem.count.toString()) }

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
                onClick = { onSave(nameText, countText, displayItem) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
