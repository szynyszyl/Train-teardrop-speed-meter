package com.rainspeed.app.data.logging

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val angleMedianDegrees: Double?,
    val angleSpreadDegrees: Double?,
    val gpsSpeedMetersPerSecond: Float?,
    val gpsAccuracyMetersPerSecond: Float?,
    val estimatedSpeedMetersPerSecond: Double?,
    val source: String
)
