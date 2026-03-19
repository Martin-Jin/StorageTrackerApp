package com.martin.storage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.martin.storage.data.receipt.OCRProcessor
import com.martin.storage.data.receipt.ParsedItem
import com.martin.storage.data.receipt.ReceiptCameraScreen
import com.martin.storage.data.receipt.ReceiptParser
import com.martin.storage.ui.theme.AppTheme

class ReceiptScannerActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                ReceiptCameraScreen { bitmap ->

                    OCRProcessor.process(bitmap) { lines ->

                        val parsed =
                            ReceiptParser.parseLines(lines)

                        setContent {

                            ReceiptItemSelectionScreen(parsed) { selected ->

                                val json =
                                    Gson().toJson(selected)

                                intent.putExtra(
                                    "receipt_items",
                                    json
                                )

                                setResult(RESULT_OK, intent)

                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptItemSelectionScreen(
    items: List<ParsedItem>,
    onConfirm: (List<ParsedItem>) -> Unit
) {

    val mutableItems = remember {
        items.map { it.copy() }.toMutableStateList()
    }

    Column(Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            items(mutableItems) { item ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {

                    Checkbox(
                        checked = item.selected,
                        onCheckedChange = {
                            item.selected = it
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = buildString {
                            append(item.name)

                            item.quantity?.let {
                                append("  $it")
                            }

                            item.unit?.let {
                                append(" $it")
                            }
                        }
                    )
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {

                val selected =
                    mutableItems.filter { it.selected }

                onConfirm(selected)
            }
        ) {
            Text("Add Selected Items")
        }
    }
}