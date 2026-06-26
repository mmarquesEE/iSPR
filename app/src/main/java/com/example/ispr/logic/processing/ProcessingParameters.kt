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
 * @property isLive In Time View, controls whether the graph follows the latest incoming samples (true)
 *                 or displays a fixed historical window (false).
 * @property maxTimeBufferSize The maximum number of samples to buffer in Time View mode.
 * @property minTimeIdx The starting index for the manual time window selection.
 * @property maxTimeIdx The ending index for the manual time window selection.
 */
data class ProcessingParameters(
	val minCol: Int = 400,
	val maxCol: Int = 800,
	val centerRowsHeight: Int = 50,
	val movingAverageWindow: Int = 5,
	val isRatiometric: Boolean = false,
	val isTimeView: Boolean = false,
	val sampleRate: Float = 10f,
	val isLive: Boolean = true,
	val maxTimeBufferSize: Int = 500,
	
	val minTimeIdx: Int = 0,
	val maxTimeIdx: Int = maxTimeBufferSize - 1
)
