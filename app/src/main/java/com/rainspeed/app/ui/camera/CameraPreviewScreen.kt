package com.rainspeed.app.ui.camera

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainspeed.app.data.vision.FrameAnalysisResult
import com.rainspeed.app.domain.fusion.SpeedSource
import com.rainspeed.app.ui.fusion.FusionUiState
import com.rainspeed.app.ui.fusion.FusionViewModel
import kotlinx.coroutines.launch

private const val LOW_LIGHT_BRIGHTNESS_THRESHOLD = 40.0

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier, viewModel: FusionViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraError by viewModel.cameraError.collectAsStateWithLifecycle()

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
        frame?.let { f ->
            Canvas(modifier = Modifier.matchParentSize()) {
                val scaleX = size.width / f.displayWidth
                val scaleY = size.height / f.displayHeight
                f.streaks.forEach { streak ->
                    drawLine(
                        color = Color.Green,
                        start = Offset(streak.x1 * scaleX, streak.y1 * scaleY),
                        end = Offset(streak.x2 * scaleX, streak.y2 * scaleY),
                        strokeWidth = 4f
                    )
                }
            }

            // Debug thumbnail proving the OpenCV grayscale pipeline runs end to end.
            Image(
                bitmap = f.previewBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp)
                    .border(1.dp, Color.White)
            )
        }

        val warnings = buildWarnings(frame, cameraError)
        if (warnings.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color(0xAA5C1A1A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                warnings.forEach { warning ->
                    Text(text = warning, color = Color.White)
                }
            }
        }

        Text(
            text = fusionStatusText(uiState),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color(0x99000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    val file = viewModel.exportCsv()
                    Toast.makeText(context, "Wyeksportowano: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Eksportuj CSV")
        }
    }

    LaunchedEffect(previewView) {
        previewView?.let { viewModel.bindCamera(lifecycleOwner, it) }
    }
}

private fun buildWarnings(frame: FrameAnalysisResult?, cameraError: String?): List<String> {
    val warnings = mutableListOf<String>()
    if (cameraError != null) {
        warnings += cameraError
    }
    if (frame != null) {
        if (frame.meanBrightness < LOW_LIGHT_BRIGHTNESS_THRESHOLD) {
            warnings += "Słabe oświetlenie — detekcja kropli może być mniej dokładna."
        }
        if (frame.streaks.isEmpty() && frame.trackedAngles.isEmpty()) {
            warnings += "Brak wykrytych smug deszczu — skieruj kamerę na mokrą szybę."
        }
    }
    return warnings
}

private fun fusionStatusText(state: FusionUiState): String {
    val speedText = state.speedKmh?.let { "%.1f km/h".format(it) } ?: "brak danych"
    val sourceText = when (state.source) {
        SpeedSource.GPS -> "GPS"
        SpeedSource.ANGLE_ESTIMATE -> "kąt kropli (kalibracja)"
        SpeedSource.UNAVAILABLE -> "brak"
    }
    return "Prędkość: $speedText\n" +
        "Źródło: $sourceText (pewność ${(state.confidence * 100).toInt()}%)\n" +
        "GPS: ${if (state.gpsAvailable) "aktywny" else "brak sygnału"}, " +
        "kalibracja: ${state.calibrationSampleCount} próbek"
}
