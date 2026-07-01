package com.rainspeed.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rainspeed.app.ui.camera.CameraPreviewScreen
import com.rainspeed.app.ui.permissions.PermissionsScreen
import com.rainspeed.app.ui.permissions.rememberAppPermissionsState

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val permissionsState = rememberAppPermissionsState()

    if (permissionsState.allGranted) {
        CameraPreviewScreen(modifier = modifier.fillMaxSize())
    } else {
        PermissionsScreen(state = permissionsState, modifier = modifier.fillMaxSize())
    }
}
