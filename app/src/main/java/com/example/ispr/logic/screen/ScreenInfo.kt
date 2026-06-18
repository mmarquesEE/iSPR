package com.example.ispr.logic.screen

import com.example.ispr.ui.widgets.InfoRowContent

/**
 * Technical constants for unit conversions.
 */
object PhysicalConstants {
    const val CM_TO_INCH = 1 / 2.54f
}

/**
 * Raw pixel bounds and center for the camera cutout area.
 */
data class CameraCutoutInfo(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    /** The horizontal center of the cutout in raw pixels. */
    val centerX: Float = left + (width / 2f)

    /** The vertical center of the cutout in raw pixels, adjusted for lens positioning. */
    val centerY: Float = top + height - (width / 2f)
}

/**
 * Encapsulates the physical capabilities and current hardware state of the device screen.
 */
class ScreenHardwareInfo(
    val resolution: String,
    val densityDpi: String,
    val xDpi: String,
    val rawXDpi: Float,
    val yDpi: String,
    val rawYDpi: Float,
    val cameraInsetBounds: String,
    val cameraRawCutout: CameraCutoutInfo?,
    val wideColorGamutSupport: String,
    val hdrSupportedTypes: List<String>,
    val maxLuminance: String,
    val minLuminance: String,
    val refreshRate: String
) {
    fun toInfoRowContentList(): List<InfoRowContent> {
        val hdrText = if (hdrSupportedTypes.isNotEmpty()) {
            hdrSupportedTypes.joinToString(", ")
        } else "no"
        return listOf(
            InfoRowContent("Resolution", resolution),
            InfoRowContent("Density", densityDpi),
            InfoRowContent("X Density", xDpi),
            InfoRowContent("Y Density", yDpi),
            InfoRowContent("Camera Inset", cameraInsetBounds),
            InfoRowContent("Wide Color Gamut", wideColorGamutSupport),
            InfoRowContent("HDR Supported", hdrText),
            InfoRowContent("Max Luminance", maxLuminance),
            InfoRowContent("Min Luminance", minLuminance),
            InfoRowContent("Refresh Rate", refreshRate)
        )
    }
}
