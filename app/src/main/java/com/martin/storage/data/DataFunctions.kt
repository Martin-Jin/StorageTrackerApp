package com.martin.storage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * A private extension property on `Context` that provides a singleton instance of `DataStore`.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// --- High-Level Data Functions ---

/**
 * Serializes a list of objects into a JSON string and saves it to DataStore,
 * completely overwriting any existing data under the given key.
 *
 * @param context The Android context, used to access the application's DataStore.
 * @param key The unique key under which the serialized data will be stored.
 * @param objectsToSave The list of objects to be saved, which must be serializable.
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
 * Creates a coroutine to write data to storage.
 *
 * @param context The context required for DataStore access.
 * @param scope A `CoroutineScope` to launch the asynchronous save operation.
 * @param itemsToSave The list of objects from the UI to be saved.
 * @param overWrite If `true`, the existing list in storage is replaced; otherwise, it's appended.
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
 * appending new objects, and writing the combined list back.
 *
 * @param context The Android context for accessing DataStore.
 * @param key The key where the list of objects is stored.
 * @param objectsToAppend The list of new objects to add to the existing list.
 */
suspend inline fun <reified T> appendObjects(
    context: Context,
    key: String,
    objectsToAppend: List<T>
) {
    Log.d(TAG, "Appending ${objectsToAppend.size} objects to key '$key'.")
    val existingJson = readLocalData(context, key).firstOrNull()

    val allObjects = if (existingJson != null) {
        Log.d(TAG, "Found ${existingJson.length} chars of existing data to append to.")
        json.decodeFromString<List<T>>(existingJson).toMutableList()
    } else {
        Log.d(TAG, "No existing data found at key '$key'. Creating a new list.")
        mutableListOf()
    }

    allObjects.addAll(objectsToAppend)
    Log.d(TAG, "Total objects after append: ${allObjects.size}. Now writing to DataStore.")

    val updatedJson = json.encodeToString(allObjects)

    writeLocalData(context, key, updatedJson)
}

/**
 * Reads a JSON string from DataStore and deserializes it into a list of objects.
 *
 * @param context The Android context for accessing DataStore.
 * @param key The key under which the objects are stored.
 * @return A `Flow` that emits the deserialized list of objects, or `null` if the key doesn't exist.
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
 */
suspend fun writeLocalData(context: Context, key: String, value: String) {
    val dataStoreKey = stringPreferencesKey(key)
    context.dataStore.edit { settings ->
        settings[dataStoreKey] = value
    }
}

/**
 * A generic, low-level function to read a raw string value from a given key in DataStore.
 * @return A `Flow` that emits the string value, or `null` if the key has no value.
 */
fun readLocalData(context: Context, key: String): Flow<String?> {
    val dataStoreKey = stringPreferencesKey(key)
    return context.dataStore.data.map { preferences ->
        preferences[dataStoreKey]
    }
}

// --- Business Logic Functions ---

/**
 * Copies an image from a content URI to the app's private internal storage, with optional resizing
 * and compression.
 *
 * @param context The application context.
 * @param uri The `Uri` of the source image.
 * @param quality The desired compression quality (0-100).
 * @param reqWidth The target width for the image.
 * @param reqHeight The target height for the image.
 * @return The absolute path of the saved image file, or `null` on failure.
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

fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

/**
 * A generic, reusable composable for loading and caching data from DataStore.
 * It observes a Flow of objects and invokes the `onDataLoaded` callback when data is successfully loaded.
 *
 * @param T The reified type of objects to load (e.g., `LocalRowItem`).
 * @param path The key for the data in DataStore.
 * @param onDataLoaded A callback function to execute when data is loaded.
 */
@Composable
inline fun <reified T : Any> LoadAndCache(
    path: String,
    crossinline onDataLoaded: (List<T>) -> Unit
) {
    val context = LocalContext.current
    val dataState by readLocalObjects<T>(context, path).collectAsState(initial = null)

    LaunchedEffect(dataState) {
        dataState?.let(onDataLoaded)
    }
}
