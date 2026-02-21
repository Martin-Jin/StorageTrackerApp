/**
 * This file is the central hub for managing all local data persistence in the application.
 * It leverages Jetpack DataStore for storing key-value pairs and uses the Kotlinx Serialization
 * library to convert complex objects into JSON strings for storage. This approach provides a robust
 * and efficient way to handle app data, from simple user preferences to lists of custom objects.
 *
 * The file is organized into several sections:
 * 1.  **Constants and Global Variables**: Defines keys for DataStore, a TAG for logging, a shared
 *     JSON serializer instance, and an in-memory cache for frequently accessed data.
 * 2.  **High-Level Data Functions**: These are the primary functions the app should use for data
 *     operations. They provide a simple, type-safe API for writing, appending, and reading
 *     serializable objects, abstracting away the underlying JSON conversion and DataStore transactions.
 * 3.  **Low-Level DataStore Functions**: These are generic helper functions that interact directly
 *     with DataStore to read and write raw string data. They form the foundation upon which the
 *     high-level functions are built.
 * 4.  **Business Logic Functions**: This section contains functions specific to the application's
 *     data model, such as converting between UI-layer objects (`RowItem`) and data-layer objects
 *     (`LocalRowItem`) and handling image processing tasks like saving and compressing images.
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
 * A constant TAG used for logging throughout this file. This allows for easy filtering
 * of logs in Logcat to isolate data management-related events and errors.
 */
const val TAG = "DataManagement"

/**
 * The preference key used to store the main list of storage items in Jetpack DataStore.
 */
const val STORAGEITEMPATH = "storageItems"

/**
 * The preference key used to store the current user's unique ID (UID) in DataStore.
 */
const val UIDLOCALPATH = "UID"

/**
 * The preference key used to store the tabs the user created.
 */
const val TABITEMSPATH = "storageTabs"

/**
 * The preference key for storing the timestamp of when the app was last opened.
 * This is used to calculate item decrements over time.
 */
const val LAST_OPENED_KEY = "last_opened"

/**
 * An in-memory cache of variables that are written and read.
 * This list is loaded when the application starts to provide immediate access to the data
 * for the UI, avoiding delays from repeated asynchronous data reads.
 */
var storageItems = mutableListOf<LocalRowItem>()
var tabItems = mutableListOf<TabItem>()

/**
 * A shared, configured instance of the Kotlinx JSON serializer.
 * `encodeDefaults = true` is crucial here; it ensures that properties with default values
 * (like a count of 0 or an empty string) are explicitly included in the output JSON.
 * This prevents data loss when an object is serialized and then deserialized.
 */
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

/**
 * A private extension property on `Context` that provides a singleton instance of `DataStore`.
 * This is the recommended pattern for creating and accessing DataStore in an Android application,
 * ensuring that only one instance of the DataStore exists for the given name ("settings").
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// --- High-Level Data Functions ---

/**
 * Serializes a list of objects into a JSON string and saves it to DataStore, completely
 * overwriting any existing data stored under the given key.
 *
 * @param context The Android context, used to access the application's DataStore instance.
 * @param key The unique key under which the serialized data will be stored.
 * @param objectsToSave The list of objects to be saved. The objects must be serializable,
 *                      typically by being annotated with `@Serializable`.
 */

suspend inline fun <reified T> writeLocalObjects(
    context: Context,
    key: String,
    objectsToSave: List<T>
) {
    Log.d(
        TAG,
        "Writing (${objectsToSave.size} objects) to key '$key'. This will overwrite existing data."
    )
    val jsonString = json.encodeToString(objectsToSave)
    Log.d(TAG, "JSON to write: $jsonString")
    writeLocalData(context, key, jsonString)
}

/**
 * Creates a coroutine scope to write data to storage.
 *
 * @param context The context required for DataStore access.
 * @param scope A `CoroutineScope` to launch the asynchronous save operation.
 * @param itemsToSave The list of objects from the UI to be saved.
 * @param overWrite If `true`, the entire existing list in storage is replaced.
 *                  If `false`, the new items are appended to the existing list.
 */
