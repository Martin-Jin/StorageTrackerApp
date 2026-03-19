package com.martin.storage.data.receipt

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

private const val TAG = "ORCProcessor"

object OCRProcessor {

    fun process(
        bitmap: Bitmap,
        onResult: (List<String>) -> Unit
    ) {

        val recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { result ->

                val orderedLines = extractReadingOrder(result)

                onResult(orderedLines)
            }
    }

    /**
     * Reconstruct reading order:
     * Top -> Bottom
     * Left -> Right inside each row
     */
    private fun extractReadingOrder(text: Text): List<String> {

        val rowTolerance = 40 // Adjust if receipt lines are too close or far apart

        val lines = text.textBlocks
            .flatMap { it.lines }
            .filter { it.boundingBox != null }

        val groupedRows = lines
            .sortedBy { it.boundingBox!!.top }
            .groupBy { line ->
                line.boundingBox!!.top / rowTolerance
            }

        val orderedLines = mutableListOf<String>()

        groupedRows
            .toSortedMap()
            .values
            .forEach { row ->

                val sortedRow = row.sortedBy { it.boundingBox!!.left }

                sortedRow.forEach { line ->
                    orderedLines.add(line.text)
                }
            }

        return orderedLines
    }
}