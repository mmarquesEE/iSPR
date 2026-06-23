package com.example.ispr.logic.processing

import android.media.Image
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Result of the image processing pipeline.
 */
data class ProcessingResult(
    val RChannelY: FloatArray,
    val GChannelY: FloatArray,
    val BChannelY: FloatArray,
    val X: IntRange,
    val RCursorX: Int,
    val RCursorY: Float,
    val GCursorX: Int,
    val GCursorY: Float,
    val BCursorX: Int,
    val BCursorY: Float,
    val isTimeView: Boolean = false,
    val timeLabels: LongArray? = null,
    val initialTimestampNs: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessingResult

        if (RCursorX != other.RCursorX) return false
        if (RCursorY != other.RCursorY) return false
        if (GCursorX != other.GCursorX) return false
        if (GCursorY != other.GCursorY) return false
        if (BCursorX != other.BCursorX) return false
        if (BCursorY != other.BCursorY) return false
        if (isTimeView != other.isTimeView) return false
        if (initialTimestampNs != other.initialTimestampNs) return false
        if (!RChannelY.contentEquals(other.RChannelY)) return false
        if (!GChannelY.contentEquals(other.GChannelY)) return false
        if (!BChannelY.contentEquals(other.BChannelY)) return false
        if (X != other.X) return false
        if (!timeLabels.contentEquals(other.timeLabels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = RCursorX
        result = 31 * result + RCursorY.hashCode()
        result = 31 * result + GCursorX
        result = 31 * result + GCursorY.hashCode()
        result = 31 * result + BCursorX
        result = 31 * result + BCursorY.hashCode()
        result = 31 * result + isTimeView.hashCode()
        result = 31 * result + initialTimestampNs.hashCode()
        result = 31 * result + RChannelY.contentHashCode()
        result = 31 * result + GChannelY.contentHashCode()
        result = 31 * result + BChannelY.contentHashCode()
        result = 31 * result + X.hashCode()
        result = 31 * result + (timeLabels?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Handles real-time image processing on camera frames.
 * Extracts intensity profiles for R, G, and B channels within a specified ROI.
 */
class ProcessingFrameProcessor {
    private val TAG = "ProcessingFrameProcessor"

    var onParametersChange: ((ProcessingParameters) -> Unit)? = null

    private val _result = MutableStateFlow<ProcessingResult?>(null)
    val result = _result.asStateFlow()

    private var Rref = FloatArray(0)
    private var Gref = FloatArray(0)
    private var Bref = FloatArray(0)

    private var lastFrameTimeNs: Long = 0


    private var params = ProcessingParameters()

    // Time-series buffers
    private val timeBufferR = mutableListOf<Float>()
    private val timeBufferG = mutableListOf<Float>()
    private val timeBufferB = mutableListOf<Float>()
    private val timeStamps = mutableListOf<Long>()
    private var lastRecordedTimestampNs: Long = 0
    private var initialTimestampNs: Long = 0

    private val MAX_TIME_POINTS = 500

    /**
     * Updates the processing parameters.
     */
    fun updateParameters(newParams: ProcessingParameters) {
        if (newParams.isTimeView != params.isTimeView) {
            timeBufferR.clear()
            timeBufferG.clear()
            timeBufferB.clear()
            timeStamps.clear()
            initialTimestampNs = 0
        }
        params = newParams
    }

    /**
     * Processes a single YUV_420_888 frame.
     */
    fun processImage(image: Image) {
        val startTime = System.nanoTime()
        val frameTimestamp = image.timestamp
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

            if (params.isRatiometric && listOf(Rref, Gref, Bref).any { it.isEmpty() }){
                Rref = smoothedR.copyOf()
                Gref = smoothedG.copyOf()
                Bref = smoothedB.copyOf()
            } else if (!params.isRatiometric && listOf(Rref, Gref, Bref).any { it.isNotEmpty() }) {
                Rref = FloatArray(0)
                Gref = FloatArray(0)
                Bref = FloatArray(0)
            }

            if (params.isRatiometric){
                divide(smoothedR, Rref)
                divide(smoothedG, Gref)
                divide(smoothedB, Bref)
            }

            // 5. Normalization
            normalize(smoothedR)
            normalize(smoothedG)
            normalize(smoothedB)

            // 6. Minimum Detection
            val (minIdxR, minValR) = findMin(smoothedR)
            val (minIdxG, minValG) = findMin(smoothedG)
            val (minIdxB, minValB) = findMin(smoothedB)

            val currentTime = System.nanoTime()
            val processingTimeMs = (currentTime - startTime) / 1_000_000.0
            if (lastFrameTimeNs > 0) {
                val hz = 1_000_000_000.0 / (currentTime - lastFrameTimeNs)
                Log.d(TAG, String.format(java.util.Locale.US, "Rate: %.2f Hz | Proc: %.2f ms", hz, processingTimeMs))
            }
            lastFrameTimeNs = currentTime

            if (params.isTimeView) {
                // Time-series mode: collect cursors over time with decimation
                val minIntervalNs = (1_000_000_000L / params.sampleRate).toLong()
                if (frameTimestamp - lastRecordedTimestampNs >= minIntervalNs) {
                    if (timeStamps.isEmpty()) initialTimestampNs = frameTimestamp
                    
                    timeBufferR.add(minValR)
                    timeBufferG.add(minValG)
                    timeBufferB.add(minValB)
                    timeStamps.add(frameTimestamp)
                    lastRecordedTimestampNs = frameTimestamp

                    if (timeBufferR.size > MAX_TIME_POINTS) {
                        timeBufferR.removeAt(0)
                        timeBufferG.removeAt(0)
                        timeBufferB.removeAt(0)
                        timeStamps.removeAt(0)
                    }
                }

                // Roll-off detection: If viewing history that is no longer in buffer, force live mode
                if (!params.isLive && timeStamps.isNotEmpty()) {
                    val bufferStartTime = (timeStamps.first() - initialTimestampNs) / 1_000_000_000f
                    if (params.maxTime < bufferStartTime) {
                        onParametersChange?.invoke(params.copy(isLive = true))
                    }
                }

                _result.value = ProcessingResult(
                    RChannelY = timeBufferR.toFloatArray(),
                    GChannelY = timeBufferG.toFloatArray(),
                    BChannelY = timeBufferB.toFloatArray(),
                    X = 0 until timeBufferR.size,
                    RCursorX = timeBufferR.size - 1,
                    RCursorY = minValR,
                    GCursorX = timeBufferG.size - 1,
                    GCursorY = minValG,
                    BCursorX = timeBufferB.size - 1,
                    BCursorY = minValB,
                    isTimeView = true,
                    timeLabels = timeStamps.toLongArray(),
                    initialTimestampNs = initialTimestampNs
                )
            } else {
                // Profile mode (default)
                _result.value = ProcessingResult(
                    RChannelY = smoothedR,
                    GChannelY = smoothedG,
                    BChannelY = smoothedB,
                    X = minCol until maxCol,
                    RCursorX = minIdxR,
                    RCursorY = minValR,
                    GCursorX = minIdxG,
                    GCursorY = minValG,
                    BCursorX = minIdxB,
                    BCursorY = minValB,
                    isTimeView = false
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
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
