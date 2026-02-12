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
 * @property img The resource ID or file path of the item's image.
 * @property count The quantity of the item.
 * @property pgIndex The index of the tab (e.g., Fridge, Cabinet) where the item belongs.
 */
@Serializable
data class LocalRowItem(
    val name: String,
    val img: String,
    val count: Int,
    val unit: String,
    val pgIndex: Int,
    val id: String
) {
    /**
     * Converts this data-centric `LocalRowItem` into a UI-centric `RowItem`.
     * @return A `RowItem` instance ready to be displayed in the UI.
     */
    fun toRowItem(): RowItem {
        return RowItem(
            initialName = this.name,
            initialImg = this.img,
            initialCount = this.count,
            initialUnit = this.unit,
            initialPgIndex = this.pgIndex,
            id = this.id
        )
    }
}

/**
 * A class representing a storage item for use in the Jetpack Compose UI.
 * It uses Compose's `State` delegates (`mutableStateOf`, `mutableIntStateOf`) so that the UI
 * can automatically recompose when the item's properties (like `name` or `count`) change.
 *
 * @param initialName The starting name of the item.
 * @param initialImg The resource ID or file path of the item's image.
 * @param initialCount The starting quantity of the item.
 * @param initialPgIndex The index of the tab where the item belongs.
 */
class RowItem(
    initialName: String = "sunflowers",
    initialImg: String = R.drawable.sunflowers.toString(),
    initialCount: Int = 0,
    initialUnit: String = "Kg",
    initialPgIndex: Int = 0,
    id: String? = null // Changed to nullable
) {
    companion object {
        private val existingIds = mutableSetOf<String>()
    }

    val id: String
    var name by mutableStateOf(initialName)
    var count by mutableIntStateOf(initialCount)
    var unit by mutableStateOf(initialUnit)
    var img by mutableStateOf(initialImg)
    var pgIndex by mutableIntStateOf(initialPgIndex)

    init {
        // Use passed id if it is not null and unique
        if (id != null && existingIds.add(id)) {
            this.id = id
        } else {
            // Otherwise, generate a new unique id
            var n = 1
            var tempId = "itemNum$n-P$initialPgIndex"
            while (!existingIds.add(tempId)) {
                n++
                tempId = "$initialName-$n-$initialPgIndex"
            }
            this.id = tempId
        }
    }

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
        return LocalRowItem(
            name = this.name,
            img = this.img,
            count = this.count,
            unit = this.unit,
            pgIndex = this.pgIndex,
            id = this.id
        )
    }
}