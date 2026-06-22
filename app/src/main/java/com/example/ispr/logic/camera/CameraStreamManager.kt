package com.example.ispr.logic.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.example.ispr.logic.processing.ProcessingFrameProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * State container for manual camera settings.
 */
data class CameraSettings(
    val isAuto: Boolean = true,
    val aeMode: Int = CaptureRequest.CONTROL_AE_MODE_ON,
    val afMode: Int = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
    val iso: Int = 400,
    val exposureTimeNs: Long = 10_000_000L, // 10ms
    val focusDistance: Float = 0f,
    val resolution: Size? = null,
    val fpsRange: Range<Int>? = null
)

/**
 * Manages the camera lifecycle, preview stream, and manual hardware controls.
 *
 * This class handles the initialization and configuration of the camera using the Camera2 API.
 * It manages a [HandlerThread] for background operations to avoid blocking the UI thread,
 * handles opening/closing the [CameraDevice], and maintains the [CameraCaptureSession].
 *
 * Features:
 * - Real-time image analysis via [FrameProcessor].
 * - Dynamic preview surface attachment/detachment.
 * - Manual control over ISO and exposure time via [CameraSettings].
 * - Lifecycle-aware management via [resume] and [pause] methods.
 */
class CameraStreamManager(context: Context) {
    private val TAG = "CameraStreamManager"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    /**
     * Semaphore to ensure mutual exclusion between camera opening and closing operations.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    private val _settings = MutableStateFlow(CameraSettings())
    val settings = _settings.asStateFlow()

    private var previewSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private var frameProcessor: ProcessingFrameProcessor? = null

    // Cache characteristics to avoid heavy hits to CameraManager during sanitization
    private var cachedCharacteristics: CameraCharacteristics? = null

    /**
     * Flow emitting the currently active camera resolution.
     */
    private val _activeResolution = MutableStateFlow<Size?>(null)
    val activeResolution = _activeResolution.asStateFlow()

    /**
     * Attaches a processor for real-time image analysis.
     *
     * @param processor The [ProcessingFrameProcessor] to handle incoming camera frames.
     */
    fun setFrameProcessor(processor: ProcessingFrameProcessor?) {
        frameProcessor = processor
    }

    /**
     * Initializes the background thread and triggers the camera opening process.
     *
     * This method should be called from the UI component's `onStart` or `onResume` lifecycle hook.
     * The camera will remain active even if no preview surface is attached.
     */
    fun resume() {
        if (backgroundThread != null) return

        backgroundThread = HandlerThread("CameraBackground", android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY).also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)

