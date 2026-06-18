package com.example.ispr.logic.camera


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat

/**
 * Handles permission logic specifically for camera hardware access.
 * This centralizes the required permission strings and check/request logic
 * within the camera logic module.
 */
class CameraPermissionManager(private val context: Context) {

    companion object {
        /**
         * The list of permissions required by the camera module.
         */
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    /**
     * Checks if all required camera permissions are currently granted.
     */
    fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * A specialized Composable that handles the camera permission request flow.
 * While it involves UI/Launcher logic, it is placed here to keep all
 * camera-access-related code in the camera module.
 */
@Composable
fun CameraPermissionRequester(
    onPermissionResult: (Boolean) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )

    SideEffect {
        launcher.launch(Manifest.permission.CAMERA)
    }
}
