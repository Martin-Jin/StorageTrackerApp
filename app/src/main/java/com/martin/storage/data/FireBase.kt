package com.martin.storage.data

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database

/**
 * A general-purpose function for writing data to a specified path in the Firebase Realtime Database.
 * This function is asynchronous and uses a callback to handle success or failure.
 *
 * @param path The database path where the data will be written.
 * @param data The data to be written, which can be any basic data type or a custom object that Firebase can serialize.
 * @param callback A function that is invoked when the write operation completes. It receives a Boolean
 * indicating success (true) or failure (false), and an optional Exception in case of an error.
 */
fun write(path: String, data: Any?, callback: (Boolean, Exception?) -> Unit) {
    val database = Firebase.database
    val myRef = database.getReference(path)
    myRef.setValue(data)
        .addOnSuccessListener {
            callback(true, null)
        }
        .addOnFailureListener { exception ->
            callback(false, exception)
        }
}

/**
 * A general-purpose function for reading data from a specified path in the Firebase Realtime Database.
 * This function is asynchronous and uses a callback to return the result.
 *
 * @param path The database path from which to read data.
 * @param callback A function that is invoked when the read operation completes. It receives a
 * `DataSnapshot` containing the retrieved data, or an optional Exception in case of an error.
 */
fun read(path: String, callback: (DataSnapshot?, Exception?) -> Unit) {
    val database = Firebase.database
    database.getReference(path).get()
        .addOnSuccessListener { dataSnapshot ->
            callback(dataSnapshot, null)
        }
        .addOnFailureListener { exception ->
            callback(null, exception)
        }
}
