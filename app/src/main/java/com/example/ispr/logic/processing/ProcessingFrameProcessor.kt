package com.example.ispr.logic.processing

import android.media.Image
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encapsulates the output of a single processing cycle from the [ProcessingFrameProcessor].
 *
 * @property rChannelY Intensity profile (or time-series) for the Red channel.
 * @property gChannelY Intensity profile (or time-series) for the Green channel.
 * @property bChannelY Intensity profile (or time-series) for the Blue channel.
 * @property x The domain of the data (e.g., column indices in Profile mode, or point indices in Time-Series mode).
 * @property rCursorX The horizontal index of the detected feature (usually the minimum) for Red.
 * @property rCursorY The vertical intensity value at the detected feature for Red.
 * @property gCursorX The horizontal index of the detected feature (usually the minimum) for Green.
 * @property gCursorY The vertical intensity value at the detected feature for Green.
 * @property bCursorX The horizontal index of the detected feature (usually the minimum) for Blue.
 * @property bCursorY The vertical intensity value at the detected feature for Blue.
 * @property timeLabels Optional array of nanosecond timestamps corresponding to each data point.
 * @property initialTimestampS The reference timestamp (start of session) used for relative time calculations.
 */
data class ProcessingResult(
    val rChannelY: FloatArray,
    val gChannelY: FloatArray,
    val bChannelY: FloatArray,
    val x: IntRange,
    val rCursorX: Int,
    val rCursorY: Float,
    val gCursorX: Int,
    val gCursorY: Float,
    val bCursorX: Int,
    val bCursorY: Float,
    val timeLabels: FloatArray? = null,
    val initialTimestampS: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessingResult

        if (rCursorX != other.rCursorX) return false
        if (rCursorY != other.rCursorY) return false
        if (gCursorX != other.gCursorX) return false
        if (gCursorY != other.gCursorY) return false
        if (bCursorX != other.bCursorX) return false
        if (bCursorY != other.bCursorY) return false
        if (initialTimestampS != other.initialTimestampS) return false
        if (!rChannelY.contentEquals(other.rChannelY)) return false
        if (!gChannelY.contentEquals(other.gChannelY)) return false
        if (!bChannelY.contentEquals(other.bChannelY)) return false
        if (x != other.x) return false
        if (!timeLabels.contentEquals(other.timeLabels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rCursorX
        result = 31 * result + rCursorY.hashCode()
        result = 31 * result + gCursorX
        result = 31 * result + gCursorY.hashCode()
        result = 31 * result + bCursorX
        result = 31 * result + bCursorY.hashCode()
        result = 31 * result + initialTimestampS.hashCode()
        result = 31 * result + rChannelY.contentHashCode()
        result = 31 * result + gChannelY.contentHashCode()
        result = 31 * result + bChannelY.contentHashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + (timeLabels?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Handles real-time image processing on camera frames.
 *
 * This processor extracts intensity profiles for R, G, and B channels from YUV_420_888
 * image frames. It supports two main modes:
 * 1. **Profile Mode**: Analyzes a single frame to show the intensity distribution across
 *    a horizontal cross-section (ROI).
 * 2. **Time-Series Mode**: Tracks the minimum intensity point of each channel over time,
 *    useful for monitoring SPR (Surface Plasmon Resonance) shifts.
 *
 * Key features include:
 * - Ratiometric processing (normalization against a reference frame).
 * - Adjustable smoothing (moving average).
 * - Sub-sampling/Decimation for time-series data storage.
 * - Dynamic ROI (Region of Interest) selection.
 */
class ProcessingFrameProcessor {
    private val logTag = "ProcessingFrameProcessor"

    var onParametersChange: ((ProcessingParameters) -> Unit)? = null

    private val _result = MutableStateFlow<ProcessingResult?>(null)
    val result = _result.asStateFlow()

    private var rRef = FloatArray(0)
    private var gRef = FloatArray(0)
    private var bRef = FloatArray(0)

    private var params = ProcessingParameters()

    // Time-series buffers
    private val timeBufferR = mutableListOf<Float>()
    private val timeBufferG = mutableListOf<Float>()
    private val timeBufferB = mutableListOf<Float>()
    private val timeStamps = mutableListOf<Float>()
    private var lastRecordedTimestampS: Float = 0f
    private var initialTimestampS: Float = 0f

    private val matTimePoints = 500

    /**
     * Updates the processing parameters.
     */
    fun updateParameters(newParams: ProcessingParameters) {
        if (newParams.isTimeView != params.isTimeView) {
            timeBufferR.clear()
            timeBufferG.clear()
            timeBufferB.clear()
            timeStamps.clear()
            initialTimestampS = 0f
        }
        params = newParams
    }

    /**
     * Processes a single YUV_420_888 frame.
     */
    fun processImage(image: Image) {

        val frameTimestampS = image.timestamp / 1_000_000_000f

        try {
            val width = image.width
            val height = image.height

            // 1. Validate and clamp ROI (Horizontal crop)
            val minCol = params.minCol.coerceIn(0, width - 1)
            val maxCol = params.maxCol.coerceIn(minCol + 1, width)
            val colCount = maxCol - minCol

            // Center band (Vertical selection)
            val centerY = height / 2
            val halfHeight = params.centerRowsHeight / 2
            val minRow = (centerY - halfHeight).coerceIn(0, height - 1)
            val maxRow = (centerY + halfHeight).coerceIn(minRow + 1, height)
            val rowCount = maxRow - minRow

            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val yRowStride = planes[0].rowStride
            val yPixelStride = planes[0].pixelStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride

            val rVector = FloatArray(colCount)
            val gVector = FloatArray(colCount)
            val bVector = FloatArray(colCount)

            // 2. Aggregate intensities along rows (for each column)
            val rowCountInv = 1f / rowCount
            val uvRowCount = (rowCount + 1) / 2
            val uvRowCountInv = 1f / uvRowCount

            for (i in 0 until colCount) {
                val actualCol = minCol + i
                var sumY = 0
                var sumU = 0
                var sumV = 0

                val uvColOffset = (actualCol / 2) * uvPixelStride

                for (j in 0 until rowCount) {
                    val actualRow = minRow + j
                    sumY += yBuffer.get(actualRow * yRowStride + actualCol * yPixelStride).toInt() and 0xFF

                    if (j % 2 == 0) {
                        val uvIndex = (actualRow / 2) * uvRowStride + uvColOffset
                        sumU += uBuffer.get(uvIndex).toInt() and 0xFF
                        sumV += vBuffer.get(uvIndex).toInt() and 0xFF
                    }
                }

                // Average YUV for the column band using Float
                val avgY = sumY * rowCountInv
                val uNorm = (sumU * uvRowCountInv) - 128f
                val vNorm = (sumV * uvRowCountInv) - 128f

                // 3. Convert aggregate YUV to RGB (Standard BT.601) - Float optimized
                rVector[i] = (avgY + 1.402f * vNorm).coerceIn(0f, 255f)
                gVector[i] = (avgY - 0.344136f * uNorm - 0.714136f * vNorm).coerceIn(0f, 255f)
                bVector[i] = (avgY + 1.772f * uNorm).coerceIn(0f, 255f)
            }

            // 4. Moving Average (Centered with Padding)
            val smoothedR = applyMovingAverage(rVector, params.movingAverageWindow)
            val smoothedG = applyMovingAverage(gVector, params.movingAverageWindow)
            val smoothedB = applyMovingAverage(bVector, params.movingAverageWindow)

            if (params.isRatiometric && listOf(rRef, gRef, bRef).any { it.isEmpty() }){
                rRef = smoothedR.copyOf()
                gRef = smoothedG.copyOf()
                bRef = smoothedB.copyOf()
            } else if (!params.isRatiometric && listOf(rRef, gRef, bRef).any { it.isNotEmpty() }) {
                rRef = FloatArray(0)
                gRef = FloatArray(0)
                bRef = FloatArray(0)
            }

            if (params.isRatiometric){
                divide(smoothedR, rRef)
                divide(smoothedG, gRef)
                divide(smoothedB, bRef)
            }

            // 5. Normalization
            normalize(smoothedR)
            normalize(smoothedG)
            normalize(smoothedB)

            // 6. Minimum Detection
            val (minIdxR, minValR) = findMin(smoothedR)
            val (minIdxG, minValG) = findMin(smoothedG)
            val (minIdxB, minValB) = findMin(smoothedB)

            if (params.isTimeView) {
                // Time-series mode: collect cursors over time with decimation
                val minIntervalS = (1f / params.sampleRate)
                if (frameTimestampS - lastRecordedTimestampS >= minIntervalS) {
                    if (timeStamps.isEmpty()) initialTimestampS = frameTimestampS
                    
                    timeBufferR.add(minIdxR.toFloat())
                    timeBufferG.add(minIdxG.toFloat())
                    timeBufferB.add(minIdxB.toFloat())
                    timeStamps.add(frameTimestampS)
                    lastRecordedTimestampS = frameTimestampS

                    if (timeBufferR.size > matTimePoints) {
                        timeBufferR.removeAt(0)
                        timeBufferG.removeAt(0)
                        timeBufferB.removeAt(0)
                        timeStamps.removeAt(0)
                    }
                }

                // Roll-off detection: If viewing history that is no longer in buffer, force live mode
                if (!params.isLive && timeStamps.isNotEmpty()) {
                    val bufferStartTime = (timeStamps.first() - initialTimestampS)
                    if (params.maxTime < bufferStartTime) {
                        onParametersChange?.invoke(params.copy(isLive = true))
                    }
                }

                _result.value = ProcessingResult(
                    rChannelY = timeBufferR.toFloatArray(),
                    gChannelY = timeBufferG.toFloatArray(),
                    bChannelY = timeBufferB.toFloatArray(),
                    x = 0 until timeBufferR.size,
                    rCursorX = timeBufferR.size - 1,
                    rCursorY = minValR,
                    gCursorX = timeBufferG.size - 1,
                    gCursorY = minValG,
                    bCursorX = timeBufferB.size - 1,
                    bCursorY = minValB,
                    timeLabels = timeStamps.toFloatArray(),
                    initialTimestampS = initialTimestampS
                )
            } else {
                // Profile mode (default)
                _result.value = ProcessingResult(
                    rChannelY = smoothedR,
                    gChannelY = smoothedG,
                    bChannelY = smoothedB,
                    x = minCol until maxCol,
                    rCursorX = minIdxR,
                    rCursorY = minValR,
                    gCursorX = minIdxG,
                    gCursorY = minValG,
                    bCursorX = minIdxB,
                    bCursorY = minValB
                )
            }

        } catch (e: Exception) {
            Log.e(logTag, "Error processing image", e)
        } finally {
            image.close()
        }
    }

    private fun divide(a: FloatArray, b: FloatArray) {
        if (a.size != b.size) throw IllegalArgumentException("Arrays must have the same length")
        for (i in a.indices) {
            a[i] /= b[i]
        }
    }

    private fun applyMovingAverage(input: FloatArray, window: Int): FloatArray {
        val n = input.size
        if (window <= 1 || n == 0) return input.copyOf()

        val output = FloatArray(n)
        val halfWindow = window / 2
        val windowSize = 2 * halfWindow + 1
        val invWindowSize = 1f / windowSize

        var currentSum = 0f
        // Initialize first window
        for (k in -halfWindow..halfWindow) {
            currentSum += input[k.coerceIn(0, n - 1)]
        }
        output[0] = currentSum * invWindowSize

        for (i in 1 until n) {
            val oldIdx = (i - halfWindow - 1).coerceIn(0, n - 1)
            val newIdx = (i + halfWindow).coerceIn(0, n - 1)
            currentSum = currentSum - input[oldIdx] + input[newIdx]
            output[i] = currentSum * invWindowSize
        }
        return output
    }

    private fun normalize(vector: FloatArray) {
        val max = vector.maxOrNull() ?: return
        if (max > 0) {
            for (i in vector.indices) {
                vector[i] /= max
            }
        }
    }

    private fun findMin(vector: FloatArray): Pair<Int, Float> {
        if (vector.isEmpty()) return Pair(0, 0f)
        var minIdx = 0
        var minVal = vector[0]
        for (i in 1 until vector.size) {
            if (vector[i] < minVal) {
                minVal = vector[i]
                minIdx = i
            }
        }
        return Pair(minIdx, minVal)
    }
}
