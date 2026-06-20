package com.example.ispr.logic.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * UI State for the Source Configuration Tab.
 */
data class ScreenSourceConfiguration(
    val isFlatSource: Boolean = true,
    val red: Float = 1.0f,
    val green: Float = 1.0f,
    val blue: Float = 1.0f,
    val frameRate: Float = 30f
) {
    /**
     * Converts the R, G, B floats (0..1) to an ARGB integer.
     */
    fun toArgbColor(): Int {
        return Color(red, green, blue, 1.0f).toArgb()
    }
}
