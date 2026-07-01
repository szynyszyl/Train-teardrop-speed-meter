package com.rainspeed.app.domain.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AngleAggregatorTest {

    private val aggregator = AngleAggregator(outlierThresholdDegrees = 25.0)

    @Test
    fun `empty input yields no estimate`() {
        assertNull(aggregator.aggregate(emptyList()))
    }

    @Test
    fun `single value yields itself as median with zero spread`() {
        val estimate = aggregator.aggregate(listOf(12.0))!!
        assertEquals(12.0, estimate.medianDegrees, 1e-9)
        assertEquals(0.0, estimate.spreadDegrees, 1e-9)
        assertEquals(1, estimate.sampleCount)
    }

    @Test
    fun `median is robust to a single outlier`() {
        val estimate = aggregator.aggregate(listOf(10.0, 11.0, 9.0, 10.5, 85.0))!!
        assertEquals(10.25, estimate.medianDegrees, 1e-9)
        assertEquals(4, estimate.sampleCount)
    }

    @Test
    fun `spread reflects dispersion of inlier angles`() {
        val tight = aggregator.aggregate(listOf(10.0, 10.0, 10.0, 10.0))!!
        val wide = aggregator.aggregate(listOf(0.0, 5.0, 15.0, 20.0))!!
        assertEquals(0.0, tight.spreadDegrees, 1e-9)
        assert(wide.spreadDegrees > tight.spreadDegrees)
    }

    @Test
    fun `all values outside threshold of rough median still return rough median with zero samples`() {
        val estimate = aggregator.aggregate(listOf(0.0, 100.0))!!
        assertEquals(0, estimate.sampleCount)
    }
}
