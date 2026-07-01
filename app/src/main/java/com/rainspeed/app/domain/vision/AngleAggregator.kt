package com.rainspeed.app.domain.vision

import com.rainspeed.app.domain.model.AngleEstimate
import kotlin.math.abs

/**
 * Collapses one frame's raw streak-angle samples (degrees from vertical) into a single robust
 * estimate: median angle plus median absolute deviation as the spread/uncertainty proxy. Values
 * further than [outlierThresholdDegrees] from the initial median are dropped as noise (static
 * droplets, false detections) before the final median/spread are computed. Pure Kotlin — no
 * Android or OpenCV dependency, so it is unit-testable on the JVM.
 */
class AngleAggregator(private val outlierThresholdDegrees: Double = 25.0) {

    fun aggregate(angles: List<Double>): AngleEstimate? {
        if (angles.isEmpty()) return null

        val roughMedian = median(angles)
        val inliers = angles.filter { abs(it - roughMedian) <= outlierThresholdDegrees }
        if (inliers.isEmpty()) return AngleEstimate(roughMedian, 0.0, 0)

        val finalMedian = median(inliers)
        val spread = median(inliers.map { abs(it - finalMedian) })
        return AngleEstimate(finalMedian, spread, inliers.size)
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}
