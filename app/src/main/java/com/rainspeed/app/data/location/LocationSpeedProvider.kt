package com.rainspeed.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rainspeed.app.domain.model.LocationSpeed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Wraps [com.google.android.gms.location.FusedLocationProviderClient] and emits GPS speed
 * readings as they arrive. Emits null when there is currently no usable fix (GPS unavailable,
 * e.g. in a tunnel), so downstream fusion logic can fall back to the angle-based estimate.
 */
class LocationSpeedProvider(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // caller must gate this on ACCESS_FINE_LOCATION being granted
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun speedUpdates(intervalMillis: Long = 1000L): Flow<LocationSpeed?> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                trySend(result.lastLocation?.toLocationSpeedOrNull())
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) trySend(null)
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    private fun Location.toLocationSpeedOrNull(): LocationSpeed? {
        if (!hasSpeed()) return null
        return LocationSpeed(
            speedMetersPerSecond = speed,
            speedAccuracyMetersPerSecond = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
            timestampMillis = time
        )
    }
}
