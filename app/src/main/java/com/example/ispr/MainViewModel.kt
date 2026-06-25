package com.example.ispr

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /**
     * Processor for real-time image analysis.
     */
    private val frameProcessor =
        com.example.ispr.logic.processing.ProcessingFrameProcessor()

    /**
     * Exposes the processing results to the UI.
     */
    val processingResult = frameProcessor.result

    /**
     * State for processing parameters, updated via sliders.
     */
    var processingParameters by mutableStateOf(
        com.example.ispr.logic.processing.ProcessingParameters())
        private set

    /**
     * State for the Source Configuration (Screen Probe).
     */
    var screenSourceConfiguration by mutableStateOf(
        com.example.ispr.logic.screen.ScreenSourceConfiguration())
        private set

    init {
        // Connect the processor to the camera stream
        cameraManager.setFrameProcessor(frameProcessor)
        
        // Handle parameter updates requested by the processor (e.g. roll-off detection)
        frameProcessor.onParametersChange = { updateProcessingParameters(it) }

        // Attempt to resume camera immediately if permission is already granted
        resumeCameraIfPossible()
        
        // Initialize the screen probe with default values
        updateSourceConfiguration(screenSourceConfiguration)
    }

    /**
     * Updates the screen probe configuration.
     */
    fun updateSourceConfiguration(config: com.example.ispr.logic.screen.ScreenSourceConfiguration) {
        screenSourceConfiguration = config
        screenManager.setControlledAreaFlatSource(config.isFlatSource)
        screenManager.setControlledAreaFrameRate(config.frameRate)
        screenManager.updateControlledAreaColor(config.toArgbColor())
    }

    /**
     * Updates processing parameters and notifies the processor.
     */
    fun updateProcessingParameters(params: com.example.ispr.logic.processing.ProcessingParameters) {
        processingParameters = params
        frameProcessor.updateParameters(params)
    }

    /**
     * Checks for permissions and starts the camera stream if allowed.
     */
    fun resumeCameraIfPossible() {
        if (cameraManager.hasPermissions()) {
            cameraManager.resume()
        }
    }

    /**
     * Resumes hardware managers when the app enters the foreground.
     */
    fun resume() {
        resumeCameraIfPossible()
        screenManager.resume()
    }

    /**
     * Pauses hardware managers when the app enters the background.
     */
    fun pause() {
        cameraManager.pause()
        screenManager.pause()
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure hardware is released if the ViewModel is destroyed
        cameraManager.pause()
    }
}
