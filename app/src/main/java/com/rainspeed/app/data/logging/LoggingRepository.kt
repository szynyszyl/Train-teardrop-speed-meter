package com.rainspeed.app.data.logging

import android.content.Context
import com.rainspeed.app.domain.fusion.SpeedSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Persists each fused measurement to Room and exports the log as CSV. */
class LoggingRepository(context: Context) {

    private val dao = RainSpeedDatabase.getInstance(context).measurementDao()

    suspend fun logMeasurement(
        timestampMillis: Long,
        angleMedianDegrees: Double?,
        angleSpreadDegrees: Double?,
        gpsSpeedMetersPerSecond: Float?,
        gpsAccuracyMetersPerSecond: Float?,
        estimatedSpeedMetersPerSecond: Double?,
        source: SpeedSource
    ) {
        dao.insert(
            MeasurementEntity(
                timestampMillis = timestampMillis,
                angleMedianDegrees = angleMedianDegrees,
                angleSpreadDegrees = angleSpreadDegrees,
                gpsSpeedMetersPerSecond = gpsSpeedMetersPerSecond,
                gpsAccuracyMetersPerSecond = gpsAccuracyMetersPerSecond,
                estimatedSpeedMetersPerSecond = estimatedSpeedMetersPerSecond,
                source = source.name
            )
        )
    }

    suspend fun exportToCsv(targetFile: File): File = withContext(Dispatchers.IO) {
        val measurements = dao.getAll()
        targetFile.bufferedWriter().use { writer ->
            writer.write(CSV_HEADER)
            writer.newLine()
            measurements.forEach { measurement ->
                writer.write(measurement.toCsvRow())
                writer.newLine()
            }
        }
        targetFile
    }

    private fun MeasurementEntity.toCsvRow(): String = listOf(
        timestampMillis,
        angleMedianDegrees ?: "",
        angleSpreadDegrees ?: "",
        gpsSpeedMetersPerSecond ?: "",
        gpsAccuracyMetersPerSecond ?: "",
        estimatedSpeedMetersPerSecond ?: "",
        source
    ).joinToString(",")

    private companion object {
        const val CSV_HEADER =
            "timestamp_ms,angle_median_deg,angle_spread_deg,gps_speed_mps,gps_accuracy_mps,estimated_speed_mps,source"
    }
}
