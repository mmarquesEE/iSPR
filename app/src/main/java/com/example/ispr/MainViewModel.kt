package com.example.ispr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ispr.logic.camera.CameraHardwareManager
import com.example.ispr.logic.screen.ScreenHardwareManager

/**
 * Survives configuration changes and maintains the single-instance hardware managers.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    /**
     * Single entry point for all camera-related operations.
     */
    val cameraManager = CameraHardwareManager(application)
    
    /**
     * Single entry point for all screen-related operations.
     */
    val screenManager = ScreenHardwareManager(application)

    override fun onCleared() {
        super.onCleared()
        // Ensure hardware is released if the ViewModel is destroyed
        cameraManager.pause()
    }
}
