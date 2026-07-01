package com.rainspeed.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rainspeed.app.data.camera.CameraController
import com.rainspeed.app.data.camera.ThrottlingAnalyzer
import com.rainspeed.app.data.vision.FrameAnalysisResult
import com.rainspeed.app.data.vision.FrameProcessor
import com.rainspeed.app.domain.vision.AngleAggregator

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { CameraController(context) }
    val frameProcessor = remember { FrameProcessor() }
    val angleAggregator = remember { AngleAggregator() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var lastFrame by remember { mutableStateOf<FrameAnalysisResult?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier,
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            }
        )

        // Overlay draws detected streaks scaled from analysis-frame pixels to view pixels.
        // Approximate: doesn't account for FILL_CENTER's center-crop when aspect ratios differ.
        lastFrame?.let { frame ->
            Canvas(modifier = Modifier.matchParentSize()) {
                val scaleX = size.width / frame.displayWidth
                val scaleY = size.height / frame.displayHeight
                frame.streaks.forEach { streak ->
                    drawLine(
                        color = Color.Green,
                        start = Offset(streak.x1 * scaleX, streak.y1 * scaleY),
                        end = Offset(streak.x2 * scaleX, streak.y2 * scaleY),
                        strokeWidth = 4f
                    )
                }
            }

            val angleEstimate = angleAggregator.aggregate(frame.trackedAngles)
            val angleText = if (angleEstimate != null) {
                "Kąt: %.1f° (±%.1f°, n=%d)".format(
                    angleEstimate.medianDegrees,
                    angleEstimate.spreadDegrees,
                    angleEstimate.sampleCount
                )
            } else {
                "Kąt: brak danych (śledzenie rozpoczyna się)"
            }
            Text(
                text = "Wykryte smugi: ${frame.streaks.size}\n$angleText",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            // Debug thumbnail proving the OpenCV grayscale pipeline runs end to end.
            Image(
                bitmap = frame.previewBitmap.asImageBitmap(),
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
                val result = try {
                    frameProcessor.process(imageProxy)
                } finally {
                    imageProxy.close()
                }
                // Mutate Compose state on the main thread; analysis runs on a background executor.
                ContextCompat.getMainExecutor(context).execute { lastFrame = result }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.shutdown()
            frameProcessor.reset()
        }
    }
}
