package com.rainspeed.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rainspeed.app.ui.permissions.PermissionsScreen
import com.rainspeed.app.ui.permissions.rememberAppPermissionsState

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val permissionsState = rememberAppPermissionsState()

    if (permissionsState.allGranted) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Uprawnienia przyznane. Podgląd kamery zostanie dodany w kolejnym kroku.")
        }
    } else {
        PermissionsScreen(state = permissionsState, modifier = modifier.fillMaxSize())
    }
}
