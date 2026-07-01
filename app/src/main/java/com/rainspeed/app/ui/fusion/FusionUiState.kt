package com.rainspeed.app.ui.fusion

import com.rainspeed.app.domain.fusion.SpeedSource

/** Display-ready fused result: speed in km/h, current source, and calibration progress. */
data class FusionUiState(
    val speedKmh: Double? = null,
    val confidence: Float = 0f,
    val source: SpeedSource = SpeedSource.UNAVAILABLE,
    val gpsAvailable: Boolean = false,
    val calibrationSampleCount: Int = 0
)
