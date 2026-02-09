package com.martin.storage

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database

// General write function
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

// General read function
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