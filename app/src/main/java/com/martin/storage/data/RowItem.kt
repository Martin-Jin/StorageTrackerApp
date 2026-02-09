package com.martin.storage.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.martin.storage.R
import kotlinx.serialization.Serializable

@Serializable
// Class used to store row item locally as json
class LocalRowItem(
    var name: String,
    var img: Int,
    var count: Int,
    var pgIndex: Int
)

class RowItem(
    initialName: String = "sunflowers",
    val img: Int = R.drawable.sunflowers,
    initialCount: Int = 0,
    val pgIndex: Int = 0
) {
    var name by mutableStateOf(initialName)
    var count by mutableIntStateOf(initialCount)
    fun increaseCount() {
        count++
    }

    fun decreaseCount() {
        if (count > 0) {
            count--
        }
    }
}