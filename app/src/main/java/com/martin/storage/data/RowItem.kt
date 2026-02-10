package com.martin.storage.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.martin.storage.R
import kotlinx.serialization.Serializable

// --- Data Models ---

/**
 * A data class representing a storage item for local serialization (saving to a file).
 * It holds the raw data for a storage item.
 *
 * It's a `data class` to ensure it has proper `equals()` and `hashCode()` implementations.
 * This is crucial for Compose's `collectAsState` to correctly detect when the data has actually changed,
 * preventing infinite recomposition loops.
 *
 * @property name The name of the item (e.g., "Apples").
 * @property img The resource ID of the item's image.
 * @property count The quantity of the item.
 * @property pgIndex The index of the tab (e.g., Fridge, Cabinet) where the item belongs.
 */
@Serializable
data class LocalRowItem(
    var name: String,
    var img: Int,
    var count: Int,
    var pgIndex: Int
) {
    /**
     * Converts this data-centric `LocalRowItem` into a UI-centric `RowItem`.
     * @return A `RowItem` instance ready to be displayed in the UI.
     */
    fun toRowItem(): RowItem {
        return RowItem(this.name, this.img, this.count, this.pgIndex)
    }
}

/**
 * A class representing a storage item for use in the Jetpack Compose UI.
 * It uses Compose's `State` delegates (`mutableStateOf`, `mutableIntStateOf`) so that the UI
 * can automatically recompose when the item's properties (like `name` or `count`) change.
 *
 * @param initialName The starting name of the item.
 * @param img The resource ID of the item's image.
 * @param initialCount The starting quantity of the item.
 * @param pgIndex The index of the tab where the item belongs.
 */
class RowItem(
    initialName: String = "sunflowers",
    val img: Int = R.drawable.sunflowers,
    initialCount: Int = 0,
    val pgIndex: Int = 0
) {
    // These properties are Compose State objects. When their value changes, any composable that reads them will be recomposed.
    var name by mutableStateOf(initialName)
    var count by mutableIntStateOf(initialCount)

    /**
     * Safely increases the item count by one.
     */
    fun increaseCount() {
        count++
    }

    /**
     * Decreases the item count by one, but not below zero.
     */
    fun decreaseCount() {
        if (count > 0) {
            count--
        }
    }

    /**
     * Converts this UI-centric `RowItem` into a data-centric `LocalRowItem` for saving.
     * @return A `LocalRowItem` instance suitable for JSON serialization.
     */
    fun toLocalRowItem(): LocalRowItem {
        return LocalRowItem(this.name, this.img, this.count, this.pgIndex)
    }
}