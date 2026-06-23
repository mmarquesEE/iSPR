package com.example.ispr.logic.processing

/**
 * Parameters for the image processing pipeline and visualization.
 *
 * This class serves as the single source of truth for the image processing backend
 * and the UI rendering logic.
 *
 * @property minCol The starting column (X-coordinate) for the Region of Interest (ROI).
 * @property maxCol The ending column (X-coordinate) for the Region of Interest (ROI).
 * @property centerRowsHeight The vertical height of the processing band, centered on the image.
 * @property movingAverageWindow Number of samples used for smoothing the channel signals.
 * @property isRatiometric If true, signals are divided by a reference frame for relative measurements.
 * @property isTimeView If true, the graph displays a time-series of minimums rather than a spatial profile.
 * @property sampleRate Frequency (Hz) at which samples are recorded in Time View mode.
 * @property isRedEnabled Whether the Red channel signal is processed and displayed.
 * @property isGreenEnabled Whether the Green channel signal is processed and displayed.
 * @property isBlueEnabled Whether the Blue channel signal is processed and displayed.
 * @property isLive In Time View, controls whether the graph follows the latest incoming samples (true)
 *                 or displays a fixed historical window (false).
 * @property minTime The start time (in seconds relative to session start) of the visible window in History mode.
 * @property maxTime The end time (in seconds relative to session start) of the visible window in History mode.
 */
data class ProcessingParameters(
    val minCol: Int = 400,
    val maxCol: Int = 800,
    val centerRowsHeight: Int = 50,
    val movingAverageWindow: Int = 5,
    val isRatiometric: Boolean = false,
    val isTimeView: Boolean = false,
    val sampleRate: Float = 10f,
    val isRedEnabled: Boolean = true,
    val isGreenEnabled: Boolean = true,
    val isBlueEnabled: Boolean = true,
    val isLive: Boolean = true,
    val minTime: Float = 0f,
    val maxTime: Float = 0f
)
