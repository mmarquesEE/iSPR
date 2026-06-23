package com.example.ispr.logic.camera

import com.example.ispr.ui.widgets.InfoRowContent

/**
 * Encapsulates detailed hardware specifications for the front-facing camera.
 * 
 * This data class is used to hold parsed information from [android.hardware.camera2.CameraCharacteristics],
 * providing a human-readable summary of the camera's capabilities, including sensor details,
 * supported ISO/Exposure ranges, and hardware levels.
 *
 * @property modelName The name or ID of the camera model.
 * @property sensorSize Physical dimensions of the camera sensor.
 * @property focalLengths List of supported focal lengths.
 * @property apertures List of supported aperture values.
 * @property isoRange Formatted string representing the supported ISO sensitivity range.
 * @property rawIsoRange The numerical [android.util.Range] of supported ISO values.
 * @property exposureTimeRange Formatted string representing the supported exposure time range.
 * @property rawExposureRange The numerical [android.util.Range] of supported exposure times in nanoseconds.
 * @property maxResolution The maximum supported capture resolution.
 * @property supportedHardwareLevel The legacy or limited/full/level-3 hardware support level.
 * @property facing Direction the camera is facing (expected to be FRONT).
 * @property sensorOrientation Clockwise angle that the sensor image needs to be rotated by.
 * @property autoExposureModes List of supported auto-exposure modes.
 * @property autoFocusModes List of supported auto-focus modes.
 */
class CameraHardwareInfo(
    val modelName: String,
    val sensorSize: String,
    val focalLengths: String,
    val apertures: String,
    val isoRange: String,
    val rawIsoRange: android.util.Range<Int>?,
    val exposureTimeRange: String,
    val rawExposureRange: android.util.Range<Long>?,
    val maxResolution: String,
    val supportedHardwareLevel: String,
    val facing: String,
    val sensorOrientation: String,
    val autoExposureModes: List<String>,
    val rawAeModes: List<Int>,
    val autoFocusModes: List<String>,
    val rawAfModes: List<Int>,
    val minFocusDistance: Float?,
    val supportedResolutions: List<android.util.Size>,
    val supportedFpsRanges: List<android.util.Range<Int>>,
    val resolutionMaxFps: Map<android.util.Size, Int>
) {
    /**
     * Returns the list of FPS ranges that are physically supported by the given resolution.
     */
    fun getSupportedFpsRangesFor(resolution: android.util.Size?): List<android.util.Range<Int>> {
        if (resolution == null) return supportedFpsRanges
        val maxFps = resolutionMaxFps[resolution] ?: 30
        return supportedFpsRanges.filter { it.upper <= maxFps }
    }

    /**
     * Converts the hardware information into a list of [InfoRowContent] for UI display.
     * 
     * @return A list of key-value pairs representing the camera's specifications.
     */
    fun toInfoRowContentList(): List<InfoRowContent> {
        val aeModes = if (autoExposureModes.isNotEmpty()) {
            autoExposureModes.joinToString(", ")
        } else "None"

        val afModes = if (autoFocusModes.isNotEmpty()) {
            autoFocusModes.joinToString(", ")
        } else "None"
        
        return listOf(
            InfoRowContent("Facing", facing),
            InfoRowContent("Max Resolution", maxResolution),
            InfoRowContent("Sensor Size", sensorSize),
            InfoRowContent("Focal Lengths", focalLengths),
            InfoRowContent("Apertures", apertures),
            InfoRowContent("ISO Range", isoRange),
            InfoRowContent("Exposure Range", exposureTimeRange),
            InfoRowContent("HW Level", supportedHardwareLevel),
            InfoRowContent("Sensor Orient.", sensorOrientation),
            InfoRowContent("AE Modes", aeModes),
            InfoRowContent("AF Modes", afModes)
        )
    }
}
