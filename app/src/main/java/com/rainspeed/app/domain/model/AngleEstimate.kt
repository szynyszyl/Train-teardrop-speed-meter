package com.rainspeed.app.domain.model

/**
 * One frame's aggregated streak angle: the median of the inlier angle samples, plus their spread
 * (median absolute deviation) as a proxy for crosswind / measurement uncertainty.
 */
data class AngleEstimate(
    val medianDegrees: Double,
    val spreadDegrees: Double,
    val sampleCount: Int
)
