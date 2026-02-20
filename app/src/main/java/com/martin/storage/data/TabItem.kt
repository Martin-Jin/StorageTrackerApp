package com.martin.storage.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

/**
 * A serializable data class representing a tab as it is stored in persistent storage.
 * This class is designed for data transfer and persistence.
 */
@Serializable
data class TabItem(val name: String = "New tab", val index: Int)

/**
 * A stateful class representing a single tab within the Compose UI.
 * It uses Compose's `State` delegates to allow the UI to automatically
 * recompose when its properties change.
 *
 * @param initialName The starting name for the tab.
 * @param initialIndex The index of the tab.
 */
class DisplayTabItem(
    initialName: String = "New tab",
    initialIndex: Int
) {
    companion object {
        var tabToEdit = mutableStateOf<DisplayTabItem?>(null)
    }
    /**
     * Secondary constructor to create a UI-layer `DisplayTabItem` directly from a data-layer `TabItem`.
     * This simplifies the conversion from persisted data to UI state.
     */
    constructor(tabItem: TabItem) : this(
        initialName = tabItem.name,
        initialIndex = tabItem.index
    )

    var name by mutableStateOf(initialName)
    var index by mutableIntStateOf(initialIndex)

    /**
     * Converts this UI-layer `DisplayTabItem` into a data-layer `TabItem` for persistence.
     * @return A `TabItem` instance with the current data, ready for serialization.
     */
    fun toTabItem(): TabItem {
        return TabItem(
            name = this.name,
            index = this.index
        )
    }
}