inline fun <reified T> updateStoredValue(
    context: Context,
    scope: CoroutineScope,
    itemsToSave: MutableList<T>,
    key: String,
    overWrite: Boolean = true,
) {
    scope.launch {
        if (overWrite) {
            Log.i(TAG, "Updating stored items with OVERWRITE.")
            writeLocalObjects(
                context,
                key,
                itemsToSave
            )
        } else {
            Log.i(TAG, "Updating stored items with APPEND.")
            appendObjects(
                context,
                key,
                itemsToSave
            )
        }
    }
}

/**
 * Atomically updates a list of objects in DataStore by reading the existing list,
 * appending new objects to it, and writing the combined list back.
 *
 * @param context The Android context for accessing DataStore.
 * @param key The key where the list of objects is stored.
 * @param objectsToAppend The list of new objects to add to the existing list.
 *                        These objects must also be serializable.
 */
suspend inline fun <reified T> appendObjects(
    context: Context,
    key: String,
    objectsToAppend: List<T>
) {
    Log.d(TAG, "Appending ${objectsToAppend.size} objects to key '$key'.")
    // 1. Read the existing JSON string from DataStore.
    val existingJson = readLocalData(context, key).firstOrNull()

    // 2. Deserialize the JSON into a mutable list. If no data exists, start with a fresh list.
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

    // 4. Serialize the newly combined list back into a JSON string.
    val updatedJson = json.encodeToString(allObjects)

    // 5. Write the updated JSON string back to DataStore, overwriting the old list.
    writeLocalData(context, key, updatedJson)
}

/**
 * Reads a JSON string from DataStore and deserializes it into a list of objects of a specified type.
 *
 * @param context The Android context for accessing DataStore.
 * @param key The key under which the objects are stored.
 * @return A `Flow` that emits the deserialized list of objects. It will emit `null` if the key
 *         does not exist or if the data cannot be deserialized.
 */
inline fun <reified T> readLocalObjects(context: Context, key: String): Flow<List<T>?> {
    val stringFlow = readLocalData(context, key)
    return stringFlow.map { jsonString ->
        if (jsonString != null) {
            Log.d(
                TAG,
                "Successfully read ${jsonString.length} chars from key '$key'. Deserializing..."
            )
            json.decodeFromString<List<T>>(jsonString)
        } else {
            Log.d(TAG, "No data found for key '$key' in readLocalObjects.")
            null
        }
    }
}

// --- Low-Level DataStore Functions ---

/**
 * A generic, low-level function to write a raw string value to a specified key in DataStore.
 * This is the foundational write operation used by the higher-level functions.
 */
suspend fun writeLocalData(context: Context, key: String, value: String) {
    val dataStoreKey = stringPreferencesKey(key)
    context.dataStore.edit { settings ->
        settings[dataStoreKey] = value
    }
}

/**
 * A generic, low-level function to read a raw string value from a given key in DataStore.
 * @return A `Flow` that emits the string value whenever it changes. It emits `null` if the key
 *         has no value.
 */
fun readLocalData(context: Context, key: String): Flow<String?> {
    val dataStoreKey = stringPreferencesKey(key)
    return context.dataStore.data.map { preferences ->
        preferences[dataStoreKey]
    }
}

// --- Business Logic Functions ---

/**
 * Copies an image from a given content URI (e.g., from a photo gallery) to the app's private
 * internal storage. It can also resize and compress the image to save space and improve performance.
 *
 * @param context The application context, used to resolve the content URI and access the file system.
 * @param uri The `Uri` of the source image.
 * @param quality The desired compression quality for the output JPEG image (0-100).
 * @param reqWidth The target width for the image. If null, the original width is maintained.
 * @param reqHeight The target height for the image. If null, the original height is maintained.
 * @return The absolute path of the newly saved image file, or `null` if the operation fails.
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

        // Scale the bitmap only if requested dimensions are provided
        val bitmapToSave = if (reqWidth != null && reqHeight != null) {
            originalBitmap.scale(reqWidth, reqHeight, true)
        } else {
            originalBitmap
        }

        // Create a unique file name to avoid collisions
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmapToSave.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.close()

        // Recycle the scaled bitmap if it was created, and always recycle the original
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
