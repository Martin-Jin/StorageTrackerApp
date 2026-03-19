package com.martin.storage.data.receipt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

@Composable
fun ReceiptCameraScreen(
    onPhotoTaken: (Bitmap) -> Unit
) {
    val context = LocalContext.current

    val previewView = remember { PreviewView(context) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            imageCapture =
                ImageCapture.Builder().build()

            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(context))
    }

    Column(Modifier.fillMaxSize()) {

        AndroidView(
            factory = { previewView },
            modifier = Modifier.weight(1f)
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {

                val photoFile = File.createTempFile(
                    "receipt",
                    ".jpg",
                    context.cacheDir
                )

                val outputOptions =
                    ImageCapture.OutputFileOptions.Builder(photoFile)
                        .build()

                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {

                        override fun onImageSaved(
                            output: ImageCapture.OutputFileResults
                        ) {

                            val bitmap =
                                BitmapFactory.decodeFile(photoFile.absolutePath)

                            onPhotoTaken(bitmap)
                        }

                        override fun onError(exception: ImageCaptureException) {}
                    }
                )
            }
        ) {
            Text("Scan Receipt")
        }
    }
}