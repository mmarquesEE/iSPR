package com.example.ispr.logic.processing

/**
 * Parameters for the image processing pipeline in the Pulse tab.
 */
data class ProcessingParameters(
    val minCol: Int = 400,
    val maxCol: Int = 800,
    val centerRowsHeight: Int = 50,
    val movingAvgSpaceWinSize: Int = 5,
    val movingAvgTimeWinSize: Int = 5
)
