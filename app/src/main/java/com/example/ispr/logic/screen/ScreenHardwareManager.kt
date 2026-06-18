package com.example.ispr.logic.screen

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import java.util.Locale

class ScreenHardwareManager(private val context: Context) {

    /**
     * Formats a float value to a string with a specific unit using US Locale.
     */
    fun formatTechValue(value: Float, unit: String, decimals: Int = 2): String {
        return String.format(Locale.US, "%.${decimals}f %s", value, unit)
    }

    /**
     * Queries the system for the most up-to-date screen hardware information.
     */
    fun getScreenInfo(): com.example.ispr.logic.screen.ScreenHardwareInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = context.display

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        // Camera Inset info extraction
        var insetBounds = "not provided"
        var rawCutout: com.example.ispr.logic.screen.CameraCutoutInfo? = null
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
