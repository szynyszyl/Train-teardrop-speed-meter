package com.rainspeed.app.domain.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalibrationModelTest {

    @Test
    fun `no samples yields no estimate`() {
        val model = CalibrationModel()
        assertNull(model.fit())
        assertNull(model.estimateSpeed(10.0))
    }

    @Test
    fun `single sample is not enough to fit a line`() {
        val model = CalibrationModel()
        model.addSample(angleDegrees = 10.0, speedMetersPerSecond = 5.0)
        assertNull(model.fit())
    }

    @Test
    fun `recovers exact slope and intercept from noiseless linear data`() {
        val model = CalibrationModel()
        // speed = 0.8 * angle + 2.0
        listOf(5.0, 10.0, 15.0, 20.0, 25.0).forEach { angle ->
            model.addSample(angleDegrees = angle, speedMetersPerSecond = 0.8 * angle + 2.0)
        }

        val coefficients = model.fit()!!
        assertEquals(0.8, coefficients.slope, 1e-6)
        assertEquals(2.0, coefficients.intercept, 1e-6)
        assertEquals(5, model.sampleCount)
    }

    @Test
    fun `estimateSpeed extrapolates using the fitted line`() {
        val model = CalibrationModel()
        listOf(0.0, 10.0, 20.0, 30.0).forEach { angle ->
            model.addSample(angleDegrees = angle, speedMetersPerSecond = 2.0 * angle)
        }

        assertEquals(50.0, model.estimateSpeed(25.0)!!, 1e-6)
    }

    @Test
    fun `approximates the trend when samples have noise`() {
        val model = CalibrationModel()
        // Underlying relationship speed = 1.5 * angle, with small alternating noise.
        val angles = listOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0)
        angles.forEachIndexed { index, angle ->
            val noise = if (index % 2 == 0) 0.2 else -0.2
            model.addSample(angleDegrees = angle, speedMetersPerSecond = 1.5 * angle + noise)
        }

        val coefficients = model.fit()!!
        assertEquals(1.5, coefficients.slope, 0.05)
    }

    @Test
    fun `identical angles cannot fit a slope`() {
        val model = CalibrationModel()
        repeat(5) { model.addSample(angleDegrees = 15.0, speedMetersPerSecond = 10.0) }
        assertNull(model.fit())
        assertNull(model.estimateSpeed(15.0))
    }
}
