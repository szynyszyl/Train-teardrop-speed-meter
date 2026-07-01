package com.rainspeed.app.domain.model

/** A GPS-derived speed reading, or null-able upstream when no fix is available. */
data class LocationSpeed(
    val speedMetersPerSecond: Float,
    val speedAccuracyMetersPerSecond: Float?,
    val timestampMillis: Long
)
