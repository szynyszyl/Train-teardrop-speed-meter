package com.rainspeed.app.ui.fusion

import android.annotation.SuppressLint
import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.rainspeed.app.data.camera.CameraController
import com.rainspeed.app.data.camera.ThrottlingAnalyzer
import com.rainspeed.app.data.location.LocationSpeedProvider
import com.rainspeed.app.data.logging.LoggingRepository
import com.rainspeed.app.data.vision.FrameAnalysisResult
import com.rainspeed.app.data.vision.FrameProcessor
import com.rainspeed.app.domain.calibration.CalibrationModel
import com.rainspeed.app.domain.fusion.SpeedFusion
import com.rainspeed.app.domain.vision.AngleAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Combines the camera-derived angle estimate with GPS speed via [kotlinx.coroutines.flow.combine],
 * drives [CalibrationModel] through [SpeedFusion], and exposes the fused result as [uiState].
 * [frame] is exposed separately for the camera screen's debug overlay (detected streaks, preview
 * thumbnail) — that visualization is unrelated to the fused speed decision itself.
 */
class FusionViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraController = CameraController(application)
    private val frameProcessor = FrameProcessor()
    private val angleAggregator = AngleAggregator()
    private val locationSpeedProvider = LocationSpeedProvider(application)
    private val calibrationModel = CalibrationModel()
    private val speedFusion = SpeedFusion(calibrationModel)
    private val loggingRepository = LoggingRepository(application)

    private val _frame = MutableStateFlow<FrameAnalysisResult?>(null)
    val frame: StateFlow<FrameAnalysisResult?> = _frame.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    private var cameraBound = false

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (cameraBound) return
        cameraBound = true
        viewModelScope.launch {
            try {
                cameraController.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    analyzer = ThrottlingAnalyzer(frameInterval = 4) { imageProxy ->
                        val result = try {
                            frameProcessor.process(imageProxy)
                        } finally {
                            imageProxy.close()
                        }
                        _frame.value = result
                    }
                )
            } catch (e: Exception) {
                _cameraError.value = "Nie udało się uruchomić kamery: ${e.message}"
            }
        }
    }

    @SuppressLint("MissingPermission") // gated by AppPermissionsState before the camera screen shows
    val uiState: StateFlow<FusionUiState> = combine(
        _frame.map { frame -> frame?.let { angleAggregator.aggregate(it.trackedAngles) } },
        locationSpeedProvider.speedUpdates()
    ) { angleEstimate, locationSpeed ->
        val result = speedFusion.fuse(angleEstimate, locationSpeed)

        if (angleEstimate != null || locationSpeed != null) {
            loggingRepository.logMeasurement(
                timestampMillis = System.currentTimeMillis(),
                angleMedianDegrees = angleEstimate?.medianDegrees,
                angleSpreadDegrees = angleEstimate?.spreadDegrees,
                gpsSpeedMetersPerSecond = locationSpeed?.speedMetersPerSecond,
                gpsAccuracyMetersPerSecond = locationSpeed?.speedAccuracyMetersPerSecond,
                estimatedSpeedMetersPerSecond = result.speedMetersPerSecond,
                source = result.source
            )
        }

        FusionUiState(
            speedKmh = result.speedMetersPerSecond?.times(3.6),
            confidence = result.confidence,
            source = result.source,
            gpsAvailable = locationSpeed != null,
            calibrationSampleCount = calibrationModel.sampleCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FusionUiState())

    /** Exports every logged measurement as CSV into app-private external storage. */
    suspend fun exportCsv(): File {
        val exportDir = getApplication<Application>().getExternalFilesDir(null)
            ?: getApplication<Application>().filesDir
        val file = File(exportDir, "rainspeed_export_${System.currentTimeMillis()}.csv")
        return loggingRepository.exportToCsv(file)
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.shutdown()
        frameProcessor.reset()
    }
}
