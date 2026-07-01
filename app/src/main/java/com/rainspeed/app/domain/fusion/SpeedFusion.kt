package com.rainspeed.app.domain.fusion

import com.rainspeed.app.domain.calibration.CalibrationModel
import com.rainspeed.app.domain.model.AngleEstimate
import com.rainspeed.app.domain.model.LocationSpeed

/**
 * Decides, for one fused tick, what speed to report: trust GPS when it has a good-enough fix
 * (and use it to teach [calibrationModel]), otherwise fall back to the angle-based estimate once
 * the model has seen enough samples. Pure Kotlin — no Android dependency — so this decision logic
 * is unit-testable without a device.
 */
class SpeedFusion(
    private val calibrationModel: CalibrationModel,
    private val minCalibrationSamples: Int = 5,
    private val goodGpsAccuracyThresholdMetersPerSecond: Float = 3.0f,
    private val maxTrustedAngleSpreadDegrees: Double = 15.0
) {
    fun fuse(angleEstimate: AngleEstimate?, locationSpeed: LocationSpeed?): SpeedFusionResult {
        if (locationSpeed != null && hasGoodAccuracy(locationSpeed)) {
            if (angleEstimate != null && angleEstimate.spreadDegrees <= maxTrustedAngleSpreadDegrees) {
                calibrationModel.addSample(angleEstimate.medianDegrees, locationSpeed.speedMetersPerSecond.toDouble())
            }
            return SpeedFusionResult(
                speedMetersPerSecond = locationSpeed.speedMetersPerSecond.toDouble(),
                confidence = 1f,
                source = SpeedSource.GPS
            )
        }

        if (angleEstimate != null && calibrationModel.sampleCount >= minCalibrationSamples) {
            val estimated = calibrationModel.estimateSpeed(angleEstimate.medianDegrees)
            if (estimated != null) {
                val spreadPenalty = (angleEstimate.spreadDegrees / maxTrustedAngleSpreadDegrees).toFloat().coerceIn(0f, 1f)
                val confidence = (1f - spreadPenalty).coerceIn(0.1f, 0.9f)
                return SpeedFusionResult(estimated, confidence, SpeedSource.ANGLE_ESTIMATE)
            }
        }

        return SpeedFusionResult(speedMetersPerSecond = null, confidence = 0f, source = SpeedSource.UNAVAILABLE)
    }

    private fun hasGoodAccuracy(locationSpeed: LocationSpeed): Boolean {
        val accuracy = locationSpeed.speedAccuracyMetersPerSecond ?: return true
        return accuracy <= goodGpsAccuracyThresholdMetersPerSecond
    }
}
