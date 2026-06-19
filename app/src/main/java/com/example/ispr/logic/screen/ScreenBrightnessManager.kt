package com.example.ispr.logic.screen

import android.app.Activity
import android.provider.Settings
import android.view.WindowManager

/**
 * Manages window-level brightness overrides and calculates dimming factors
 * to maintain UI consistency while driving the screen to high-luminance states.
 */
class ScreenBrightnessManager(private val activity: Activity) {

    private var originalSystemBrightness: Int = 127 // Default mid-range
    
    /**
     * Captures the current system brightness to use as a baseline for UI dimming.
     */
    fun synchronizeWithSystem() {
        try {
            originalSystemBrightness = Settings.System.getInt(
                activity.contentResolver, 
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Settings.SettingNotFoundException) {
            originalSystemBrightness = 127
        }
    }

    /**
     * Ramps the window brightness to maximum. 
     * This affects the physical backlight/OLED voltage globally for this window.
     */
    fun setMaxWindowBrightness() {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        activity.window.attributes = layoutParams
    }

    /**
     * Restores the window brightness to follow system settings.
     */
    fun restoreSystemBrightness() {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = layoutParams
    }

    /**
     * Calculates the alpha value for a dimming overlay to make a 100% bright screen
     * appear as if it had the [originalSystemBrightness].
     * 
     * Formula: 1.0 - (original / max)
     */
    fun getUiDimmingAlpha(): Float {
        val ratio = originalSystemBrightness / 255f
        return (1.0f - ratio).coerceIn(0f, 1f)
    }
}
