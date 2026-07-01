package com.rainspeed.app.domain.fusion

enum class SpeedSource { GPS, ANGLE_ESTIMATE, UNAVAILABLE }

data class SpeedFusionResult(
    val speedMetersPerSecond: Double?,
    val confidence: Float,
    val source: SpeedSource
)
