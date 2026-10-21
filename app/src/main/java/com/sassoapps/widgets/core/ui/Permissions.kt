package com.sassoapps.widgets.core.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

class PermissionState(val granted: Boolean, val request: () -> Unit)

/** Tracks one runtime permission, re-checking when the screen resumes (e.g. after visiting Settings). */
@Composable
fun rememberPermissionState(permission: String, onResult: (Boolean) -> Unit = {}): PermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.isGranted(permission)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        onResult(it)
    }
    LifecycleResumeEffect(permission) {
        granted = context.isGranted(permission)
        onPauseOrDispose {}
    }
    return PermissionState(granted) { launcher.launch(permission) }
}

private fun Context.isGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
