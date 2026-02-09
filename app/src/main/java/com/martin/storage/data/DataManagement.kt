package com.martin.storage.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// Keys
const val storageItemPath = "storageItems"

// Stored variables
var storageItems = mutableListOf(mutableStateListOf<RowItem>())

// DataStore instance for storing data locally
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
// val myItemsFlow: Flow<List<RowItem>?> = readLocalObjects(context, storageItemPath)

/**
 * Serializes the given list of objects into a JSON string and saves it to DataStore, overwriting existing data.
 * @param context The context to access DataStore.
 * @param key The key to store the objects under.
 * @param objectsToSave The list of objects to save. Must be annotated with @Serializable.
 */
suspend inline fun <reified T> writeLocalObjects(
    context: Context,
    key: String,
    objectsToSave: List<T>
) {
    val jsonString = Json.encodeToString(objectsToSave)
    writeLocalData(context, key, jsonString)
}

/**
 * Reads existing data from DataStore, appends new objects, and saves the combined list.
 * @param context The context to access DataStore.
 * @param key The key where the objects are stored.
 * @param objectsToAppend The list of new objects to append. Must be annotated with @Serializable.
 */
suspend inline fun <reified T> appendObjects(
    context: Context,
    key: String,
    objectsToAppend: List<T>
) {
    // 1. Read the existing JSON string from DataStore.
    val existingJson = readLocalData(context, key).firstOrNull()

    // 2. Deserialize the existing JSON into a mutable list, or create an empty one.
    val allObjects = if (existingJson != null) {
        Json.decodeFromString<List<T>>(existingJson).toMutableList()
    } else {
        mutableListOf()
    }

    // 3. Add the new objects to the list.
    allObjects.addAll(objectsToAppend)

    // 4. Serialize the combined list back to a JSON string.
    val updatedJson = Json.encodeToString(allObjects)

    // 5. Save the updated JSON string back to DataStore.
    writeLocalData(context, key, updatedJson)
}

/**
 * Reads a JSON string from DataStore and deserializes it into a list of objects.
 * @param context The context to access DataStore.
 * @param key The key the objects are stored under.
 * @return a Flow that emits the deserialized list of objects, or null if not found.
 */
inline fun <reified T> readLocalObjects(context: Context, key: String): Flow<List<T>?> {
    val stringFlow = readLocalData(context, key)
    return stringFlow.map { jsonString ->
        if (jsonString != null) {
            Json.decodeFromString<List<T>>(jsonString)
        } else {
            null
        }
    }
}

// Write function for local data (suspend function)
suspend fun writeLocalData(context: Context, key: String, value: String) {
    val dataStoreKey = stringPreferencesKey(key)
    context.dataStore.edit { settings ->
        settings[dataStoreKey] = value
    }
}

// Read function for local data (returns a Flow)
fun readLocalData(context: Context, key: String): Flow<String?> {
    val dataStoreKey = stringPreferencesKey(key)
    return context.dataStore.data.map { preferences ->
        preferences[dataStoreKey]
    }
}

fun saveStorageItem(
    context: Context,
    scope: CoroutineScope,
    rowItem: RowItem,
) {
    val tempItem = LocalRowItem(rowItem.name, rowItem.img,rowItem.count, rowItem.pgIndex)
    scope.launch {
        appendObjects(
            context,
            storageItemPath,
            listOf(tempItem)
        )
    }
}
