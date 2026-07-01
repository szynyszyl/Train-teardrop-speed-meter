package com.rainspeed.app.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION
)

data class AppPermissionsState(
    val cameraGranted: Boolean,
    val locationGranted: Boolean,
    val shouldShowRationale: Boolean,
    val permanentlyDenied: Boolean,
    val requestPermissions: () -> Unit
) {
    val allGranted: Boolean get() = cameraGranted && locationGranted
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@Composable
fun rememberAppPermissionsState(): AppPermissionsState {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.CAMERA)) }
    var locationGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var hasRequestedOnce by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasRequestedOnce = true
        cameraGranted = result[Manifest.permission.CAMERA] ?: cameraGranted
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: locationGranted
    }

    // Re-check permissions when the user returns from Settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraGranted = isGranted(context, Manifest.permission.CAMERA)
                locationGranted = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldShowRationale = activity != null && (
        activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
            activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        )

    val isPermanentlyDenied = hasRequestedOnce &&
        (!cameraGranted || !locationGranted) &&
        !shouldShowRationale

    return AppPermissionsState(
        cameraGranted = cameraGranted,
        locationGranted = locationGranted,
        shouldShowRationale = shouldShowRationale,
        permanentlyDenied = isPermanentlyDenied,
        requestPermissions = { launcher.launch(REQUIRED_PERMISSIONS) }
    )
}
