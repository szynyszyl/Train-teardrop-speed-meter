package com.rainspeed.app.domain.fusion

import com.rainspeed.app.domain.calibration.CalibrationModel
import com.rainspeed.app.domain.model.AngleEstimate
import com.rainspeed.app.domain.model.LocationSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedFusionTest {

    private fun locationSpeed(speedMps: Float, accuracy: Float?) =
        LocationSpeed(speedMetersPerSecond = speedMps, speedAccuracyMetersPerSecond = accuracy, timestampMillis = 0L)

    private fun angle(median: Double, spread: Double = 2.0, samples: Int = 10) =
        AngleEstimate(medianDegrees = median, spreadDegrees = spread, sampleCount = samples)

    @Test
    fun `trusts good GPS fix and reports it directly`() {
        val fusion = SpeedFusion(CalibrationModel())
        val result = fusion.fuse(angle(10.0), locationSpeed(20f, accuracy = 1.0f))

        assertEquals(20.0, result.speedMetersPerSecond!!, 1e-9)
        assertEquals(SpeedSource.GPS, result.source)
        assertEquals(1f, result.confidence, 1e-9f)
    }

    @Test
    fun `good GPS fix teaches the calibration model`() {
        val model = CalibrationModel()
        val fusion = SpeedFusion(model)
        repeat(3) { i -> fusion.fuse(angle(10.0 + i), locationSpeed(20f + i, accuracy = 1.0f)) }

        assertEquals(3, model.sampleCount)
    }

    @Test
    fun `poor GPS accuracy is not trusted as ground truth`() {
        val model = CalibrationModel()
        val fusion = SpeedFusion(model, goodGpsAccuracyThresholdMetersPerSecond = 3.0f)
        val result = fusion.fuse(angle(10.0), locationSpeed(20f, accuracy = 10.0f))

        assertEquals(SpeedSource.UNAVAILABLE, result.source)
        assertEquals(0, model.sampleCount)
    }

    @Test
    fun `falls back to angle estimate once calibrated and GPS is unavailable`() {
        val model = CalibrationModel()
        val fusion = SpeedFusion(model, minCalibrationSamples = 3)
        // Calibrate: speed = 2 * angle.
        listOf(5.0, 10.0, 15.0).forEach { a -> fusion.fuse(angle(a), locationSpeed((2 * a).toFloat(), accuracy = 0.5f)) }

        val result = fusion.fuse(angle(20.0), locationSpeed = null)

        assertEquals(SpeedSource.ANGLE_ESTIMATE, result.source)
        assertEquals(40.0, result.speedMetersPerSecond!!, 1e-6)
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun `reports unavailable when GPS is gone and calibration has too few samples`() {
        val fusion = SpeedFusion(CalibrationModel(), minCalibrationSamples = 5)
        val result = fusion.fuse(angle(20.0), locationSpeed = null)

        assertEquals(SpeedSource.UNAVAILABLE, result.source)
        assertNull(result.speedMetersPerSecond)
        assertEquals(0f, result.confidence, 1e-9f)
    }

    @Test
    fun `reports unavailable with no angle and no GPS`() {
        val fusion = SpeedFusion(CalibrationModel())
        val result = fusion.fuse(angleEstimate = null, locationSpeed = null)

        assertEquals(SpeedSource.UNAVAILABLE, result.source)
    }

    @Test
    fun `wide angle spread does not pollute calibration even with good GPS`() {
        val model = CalibrationModel()
        val fusion = SpeedFusion(model, maxTrustedAngleSpreadDegrees = 15.0)
        fusion.fuse(angle(10.0, spread = 40.0), locationSpeed(20f, accuracy = 1.0f))

        assertEquals(0, model.sampleCount)
    }
}
