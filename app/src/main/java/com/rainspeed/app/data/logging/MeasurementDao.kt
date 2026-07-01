package com.rainspeed.app.data.logging

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY timestampMillis ASC")
    suspend fun getAll(): List<MeasurementEntity>
}
