package com.rainspeed.app.domain.calibration

import kotlin.math.abs

/** Linear fit `speed = slope * angle + intercept` produced by [CalibrationModel.fit]. */
data class CalibrationCoefficients(val slope: Double, val intercept: Double)

/**
 * Online least-squares regression mapping streak angle (degrees from vertical) to train speed
 * (m/s). Samples are added whenever GPS has a good-enough fix to trust as ground truth; once
 * fitted, the model can estimate speed from angle alone when GPS is unavailable (e.g. a tunnel).
 *
 * Pure Kotlin, no Android dependency — the running sums (n, Σx, Σy, Σxy, Σx²) let each new
 * sample update the fit in O(1) without keeping the sample history around.
 */
class CalibrationModel {
    private var n = 0
    private var sumAngle = 0.0
    private var sumSpeed = 0.0
    private var sumAngleSpeed = 0.0
    private var sumAngleSquared = 0.0

    val sampleCount: Int get() = n

    fun addSample(angleDegrees: Double, speedMetersPerSecond: Double) {
        n++
        sumAngle += angleDegrees
        sumSpeed += speedMetersPerSecond
        sumAngleSpeed += angleDegrees * speedMetersPerSecond
        sumAngleSquared += angleDegrees * angleDegrees
    }

    /** Fits `speed = slope * angle + intercept`, or null if there aren't enough samples yet or
     * all sampled angles are identical (no variance to fit a slope against). */
    fun fit(): CalibrationCoefficients? {
        if (n < 2) return null
        val denominator = n * sumAngleSquared - sumAngle * sumAngle
        if (abs(denominator) < MIN_DENOMINATOR) return null
        val slope = (n * sumAngleSpeed - sumAngle * sumSpeed) / denominator
        val intercept = (sumSpeed - slope * sumAngle) / n
        return CalibrationCoefficients(slope, intercept)
    }

    fun estimateSpeed(angleDegrees: Double): Double? {
        val coefficients = fit() ?: return null
        return coefficients.slope * angleDegrees + coefficients.intercept
    }

    private companion object {
        const val MIN_DENOMINATOR = 1e-9
    }
}
