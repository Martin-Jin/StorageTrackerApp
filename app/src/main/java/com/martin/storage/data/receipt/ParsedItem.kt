package com.martin.storage.data.receipt

data class ParsedItem(
    val name: String,
    val quantity: Double?,
    val unit: String?,
    var selected: Boolean = true
)