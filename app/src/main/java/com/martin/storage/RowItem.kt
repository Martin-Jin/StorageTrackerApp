package com.martin.storage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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