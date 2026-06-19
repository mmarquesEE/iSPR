package com.example.ispr.logic.screen

import com.example.ispr.ui.widgets.InfoRowContent

/**
 * Technical constants for unit conversions and physical calculations.
 */
object PhysicalConstants {
    /** Conversion factor from centimeters to inches. */
    const val CM_TO_INCH = 1 / 2.54f
}

/**
 * Encapsulates raw pixel bounds and calculated center for the device's camera cutout.
 * 
 * This class provides convenient access to the geometric properties of the 
 * display's "hole punch" or "notch", allowing for precise UI alignment
 * relative to the camera lens.
 *
 * @property left The X-coordinate of the left edge of the cutout in pixels.
 * @property top The Y-coordinate of the top edge of the cutout in pixels.
 * @property width The width of the cutout area in pixels.
 * @property height The height of the cutout area in pixels.
 */
data class CameraCutoutInfo(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    /** The horizontal center of the cutout in raw pixels. */
    val centerX: Float = left + (width / 2f)

    /** 
     * The vertical center of the cutout in raw pixels. 
     * Note: Adjusted for specific hardware lens positioning within the cutout area.
     */
    val centerY: Float = top + height - (width / 2f)
}

/**
 * Encapsulates the physical capabilities and current hardware state of the device screen.
 * 
 * This data class aggregates information about display density, resolution,
 * HDR support, and camera cutout geometry. It is typically populated by [ScreenHardwareManager].
 *
 * @property resolution Human-readable resolution string (e.g., "1080 x 2400 px").
 * @property densityDpi Dots per inch as reported by system metrics.
 * @property xDpi Horizontal pixels per inch.
 * @property rawXDpi Numerical horizontal pixels per inch.
 * @property yDpi Vertical pixels per inch.
 * @property rawYDpi Numerical vertical pixels per inch.
 * @property cameraInsetBounds Formatted string describing the cutout bounding box.
 * @property cameraRawCutout Detailed [CameraCutoutInfo] for precise positioning.
 * @property wideColorGamutSupport Indicates if the display supports wide color gamut.
 * @property hdrSupportedTypes List of supported HDR standards (HDR10, HLG, etc.).
 * @property maxLuminance Maximum brightness in nits.
 * @property minLuminance Minimum brightness in nits.
 * @property refreshRate Display refresh frequency in Hz.
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
    /**
     * Converts the screen information into a list of [InfoRowContent] for UI display.
     * 
     * @return A list of key-value pairs representing the display's specifications.
     */
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

    /**
     * Calculates the pixel bounds for a rectangle positioned relative to the camera lens.
     *
     * @param distanceFromCameraCm Vertical distance from lens center to rectangle center in cm.
     * @param widthCm Width of the rectangle in cm.
     * @param heightCm Height of the rectangle in cm.
     * @return A [android.graphics.Rect] containing the pixel coordinates.
     */
    fun calculateRectangleBounds(
        distanceFromCameraCm: Float,
        widthCm: Float,
        heightCm: Float
    ): android.graphics.Rect? {
        val cutout = cameraRawCutout ?: return null

        // 1. Convert CM to Pixels using raw DPI
        val distancePx = (distanceFromCameraCm * PhysicalConstants.CM_TO_INCH) * rawYDpi
        val widthPx = (widthCm * PhysicalConstants.CM_TO_INCH) * rawXDpi
        val heightPx = (heightCm * PhysicalConstants.CM_TO_INCH) * rawYDpi

        // 2. Position relative to lens center
        val centerX = cutout.centerX
        val centerY = cutout.centerY + distancePx

        return android.graphics.Rect(
            (centerX - widthPx / 2f).toInt(),
            (centerY - heightPx / 2f).toInt(),
            (centerX + widthPx / 2f).toInt(),
            (centerY + heightPx / 2f).toInt()
        )
    }
}
