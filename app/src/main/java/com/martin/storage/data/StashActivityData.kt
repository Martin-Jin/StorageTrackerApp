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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.martin.storage.customUI.RowItem
import com.martin.storage.customUI.RowItemUI
import com.martin.storage.customUI.TabItem
import com.martin.storage.customUI.TabItemUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
