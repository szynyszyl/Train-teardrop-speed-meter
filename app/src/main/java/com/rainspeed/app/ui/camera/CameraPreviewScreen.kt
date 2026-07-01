package com.rainspeed.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rainspeed.app.data.camera.CameraController
import com.rainspeed.app.data.camera.ThrottlingAnalyzer

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { CameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { previewView = it }
        }
    )

    LaunchedEffect(previewView) {
        val view = previewView ?: return@LaunchedEffect
        cameraController.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            previewView = view,
            // Placeholder analyzer for now: frame processing (grayscale, drop detection)
            // is wired in once OpenCV is integrated.
            analyzer = ThrottlingAnalyzer(frameInterval = 4) { imageProxy -> imageProxy.close() }
        )
    }

    DisposableEffect(Unit) {
        onDispose { cameraController.shutdown() }
    }
}
