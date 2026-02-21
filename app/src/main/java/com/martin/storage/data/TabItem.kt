package com.martin.storage.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

/**
 * A serializable data class that represents a tab in its persistent state.
 * This class is used for saving and loading tab data from sources like DataStore or Firebase.
 *
 * @property name The name of the tab (e.g., "Fridge", "Pantry").
 * @property index The position of the tab in the tab row.
 */
@Serializable
data class TabItem(val name: String = "New tab", val index: Int)

/**
 * A stateful class that represents a single tab in the Compose UI.
 * It uses Compose's `State` delegates to make its properties observable, allowing the UI to
 * automatically recompose when the tab's name or index changes.
 *
 * @param initialName The initial name for the tab.
 * @param initialIndex The initial index of the tab.
 */
class DisplayTabItem(
    initialName: String = "New tab",
    initialIndex: Int
) {
    companion object {
        /**
         * A global, observable state that holds the `DisplayTabItem` currently being edited.
         * When this is set to a `DisplayTabItem`, the `EditTabDialogue` will be shown.
         */
        var tabToEdit = mutableStateOf<DisplayTabItem?>(null)
    }
    /**
     * A secondary constructor that creates a `DisplayTabItem` for the UI layer from a `TabItem` from the data layer.
     * This simplifies the process of converting persisted data into a stateful UI object.
     */
    constructor(tabItem: TabItem) : this(
        initialName = tabItem.name,
        initialIndex = tabItem.index
    )

    var name by mutableStateOf(initialName)
    var index by mutableIntStateOf(initialIndex)

    /**
     * Converts this UI-layer `DisplayTabItem` into a data-layer `TabItem` to be persisted.
     * @return A `TabItem` instance containing the current data, ready for serialization.
     */
    fun toTabItem(): TabItem {
        return TabItem(
            name = this.name,
            index = this.index
        )
    }
}
