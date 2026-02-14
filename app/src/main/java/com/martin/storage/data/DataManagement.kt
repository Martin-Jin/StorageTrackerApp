/**
 * This file manages all local data storage for the application using Jetpack DataStore and Kotlinx Serialization.
 * It provides a set of high-level functions for reading, writing, and appending serializable objects,
 * abstracting the underlying implementation details of DataStore and JSON conversion.
 *
 * It is structured into three main sections:
 * 1. High-Level Data Functions: Simple API for common data operations on lists of objects.
 * 2. Low-Level DataStore Functions: Generic helpers for direct DataStore interaction (reading/writing raw strings).
 * 3. Business Logic Functions: App-specific logic, like handling `RowItem` conversions and updates.
 */
package com.martin.storage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
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
import java.io.File
import java.io.FileOutputStream

// --- Constants and Global Variables ---

/**
 * A constant TAG for logging to easily filter logs in Logcat.
 */
const val TAG = "DataManagement"

/**
 * The key used to save the list of storage items in DataStore.
 */
const val storageItemPath = "storageItems"
/**
 * The key used to save the uid of the user in DataStore.
 */
const val uidLocalPath = "UID"
/**
 * The key used to save the last opened timestamp in DataStore.
 */
const val LAST_OPENED_KEY = "last_opened"

/**
 * A temporary, in-memory cache for the user's storage items.
 * This list is populated when the app launches to be immediately available to the UI.
 */
var storageItems = mutableListOf<LocalRowItem>()

/**
 * A shared instance of the Json serializer.
 * `encodeDefaults = true` ensures that properties with default values (e.g., count = 0)
 * are included in the JSON output, preventing data loss during serialization.
 */
val json = Json { encodeDefaults = true }

/**
 * A private extension property on Context to provide a singleton instance of DataStore.
 * This is the standard way to create and access DataStore in an Android app.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// --- High-Level Data Functions ---

/**
 * Serializes a list of objects into a JSON string and saves it to DataStore, overwriting any existing data at the given key.
 * @param context The context to access DataStore.
 * @param key The key to store the objects under.
 * @param objectsToSave The list of objects to save. Must be annotated with @Serializable.
 */
suspend inline fun <reified T> writeLocalObjects(
    context: Context,
    key: String,
    objectsToSave: List<T>
) {
    Log.d(TAG, "Writing (${objectsToSave.size} objects) to key '$key'. This will overwrite existing data.")
    val jsonString = json.encodeToString(objectsToSave)
    Log.d(TAG, "JSON to write: $jsonString")
    writeLocalData(context, key, jsonString)
}

/**
 * Reads existing data from DataStore, appends new objects, and saves the combined list back.
 * @param context The context to access DataStore.
 * @param key The key where the objects are stored.
 * @param objectsToAppend The list of new objects to append. Must be annotated with @Serializable.
 */
suspend inline fun <reified T> appendObjects(
    context: Context,
    key: String,
    objectsToAppend: List<T>
) {
    Log.d(TAG, "Appending ${objectsToAppend.size} objects to key '$key'.")
    // 1. Read the existing JSON string from DataStore.
    val existingJson = readLocalData(context, key).firstOrNull()

    // 2. Deserialize the existing JSON into a mutable list, or create an empty one.
    val allObjects = if (existingJson != null) {
        Log.d(TAG, "Found ${existingJson.length} chars of existing data to append to.")
        json.decodeFromString<List<T>>(existingJson).toMutableList()
    } else {
        Log.d(TAG, "No existing data found at key '$key'. Creating a new list.")
        mutableListOf()
    }

    // 3. Add the new objects to the list.
    allObjects.addAll(objectsToAppend)
    Log.d(TAG, "Total objects after append: ${allObjects.size}. Now writing to DataStore.")

    // 4. Serialize the combined list back to a JSON string.
    val updatedJson = json.encodeToString(allObjects)

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
            Log.d(TAG, "Successfully read ${jsonString.length} chars from key '$key'. Deserializing...")
            json.decodeFromString<List<T>>(jsonString)
        } else {
            Log.d(TAG, "No data found for key '$key' in readLocalObjects.")
            null
        }
    }
}

// --- Low-Level DataStore Functions ---

/**
 * A generic, low-level function to write a string value to a given key in DataStore.
 */
suspend fun writeLocalData(context: Context, key: String, value: String) {
    val dataStoreKey = stringPreferencesKey(key)
    context.dataStore.edit { settings ->
        settings[dataStoreKey] = value
    }
}

/**
 * A generic, low-level function to read a string value from a given key in DataStore.
 * @return A Flow that will emit the string value when it's available, or null if not present.
 */
fun readLocalData(context: Context, key: String): Flow<String?> {
    val dataStoreKey = stringPreferencesKey(key)
    return context.dataStore.data.map { preferences ->
        preferences[dataStoreKey]
    }
}

// --- Business Logic Functions ---

/**
 * A helper function to save a list of UI-layer `RowItem` objects to local storage.
 * It converts them to `LocalRowItem` objects suitable for serialization.
 * @param context The context for DataStore access.
 * @param scope A CoroutineScope to launch the DataStore operation.
 * @param rowItems The list of `RowItem` objects to save.
 * @param overWrite If true, overwrites all existing data. If false, appends to existing data.
 */
fun updateStoredItems(
    context: Context,
    scope: CoroutineScope,
    rowItems: MutableList<RowItem>,
    overWrite: Boolean = true,
) {
    val localRowItems = mutableListOf<LocalRowItem>()
    for (item in rowItems) {
        localRowItems.add(item.toLocalRowItem())
    }
    scope.launch {
        if (overWrite) {
            Log.i(TAG, "Updating stored items with OVERWRITE.")
            writeLocalObjects(
                context,
                storageItemPath,
                localRowItems
            )
        } else {
            Log.i(TAG, "Updating stored items with APPEND.")
            appendObjects(
                context,
                storageItemPath,
                localRowItems
            )
        }
    }
}

/**
 * Copies an image from a given URI to the app's internal storage, with optional resizing and compression.
 *
 * @param context The application context.
 * @param uri The URI of the image to save.
 * @param quality The compression quality, from 0 to 100. 100 means no compression.
 * @param reqWidth The desired width of the image. If null, the original width is used.
 * @param reqHeight The desired height of the image. If null, the original height is used.
 * @return The absolute path of the newly created image file, or null if saving fails.
 */
fun saveImageFromUri(
    context: Context,
    uri: Uri,
    quality: Int = 90,
    reqWidth: Int? = 150,
    reqHeight: Int? = 150
): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val bitmapToSave = if (reqWidth != null && reqHeight != null) {
            originalBitmap.scale(reqWidth, reqHeight, true)
        } else {
            originalBitmap
        }

        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmapToSave.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.close()

        if (bitmapToSave != originalBitmap) {
            bitmapToSave.recycle()
        }
        originalBitmap.recycle()

        Log.d(
            "DataManagement",
            "Saved and compressed image to: ${file.absolutePath} with resolution ${bitmapToSave.width}x${bitmapToSave.height}"
        )
        file.absolutePath
    } catch (e: Exception) {
        Log.e("DataManagement", "Error saving image from URI", e)
        null
    }
}