        openCamera()
    }

    /**
     * Stops the camera, releases resources, and shuts down the background thread.
     *
     * This method should be called from the UI component's `onStop` or `onPause` lifecycle hook.
     */
    fun pause() {
        closeCamera()
        imageReader?.close()
        imageReader = null
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping background thread", e)
        }
    }

    /**
     * Internal helper to identify the front camera and request access from the system.
     *
     * This method:
     * 1. Acquires a lock to prevent concurrent access.
     * 2. Selects the best front-facing camera.
     * 3. Selects an optimal resolution (preferring 16:9 aspect ratio).
     * 4. Configures an [ImageReader] to receive YUV_420_888 frames.
     * 5. Opens the [CameraDevice].
     */
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        backgroundHandler?.post {
            try {
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw RuntimeException("Time out waiting to lock camera opening.")
                }

                val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                } ?: run {
                    cameraOpenCloseLock.release()
                    return@post
                }

                val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
                cachedCharacteristics = characteristics
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                // Query sizes specifically for YUV processing format
                val outputSizes = map?.getOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()

                // Log available FPS ranges for debugging
                val ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                Log.d(TAG, "Available FPS Ranges: ${ranges?.contentToString()}")

                // Prefer setting resolution, otherwise fallback to 720p/480p logic
                val bestSize = _settings.value.resolution ?: outputSizes.filter { it.width <= 1280 && it.height <= 720 }
                    .sortedByDescending { it.width * it.height }
                    .firstOrNull {
                        val ratio = it.width.toFloat() / it.height.toFloat()
                        abs(ratio - (16f / 9f)) < 0.1
                    } ?: Size(640, 480)

                // Sync the settings state with the hardware-selected defaults *before* setting activeResolution
                // This prevents "null vs value" mismatches that trigger unnecessary restarts.
                val currentSettings = _settings.value
                val availableFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                val defaultFps = availableFpsRanges?.maxByOrNull { it.upper }
                
                _settings.value = currentSettings.copy(
                    resolution = currentSettings.resolution ?: bestSize,
                    fpsRange = currentSettings.fpsRange ?: defaultFps
                )
                
                // Now update activeResolution to match what we actually opened with
                _activeResolution.value = _settings.value.resolution ?: bestSize

                imageReader?.close()

                imageReader = ImageReader.newInstance(
                    bestSize.width, bestSize.height, ImageFormat.YUV_420_888, 4
                ).apply {
                    setOnImageAvailableListener({ reader ->
                        val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                        val processor = frameProcessor
                        if (processor != null) {
                            processor.processImage(image)
                        } else {
                            image.close()
                        }
                    }, backgroundHandler)
                }

                cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraOpenCloseLock.release()
                        cameraDevice = camera
                        createCaptureSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        cameraOpenCloseLock.release()
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        cameraOpenCloseLock.release()
                        camera.close()
                        cameraDevice = null
                    }
                }, backgroundHandler)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening camera", e)
                cameraOpenCloseLock.release()
            }
        }
    }

    /**
     * Attaches or detaches a preview surface (e.g., from a TextureView).
     *
     * Camera capture continues regardless of this surface's presence. If a new surface
     * is provided, the [CameraCaptureSession] is re-created to include it.
     *
     * @param surface The [Surface] to display the preview, or null to detach.
     */
    fun setPreviewSurface(surface: Surface?) {
        backgroundHandler?.post {
            if (previewSurface == surface) return@post
            previewSurface = surface
            // Re-create session to include/exclude the preview surface
            if (cameraDevice != null) {
                createCaptureSession()
            }
        }
    }

    /**
     * Creates a new [CameraCaptureSession] with the configured surfaces.
     *
     * Always includes the [ImageReader] surface for processing, and optionally
     * the [previewSurface] if one is attached.
     */
    private fun createCaptureSession() {
        val device = cameraDevice ?: return
        val readerSurface = imageReader?.surface ?: return

        val surfaces = mutableListOf(readerSurface)
        previewSurface?.let { surfaces.add(it) }

        try {
            // Close existing session before creating a new one
            captureSession?.close()
            captureSession = null

            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            for (s in surfaces) {
                previewRequestBuilder?.addTarget(s)
            }

            @Suppress("DEPRECATION")
            device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    updateCaptureRequest()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Capture session configuration failed")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
        }
    }

    /**
     * Updates the running camera stream with new hardware settings.
     * Performs validation to ensure Resolution, FPS, and Exposure are physically compatible.
     *
     * @param settings The new [CameraSettings] to apply.
     */
    fun updateSettings(newSettings: CameraSettings) {
        val oldSettings = _settings.value
        val sanitized = sanitizeSettings(newSettings)
        
        // Only update state if it actually changed to prevent unnecessary recompositions
        if (sanitized != oldSettings) {
            _settings.value = sanitized
            
            backgroundHandler?.post {
                // Resolution change requires re-opening camera.
                // We only restart if the resolution actually changed from the currently active one.
                if (sanitized.resolution != _activeResolution.value && sanitized.resolution != null) {
                    closeCamera()
                    openCamera()
                } else {
                    updateCaptureRequest()
                }
            }
        }
    }

    /**
     * Enforces hardware coupling rules and physical sensor limits:
     * 1. If isAuto is true, force AE/AF to standard auto modes.
     * 2. FPS range must be supported by the current resolution.
     * 3. Exposure time cannot exceed the frame duration (1/FPS).
     * 4. ISO and Exposure must stay within physical sensor limits.
     */
    private fun sanitizeSettings(s: CameraSettings): CameraSettings {
        val characteristics = cachedCharacteristics ?: return s
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return s
        val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val availableFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

        var sanitized = s

        // 1. Auto Mode Enforcement - Only force if isAuto is true
        if (s.isAuto) {
            sanitized = sanitized.copy(
                aeMode = CaptureRequest.CONTROL_AE_MODE_ON,
                afMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        }

        // 2. Resolution -> FPS Coupling
        val activeRes = sanitized.resolution ?: _activeResolution.value
        val maxFpsForRes = activeRes?.let { res ->
            val minFrameDuration = streamMap.getOutputMinFrameDuration(ImageFormat.YUV_420_888, res)
            if (minFrameDuration > 0) (1_000_000_000L / minFrameDuration).toInt() else 60
        } ?: 60

        // Only override the FPS range if the current one is physically impossible for the resolution
        var finalFps = sanitized.fpsRange ?: availableFpsRanges?.maxByOrNull { it.upper }
        if (finalFps != null && finalFps.upper > maxFpsForRes) {
            finalFps = availableFpsRanges
                ?.filter { it.upper <= maxFpsForRes }
                ?.maxByOrNull { it.upper }
        }

        // 3. FPS -> Exposure Coupling
        val currentMaxFps = finalFps?.upper ?: 30
        val maxExposureByFps = 1_000_000_000L / currentMaxFps
        
        // 4. Physical Range Clamping
        val finalIso = isoRange?.let { sanitized.iso.coerceIn(it.lower, it.upper) } ?: sanitized.iso
        // Ensure exposure doesn't exceed 1/FPS to prevent frame drops, but don't force a fallback if within limits
        val finalExp = expRange?.let { 
            sanitized.exposureTimeNs.coerceIn(it.lower, minOf(it.upper, maxExposureByFps)) 
        } ?: sanitized.exposureTimeNs.coerceAtMost(maxExposureByFps)

        return sanitized.copy(
            iso = finalIso, 
            exposureTimeNs = finalExp,
            fpsRange = finalFps,
            resolution = sanitized.resolution // Preserve null if it was null, or the user's selection
        )
    }

    /**
     * Applies the current [CameraSettings] to the [CaptureRequest.Builder] and
     * restarts the repeating request in the [CameraCaptureSession].
     */
    private fun updateCaptureRequest() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return
        val currentSettings = _settings.value

        try {
            // Use the settings range; it should have been sanitized already by updateSettings
            currentSettings.fpsRange?.let { range ->
                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
            }

            // Disable stabilization to reduce processing overhead
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)

            if (currentSettings.isAuto) {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            } else {
                builder.set(CaptureRequest.CONTROL_AE_MODE, currentSettings.aeMode)
                builder.set(CaptureRequest.CONTROL_AF_MODE, currentSettings.afMode)
                
                if (currentSettings.aeMode == CaptureRequest.CONTROL_AE_MODE_OFF) {
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, currentSettings.iso)
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentSettings.exposureTimeNs)
                }
                
                if (currentSettings.afMode == CaptureRequest.CONTROL_AF_MODE_OFF) {
                    builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, currentSettings.focusDistance)
                }
            }

            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to update capture request", e)
        }
    }

    /**
     * Safely releases the [CameraCaptureSession] and [CameraDevice].
     *
     * Acquires the [cameraOpenCloseLock] to ensure no new sessions are being created
     * during the closing process.
     */
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            _activeResolution.value = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }
}
