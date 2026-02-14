package com.martin.storage.data

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database

/**
 * A general-purpose function for writing data to a specified path in the Firebase Realtime Database.
 *
 * @param path The path in the database where the data will be written.
 * @param data The data to be written. This can be any basic data type or a custom object.
 * @param callback A function to be invoked when the write operation is complete. It receives a Boolean
 * indicating success or failure, and an optional Exception object in case of failure.
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
 *
 * @param path The path in the database from which to read data.
 * @param callback A function to be invoked when the read operation is complete. It receives a
 * DataSnapshot containing the retrieved data, or an optional Exception object in case of failure.
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
