package com.rainspeed.app.data.logging

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MeasurementEntity::class], version = 1, exportSchema = false)
abstract class RainSpeedDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile private var instance: RainSpeedDatabase? = null

        fun getInstance(context: Context): RainSpeedDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RainSpeedDatabase::class.java,
                    "rainspeed.db"
                ).build().also { instance = it }
            }
    }
}
