package com.example.ispr.logic.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import java.util.Locale

/**
 * Logic provider that probes the Android Camera2 API to extract detailed camera hardware specifications.
 */
class CameraHardwareManager(private val context: Context) {
    val permissionManager = CameraPermissionManager(context)
    val streamManager = CameraStreamManager(context)

    /**
     * Queries the system for the front-facing camera hardware information.
     */
    fun getCameraInfo(): com.example.ispr.logic.camera.CameraHardwareInfo? {
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

    private fun extractCameraInfo(chars: CameraCharacteristics): com.example.ispr.logic.camera.CameraHardwareInfo {
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
            autoFocusModes = afModesList
        )
    }
}

