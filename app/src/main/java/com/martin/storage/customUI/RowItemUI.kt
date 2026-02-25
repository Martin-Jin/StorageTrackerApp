package com.martin.storage.customUI

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.martin.storage.R
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Data Models ---

/**
 * A serializable data class that represents a storage item in its persistent state.
 * This class is used for saving and loading item data from sources like DataStore or Firebase.
 *
 * It's a `data class` to automatically get `equals()`, `hashCode()`, and other utility functions,
 * which are essential for comparing and managing items in collections. The `@Serializable` annotation
 * enables the Kotlinx Serialization library to convert this class to and from JSON format.
 *
 * @property name The name of the item (e.g., "Milk").
 * @property img A string that holds the path to the item's image, which can be a local file path or a drawable resource ID.
 * @property count The quantity of the item.
 * @property unit The unit of measurement (e.g., "Liters", "gallons").
 * @property pgIndex The zero-based index of the tab (e.g., Fridge, Pantry) where the item is stored.
 * @property decrement The value by which the `count` automatically decreases.
 * @property decrementInterval The frequency in days at which the `decrement` is applied.
 */
@Serializable
data class RowItem(
    val name: String,
    val img: String,
    val count: Int,
    val unit: String,
    val pgIndex: Int,
    val decrement: Int = 1,
    val decrementInterval: Int,
    val lastOpened: String = ""
)

/**
 * A stateful class that represents a single item in the Compose UI.
 * This class acts as the single source of truth for the UI layer. It uses Compose's `State` delegates
 * (`mutableStateOf`, `mutableIntStateOf`) to make its properties observable. Any composable that reads
 * these properties will automatically recompose when their values change, keeping the UI in sync.
 *
 * @param initialName The initial name for a newly created item.
 * @param initialImg The default image resource or path.
 * @param initialCount The starting quantity.
 * @param initialUnit The initial unit of measurement.
 * @param initialPgIndex The default tab index.
 * @param display A flag indicating whether the item should be displayed, useful for filtering in search results.
 * @param decrement The amount by which the count automatically decreases.
 * @param decrementInterval The period in days for the automatic decrement.
 */
class RowItemUI(
    initialName: String = "New item",
    initialImg: String = R.drawable.sunflowers.toString(),
    initialCount: Int = 0,
    initialUnit: String = "",
    initialPgIndex: Int = 0,
    var display: Boolean = true,
    var decrement: Int = 1,
    var decrementInterval: Int = 1,
    var lastOpened: String = "" // The last date a decrement was recorded
): UserInterface(name = initialName, identifier = "") {
    /**
     * A secondary constructor that creates a `RowItem` for the UI layer from a `LocalRowItem` from the data layer.
     * This simplifies the process of converting persisted data into a stateful UI object.
     */
    constructor(localItem: RowItem) : this(
        initialName = localItem.name,
        initialImg = localItem.img,
        initialCount = localItem.count,
        initialUnit = localItem.unit,
        initialPgIndex = localItem.pgIndex,
        decrement = localItem.decrement,
        decrementInterval = localItem.decrementInterval,
        lastOpened = localItem.lastOpened // The last date a decrement was recorded
    )

    override var name by mutableStateOf(initialName)
    var count by mutableIntStateOf(initialCount)
    var unit by mutableStateOf(initialUnit)
    var img by mutableStateOf(initialImg)
    var pgIndex by mutableIntStateOf(initialPgIndex)

    companion object {
        /**
         * A global, observable state that holds the `RowItem` currently being edited.
         * When this value is set to a `RowItem`, the `EditItemDialog` is displayed.
         * When it is `null`, the dialog is hidden.
         */
        var itemToEdit = mutableStateOf<RowItemUI?>(null)

        /**
         * Decreases the item's count by the `decrement` value, preventing it from going below zero.
         * This function now takes the last opened date and calculates the difference in days.
         */
        fun decrementByDays(pageItems: MutableList<RowItemUI>) {
            pageItems.forEach { 
                val last = it.lastOpened
                if (last.isNotEmpty()) {
                    val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    try {
                        val lastDate = format.parse(last)
                        if (lastDate != null) {
                            val currentDate = Date()
                            val diff = currentDate.time - lastDate.time
                            val daysPassed = TimeUnit.MILLISECONDS.toDays(diff)

                            if (daysPassed > 0 && it.decrementInterval > 0) {
                                val timesToDecrement = daysPassed / it.decrementInterval
                                if (timesToDecrement > 0) {
                                    val totalDecrement = (it.decrement * timesToDecrement).toInt()
                                    it.decreaseCount(totalDecrement)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Handle exception
                    }
                }
                it.lastOpened = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            }
        }
    }

    /**
     * Increases the item's count by one.
     */
    fun increaseCount() {
        count++
    }

    fun decreaseCount(amount: Int = 1) {
        count = (count - amount).coerceAtLeast(0)
    }

    /**
     * Converts this UI-layer `RowItem` into a data-layer `LocalRowItem` to be persisted.
     * @return A `LocalRowItem` instance containing the current data, ready for serialization.
     */
    fun toLocalRowItem(): RowItem {
        return RowItem(
            name = this.name,
            img = this.img,
            count = this.count,
            unit = this.unit,
            pgIndex = this.pgIndex,
            decrement = this.decrement,
            decrementInterval = this.decrementInterval,
            lastOpened = this.lastOpened
        )
    }
}
