/**
 * This file serves as the central hub for all local data persistence in the application.
 * It uses Jetpack DataStore for storing key-value pairs and Kotlinx Serialization for converting
 * complex objects into JSON strings. This provides a robust and efficient way to handle app data,
 * from simple preferences to lists of custom objects.
 *
 * The file is structured as follows:
 * 1.  **Constants and Global Variables**: Defines keys for DataStore, a TAG for logging, a shared
 *     JSON serializer, and an in-memory cache for frequently accessed data.
 * 2.  **High-Level Data Functions**: These are the primary functions for data operations, providing
 *     a simple, type-safe API for writing, appending, and reading serializable objects.
 * 3.  **Low-Level DataStore Functions**: These are generic helpers that interact directly with
 *     DataStore to read and write raw string data.
 * 4.  **Business Logic Functions**: Contains functions specific to the application's data model,
 *     such as image processing.
 */
package com.martin.storage.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martin.storage.customUI.RowItem
import com.martin.storage.customUI.RowItemUI
import com.martin.storage.customUI.TabItem
import com.martin.storage.customUI.TabItemUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

// --- Constants and Global Variables ---

/**
 * A constant TAG used for logging to easily filter data management-related events in Logcat.
 */
const val TAG = "StashData"

/**
 * The preference key for storing the main list of storage items in Jetpack DataStore.
 */
const val STORAGEITEMPATH = "storageItems"

/**
 * The preference key for storing the current user's unique ID (UID) in DataStore.
 */
const val UIDLOCALPATH = "UID"

@Serializable
data class StashList(
    var listName: String = "New list",
    val items: MutableList<RowItem> = mutableListOf(),
    val tabs: MutableList<TabItem> = mutableListOf(TabItem("Tab1", 0))
)

class StashListUI(
    var listName: MutableState<String> = mutableStateOf("String"),
    var items: SnapshotStateList<RowItemUI> = mutableStateListOf(),
    var tabs:  SnapshotStateList<TabItemUI> = mutableStateListOf(TabItemUI("Tab1", 0))
) {
    constructor(stashList: StashList) : this(
        listName = mutableStateOf(stashList.listName ),
        items = stashList.items.map { RowItemUI(it) }.toMutableStateList(),
        tabs = stashList.tabs.map { TabItemUI(it) }.toMutableStateList(),
    )

    /**
     * Converts this UI-layer `RowItem` into a data-layer `LocalRowItem` to be persisted.
     * @return A `UIRowItem` instance containing the current data, ready for serialization.
     */
    fun toStashList(): StashList {
        return StashList(
            listName = listName.value,
            items = items.map { it.toRowItemUI() } as MutableList<RowItem>,
            tabs = tabs.map { it.toTabItem() } as MutableList<TabItem>
        )
    }
}

/**
 * A shared, configured instance of the Kotlinx JSON serializer.
 * `encodeDefaults = true` ensures that properties with default values are included in the output JSON.
 * `ignoreUnknownKeys = true` provides forward compatibility if the data model changes.
 */
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
@Composable
fun FilterPopup(
    availableTags: List<String>,
    onApplyFilter: (tag: String?, ascending: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("tags") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var ascending by remember { mutableStateOf(true) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var showTagDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Items") },
        text = {
            Column {

                // Filter type dropdown
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filter by: ")
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Button(onClick = { showFilterDropdown = !showFilterDropdown }) {
                            Text(selectedFilter.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                                Locale.ROOT) else it.toString() })
                        }
                        DropdownMenu(
                            expanded = showFilterDropdown,
                            onDismissRequest = { showFilterDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tags") },
                                onClick = { selectedFilter = "tags"; showFilterDropdown = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Date") },
                                onClick = { selectedFilter = "date"; showFilterDropdown = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // If "Date" is selected, show ascending/descending buttons
                if (selectedFilter == "date") {
                    Text("Sort Order")
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Button(
                            onClick = { ascending = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ascending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ascending", color = if (ascending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { ascending = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!ascending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Descending", color = if (!ascending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Tag selection dropdown
                if (selectedFilter == "tags") {
                    Box {
                        Button(onClick = { showTagDropdown = !showTagDropdown }) {
                            Text(selectedTag ?: "Select tag")
                        }
                        DropdownMenu(
                            expanded = showTagDropdown,
                            onDismissRequest = { showTagDropdown = false }
                        ) {
                            availableTags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = { selectedTag = tag; showTagDropdown = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Button(onClick = {
                    onApplyFilter(
                        if (selectedFilter == "tags") selectedTag else null,
                        ascending
                    )
                    onDismiss()
                }) {
                    Text("Apply")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
