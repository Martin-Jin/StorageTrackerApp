package com.martin.storage.data.receipt

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.martin.storage.ReceiptScannerActivity

class ReceiptScannerLauncher(
    private val launcher: ActivityResultLauncher<Intent>
) {

    fun launch(activity: Activity) {

        val intent =
            Intent(activity, ReceiptScannerActivity::class.java)

        launcher.launch(intent)
    }

    companion object {

        fun parseResult(data: Intent?): List<ParsedItem> {

            val json =
                data?.getStringExtra("receipt_items")
                    ?: return emptyList()

            val type =
                object : TypeToken<List<ParsedItem>>() {}.type

            return Gson().fromJson(json, type)
        }
    }
}