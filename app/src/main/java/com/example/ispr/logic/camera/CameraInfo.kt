package com.example.ispr.logic.camera

import com.example.ispr.ui.widgets.InfoRowContent

/**
 * Encapsulates detailed hardware specifications for the front-facing camera.
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
    val autoFocusModes: List<String>
) {
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