package com.example.ispr.logic.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Logic provider that probes the Android Camera2 API to extract detailed camera hardware specifications.
 */
class CameraHardwareManager(private val context: Context) {

    private val cameraStreamManager = CameraStreamManager(context)

    /**
     * Flow emitting the currently active camera resolution.
     */
    val activeResolution = cameraStreamManager.activeResolution
    val settings = cameraStreamManager.settings

    /**
     * Starts or resumes the camera stream.
     */
    fun resume() = cameraStreamManager.resume()

    /**
     * Pauses the camera stream and releases hardware resources.
     */
    fun pause() = cameraStreamManager.pause()

    /**
     * Attaches a preview surface (e.g., from a TextureView).
     */
    fun setPreviewSurface(surface: android.view.Surface?) = cameraStreamManager.setPreviewSurface(surface)

    /**
     * Updates the camera hardware settings (ISO, Exposure, etc.).
     */
    fun updateSettings(settings: CameraSettings) = cameraStreamManager.updateSettings(settings)

    /**
     * Attaches a processor for real-time image analysis.
     */
    fun setFrameProcessor(processor: com.example.ispr.logic.processing.ProcessingFrameProcessor?) =
        cameraStreamManager.setFrameProcessor(processor)

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

    /**
     * Queries the system for the front-facing camera hardware information.
     */
    fun getCameraInfo(): CameraHardwareInfo? {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraIds = manager.cameraIdList
            for (id in cameraIds) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                // We are looking for the front camera
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return extractCameraInfo(characteristics)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun extractCameraInfo(chars: CameraCharacteristics): CameraHardwareInfo {
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

        val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val outputSizes = streamMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray<Size>()
        val maxRes = outputSizes.maxByOrNull { it.width * it.height }

        val hwLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hwLevelStr = when (hwLevel) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External"
            else -> "Unknown"
        }

        val aeModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        val aeModesRaw = aeModes?.toList() ?: emptyList()
        val aeModesList = aeModes?.map {
            when(it) {
                0 -> "OFF"
                1 -> "ON"
                2 -> "ON_AUTO_FLASH"
                3 -> "ON_ALWAYS_FLASH"
                4 -> "ON_AUTO_FLASH_REDEYE"
                else -> "ID_$it"
            }
        } ?: emptyList()

        val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        val afModesRaw = afModes?.toList() ?: emptyList()
        val afModesList = afModes?.map {
            when(it) {
                0 -> "OFF"
                1 -> "AUTO"
                2 -> "MACRO"
                3 -> "CONTINUOUS_VIDEO"
                4 -> "CONTINUOUS_PICTURE"
                5 -> "EDOF"
                else -> "ID_$it"
            }
        } ?: emptyList()

        val minFocusDistance = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val supportedResolutions = streamMap?.getOutputSizes(ImageFormat.YUV_420_888)?.toList() ?: emptyList()
        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList() ?: emptyList()

        val resolutionMaxFps = supportedResolutions.associateWith { size ->
            val minFrameDuration = streamMap?.getOutputMinFrameDuration(ImageFormat.YUV_420_888, size) ?: 0L
            if (minFrameDuration > 0) (1_000_000_000L / minFrameDuration).toInt() else 30
        }

        return CameraHardwareInfo(
            modelName = Build.MODEL, // Android doesn't directly provide a camera "Model Name" usually
            sensorSize = sensorSize?.let { "${it.width} x ${it.height} mm" } ?: "Unknown",
            focalLengths = focalLengths?.joinToString(", ") { "${it}mm" } ?: "Unknown",
            apertures = apertures?.joinToString(", ") { "f/$it" } ?: "Unknown",
            isoRange = isoRange?.let { "${it.lower} - ${it.upper}" } ?: "Unknown",
            rawIsoRange = isoRange,
            exposureTimeRange = exposureRange?.let {
                val lowerMs = it.lower / 1_000_000.0
                val upperMs = it.upper / 1_000_000.0
                String.format(Locale.US, "%.3f - %.1f ms", lowerMs, upperMs)
            } ?: "Unknown",
            rawExposureRange = exposureRange,
            maxResolution = maxRes?.let { "${it.width} x ${it.height} px" } ?: "Unknown",
            supportedHardwareLevel = hwLevelStr,
            facing = "Front",
            sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION)?.toString() ?: "Unknown",
            autoExposureModes = aeModesList,
            rawAeModes = aeModesRaw,
            autoFocusModes = afModesList,
            rawAfModes = afModesRaw,
            minFocusDistance = minFocusDistance,
            supportedResolutions = supportedResolutions,
            supportedFpsRanges = fpsRanges,
            resolutionMaxFps = resolutionMaxFps
        )
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        } else {
            onPermissionResult(true)
        }
    }
}
