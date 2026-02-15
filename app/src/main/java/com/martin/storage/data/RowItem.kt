package com.martin.storage.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.martin.storage.R
import kotlinx.serialization.Serializable

// --- Data Models ---

/**
 * A serializable data class representing a storage item as it is stored in persistent storage
 * (like DataStore or Firebase). This class is designed for data transfer and persistence.
 *
 * It is a `data class` to get auto-generated `equals()` and `hashCode()`, which is important for
 * correctly comparing items and for efficient use in collections. The `@Serializable` annotation
 * allows the Kotlinx Serialization library to convert instances of this class to and from JSON.
 *
 * @property name The display name of the item (e.g., "Apples").
 * @property img A string representing the item's image. This can be a local file path or a drawable resource ID.
 * @property count The current quantity of the item.
 * @property unit The unit of measurement for the count (e.g., "Kg", "pcs").
 * @property pgIndex The zero-based index of the tab/page (e.g., Fridge, Cabinet) where this item is located.
 * @property id A unique identifier for the item, crucial for list operations and database keys.
 * @property decrement The amount the `count` should decrease by.
 * @property decrementInterval The frequency in days at which the `decrement` is applied.
 */
@Serializable
data class LocalRowItem(
    val name: String,
    val img: String,
    val count: Int,
    val unit: String,
    val pgIndex: Int,
    val id: String,
    val decrement: Int = 1,
    val decrementInterval: Int
)

/**
 * A stateful class representing a single storage item within the Compose UI.
 * This class is the "source of truth" for the UI layer. It uses Compose's `State` delegates
 * (`mutableStateOf`, `mutableIntStateOf`) which allows any composable that reads these properties
 * to automatically recompose whenever their values change, ensuring the UI is always up-to-date.
 *
 * @param initialName The starting name for the item when it's created.
 * @param initialImg The default image resource or path.
 * @param initialCount The starting quantity.
 * @param initialUnit The starting unit of measurement.
 * @param initialPgIndex The default tab index.
 * @param id An optional existing ID. If not provided, a new unique one will be generated.
 * @param decrement The amount the count decreases automatically.
 * @param decrementInterval The period in days for the automatic decrement.
 */
class RowItem(
    initialName: String = "New item",
    initialImg: String = R.drawable.sunflowers.toString(),
    initialCount: Int = 0,
    initialUnit: String = "Kg",
    initialPgIndex: Int = 0,
    id: String? = null, // Nullable to allow for new item creation
    var decrement: Int = 1,
    var decrementInterval: Int = 1
) {
    /**
     * Secondary constructor to create a UI-layer `RowItem` directly from a data-layer `LocalRowItem`.
     * This simplifies the conversion from persisted data to UI state.
     */
    constructor(localItem: LocalRowItem) : this(
        initialName = localItem.name,
        initialImg = localItem.img,
        initialCount = localItem.count,
        initialUnit = localItem.unit,
        initialPgIndex = localItem.pgIndex,
        id = localItem.id,
        decrement = localItem.decrement,
        decrementInterval = localItem.decrementInterval
    )

    companion object {
        /**
         * A global, observable state that holds the `RowItem` currently being edited.
         * When this value is set to a `RowItem`, the `EditItemDialog` will appear.
         * When it's set to `null`, the dialog is hidden.
         */
        var itemToEdit = mutableStateOf<RowItem?>(null)

        /**
         * A private set to keep track of all `RowItem` IDs currently in use.
         * This is used to ensure that any newly created `RowItem` gets a unique ID,
         * which is essential for stable list management in Compose's `LazyColumn`.
         */
        private val existingIds = mutableSetOf<String>()
    }

    val id: String
    var name by mutableStateOf(initialName)
    var count by mutableIntStateOf(initialCount)
    var unit by mutableStateOf(initialUnit)
    var img by mutableStateOf(initialImg)
    var pgIndex by mutableIntStateOf(initialPgIndex)

    init {
        // This block ensures every RowItem instance has a unique ID.
        // If a valid, unique ID is passed during construction, it's used.
        if (id != null && existingIds.add(id)) {
            this.id = id
        } else {
            // Otherwise, generate a new unique ID by trying different combinations
            // until an unused one is found and added to the set of existing IDs.
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
     * Decreases the item count by the `decrement` value, ensuring it does not go below zero.
     */
    fun decreaseCount() {
        if (count > 0) {
            count -= decrement
            if (count < 0) {
                count = 0
            }
        }
    }

    /**
     * Converts this UI-layer `RowItem` into a data-layer `LocalRowItem` for persistence.
     * @return A `LocalRowItem` instance with the current data, ready for serialization.
     */
    fun toLocalRowItem(): LocalRowItem {
        return LocalRowItem(
            name = this.name,
            img = this.img,
            count = this.count,
            unit = this.unit,
            pgIndex = this.pgIndex,
            id = this.id,
            decrement = this.decrement,
            decrementInterval = this.decrementInterval
        )
    }

    /**
     * Calculates and applies the automatic decrement to the item's count based on the
     * time elapsed since the app was last opened.
     *
     * @param last The timestamp (as a String) of the last time the app was opened.
     * @param now The current timestamp (as a String).
     */
    fun updateDecrement(last: String, now: String) {
        val lastMillis = last.toLongOrNull()
        val nowMillis = now.toLongOrNull()

        if (lastMillis == null || nowMillis == null) {
            Log.e("RowItem", "Invalid timestamps provided for decrement update.")
            return
        }

        val diffInMillis = nowMillis - lastMillis
        // Calculate the number of full days that have passed.
        val daysPassed = diffInMillis / (1000 * 60 * 60 * 24)

        Log.d("RowItem", "Days passed since last open for item '$name': $daysPassed")

        if (daysPassed > 0 && decrementInterval > 0) {
            // Determine how many times the decrement interval has occurred.
            val timesToDecrement = daysPassed / decrementInterval

            if (timesToDecrement > 0) {
                // Apply the total calculated decrement to the count.
                val totalDecrement = (decrement * timesToDecrement).toInt()
                // `coerceAtLeast(0)` ensures the count never drops below zero.
                count = (count - totalDecrement).coerceAtLeast(0)
                Log.d("RowItem", "Decremented '$name' by $totalDecrement over $daysPassed days. New count: $count")
            }
        }
    }
}
