package com.example.ispr.logic.screen

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Display
import android.view.SurfaceView
import android.view.WindowManager
import java.util.Locale

/**
 * Aggregator for all screen-related hardware features.
 * Provides access to display metrics, brightness control, and low-level pixel controlled areas.
 */
class ScreenHardwareManager(private val context: Context) {

    private var brightnessManager: ScreenBrightnessManager? = null
    private var activeControlledArea: ScreenHardwareControlledArea? = null

    /**
     * Initializes the brightness control for a specific activity.
     */
    fun initializeBrightness(activity: Activity) {
        brightnessManager = ScreenBrightnessManager(activity).apply {
            synchronizeWithSystem()
        }
    }

    /**
     * Toggles the high-intensity mode (100% backlight with UI dimming).
     */
    fun setHighIntensityMode(enabled: Boolean) {
        if (enabled) {
            brightnessManager?.setMaxWindowBrightness()
        } else {
            brightnessManager?.restoreSystemBrightness()
        }
    }

    /**
     * Returns the alpha value for the UI dimming overlay.
     */
    fun getUiDimmingAlpha(): Float {
        return brightnessManager?.getUiDimmingAlpha() ?: 0f
    }

    /**
     * Attaches a direct hardware controlled area to the provided SurfaceView.
     */
    fun attachControlledArea(surfaceView: SurfaceView) {
        activeControlledArea = ScreenHardwareControlledArea(surfaceView)
    }

    /**
     * Updates the physical bounds of the active controlled area.
     */
    fun updateControlledAreaBounds(bounds: Rect) {
        activeControlledArea?.updateBounds(bounds)
    }

    /**
     * Updates the color/intensity of the active controlled area.
     */
    fun updateControlledAreaColor(color: Int) {
        activeControlledArea?.updateColor(color)
    }

    /**
     * Formats a float value to a string with a specific unit using US Locale.
     */
    private fun formatTechValue(value: Float, unit: String, decimals: Int = 2): String {
        return String.format(Locale.US, "%.${decimals}f %s", value, unit)
    }

    /**
     * Queries the system for the most up-to-date screen hardware information.
     */
    fun getScreenInfo(): ScreenHardwareInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = context.display

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        // Camera Inset info extraction
        var insetBounds = "not provided"
        var rawCutout: CameraCutoutInfo? = null
        val cutout = windowManager.currentWindowMetrics.windowInsets.displayCutout

        cutout?.boundingRects?.firstOrNull()?.let { rect ->
            insetBounds = "L:${rect.left} px, T:${rect.top} px, W:${rect.width()} px, H:${rect.height()} px"
            rawCutout = CameraCutoutInfo(rect.left, rect.top, rect.width(), rect.height())
        }

        // Color & HDR capability detection
        val wcg = display.isWideColorGamut.toString()

        val hdrTypes = display.mode.supportedHdrTypes.map {
            when (it) {
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                else -> "Unknown"
            }
        }

        @Suppress("DEPRECATION")
        val hdrCapabilities = display.hdrCapabilities
        val maxLum = hdrCapabilities?.desiredMaxLuminance?.let {
            if (it > 0) formatTechValue(it, "nits", 1) else "not provided"
        } ?: "not provided"
        val minLum = hdrCapabilities?.desiredMinLuminance?.let {
            if (it > 0) formatTechValue(it, "nits", 3) else "not provided"
        } ?: "not provided"

        return ScreenHardwareInfo(
            resolution = "${metrics.widthPixels} x ${metrics.heightPixels} px",
            densityDpi = "${metrics.densityDpi} dpi",
            xDpi = formatTechValue(metrics.xdpi, "dpi"),
            rawXDpi = metrics.xdpi,
            yDpi = formatTechValue(metrics.ydpi, "dpi"),
            rawYDpi = metrics.ydpi,
            cameraInsetBounds = insetBounds,
            cameraRawCutout = rawCutout,
            wideColorGamutSupport = wcg,
            hdrSupportedTypes = hdrTypes,
            maxLuminance = maxLum,
            minLuminance = minLum,
            refreshRate = formatTechValue(display.refreshRate, "Hz", 1)
        )
    }
}
