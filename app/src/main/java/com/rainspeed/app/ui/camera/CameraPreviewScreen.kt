package com.rainspeed.app.ui.camera

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rainspeed.app.data.camera.CameraController
import com.rainspeed.app.data.camera.ThrottlingAnalyzer
import com.rainspeed.app.data.vision.GrayscaleFrameProcessor

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { CameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var grayscaleFrame by remember { mutableStateOf<Bitmap?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier,
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            }
        )

        // Debug thumbnail proving the OpenCV grayscale pipeline runs end to end.
        // Replaced by the ROI + detected-streaks overlay once DropDetector lands.
        grayscaleFrame?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp)
                    .border(1.dp, Color.White)
            )
        }
    }

    LaunchedEffect(previewView) {
        val view = previewView ?: return@LaunchedEffect
        cameraController.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            previewView = view,
            analyzer = ThrottlingAnalyzer(frameInterval = 4) { imageProxy ->
                val bitmap = try {
                    GrayscaleFrameProcessor.process(imageProxy)
                } finally {
                    imageProxy.close()
                }
                // Mutate Compose state on the main thread; analysis runs on a background executor.
                ContextCompat.getMainExecutor(context).execute { grayscaleFrame = bitmap }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { cameraController.shutdown() }
    }
}
