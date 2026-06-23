package com.example.ispr.logic.screen

import android.app.Activity
import android.view.WindowManager

/**
 * Manages window-level brightness overrides and calculates dimming factors
 * to maintain UI consistency while driving the screen to high-luminance states.
 */
class ScreenBrightnessManager(private val activity: Activity) {
    /**
     * Ramps the window brightness to maximum. 
     * This affects the physical backlight/O-LED voltage globally for this window.
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
}
