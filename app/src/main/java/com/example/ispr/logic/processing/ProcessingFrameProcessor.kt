package com.example.ispr.logic.processing

import android.media.Image
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChannelData(
    val chartData: IntArray,
    val minCursor: Pair<Long, Int>,
    val maxCursor: Pair<Long, Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelData

        if (!chartData.contentEquals(other.chartData)) return false
        if (minCursor != other.minCursor) return false
        if (maxCursor != other.maxCursor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chartData.contentHashCode()
        result = 31 * result + minCursor.hashCode()
        result = 31 * result + maxCursor.hashCode()
        return result
    }
}

data class ProcessingResult(
    val rChannelData: ChannelData,
    val gChannelData: ChannelData,
    val bChannelData: ChannelData,
    val x: LongArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessingResult

        if (rChannelData != other.rChannelData) return false
        if (gChannelData != other.gChannelData) return false
        if (bChannelData != other.bChannelData) return false
        if (!x.contentEquals(other.x)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rChannelData.hashCode()
        result = 31 * result + gChannelData.hashCode()
        result = 31 * result + bChannelData.hashCode()
        result = 31 * result + x.contentHashCode()
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

    private var rRef = IntArray(0)
    private var gRef = IntArray(0)
    private var bRef = IntArray(0)

    private var params = ProcessingParameters()

    private val timeBufferR = mutableListOf<Int>()
    private val timeBufferG = mutableListOf<Int>()
    private val timeBufferB = mutableListOf<Int>()
    private val timeStampsNs = mutableListOf<Long>()
    private var lastRecordedTimestampNs: Long = 0L
    private var initialTimestampNs: Long = 0L

    private val matTimePoints = 500

    fun updateParameters(newParams: ProcessingParameters) {
        if (newParams.isTimeView != params.isTimeView) {
            timeBufferR.clear()
            timeBufferG.clear()
            timeBufferB.clear()
            timeStampsNs.clear()
            initialTimestampNs = 0L
        }
        params = newParams
    }

    fun processImage(image: Image) {
        val frameTimestampNs = image.timestamp

        try {
            val width = image.width
            val height = image.height

            val minCol = params.minCol.coerceIn(0, width - 1)
            val maxCol = params.maxCol.coerceIn(minCol + 1, width)
            val colCount = maxCol - minCol

            // Center band calculations optimized with bit shifts where applicable
            val centerY = height shr 1
            val halfHeight = params.centerRowsHeight shr 1
            val minRow = (centerY - halfHeight).coerceIn(0, height - 1)
            val maxRow = (centerY + halfHeight).coerceIn(minRow + 1, height)

            // Force rowCount to be even to eliminate fractional row scaling
            var rowCount = (maxRow - minRow) and -2

            if (rowCount <= 0) rowCount = 1

            // Dynamically calculate the maximum safe bit shift factor for this rowCount
            val maxAbsValue = 255 * rowCount
            val dynamicBitShift = Integer.numberOfLeadingZeros(maxAbsValue) - 1

            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val yRowStride = planes[0].rowStride
            val yPixelStride = planes[0].pixelStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride

            val rVector = IntArray(colCount)
            val gVector = IntArray(colCount)
            val bVector = IntArray(colCount)

            for (i in 0 until colCount) {
                val actualCol = minCol + i
                var sumY = 0
                var sumU = 0
                var sumV = 0

                // Divide actualCol by 2 via right shift for UV subsampling offset
                val uvColOffset = (actualCol shr 1) * uvPixelStride

                for (j in 0 until rowCount) {
                    val actualRow = minRow + j
                    sumY += yBuffer.get(actualRow * yRowStride + actualCol * yPixelStride).toInt() and 0xFF

                    if (j and 1 == 0) {
                        val uvIndex = (actualRow shr 1) * uvRowStride + uvColOffset
                        sumU += uBuffer.get(uvIndex).toInt() and 0xFF
                        sumV += vBuffer.get(uvIndex).toInt() and 0xFF
                    }
                }

                // Double the U/V sums via a bit shift left to perfectly match the full scale of sumY
                val explicitSumU = sumU shl 1
                val explicitSumV = sumV shl 1

                val shiftOffsetUV = 128 * rowCount
                val uNorm = explicitSumU - shiftOffsetUV
                val vNorm = explicitSumV - shiftOffsetUV

                // Integer BT.601 matrix applied directly with bit shifts (shr 10 scales by 1/1024)
                rVector[i] = (sumY + ((1436 * vNorm) shr 10)).coerceIn(0, maxAbsValue)
                gVector[i] = (sumY - ((352 * uNorm + 731 * vNorm) shr 10)).coerceIn(0, maxAbsValue)
                bVector[i] = (sumY + ((1815 * uNorm) shr 10)).coerceIn(0, maxAbsValue)
            }

            val smoothedR = applyMovingAverage(rVector, params.movingAverageWindow)
            val smoothedG = applyMovingAverage(gVector, params.movingAverageWindow)
            val smoothedB = applyMovingAverage(bVector, params.movingAverageWindow)

            if (params.isRatiometric && listOf(rRef, gRef, bRef).any { it.isEmpty() }){
                rRef = smoothedR.copyOf()
                gRef = smoothedG.copyOf()
                bRef = smoothedB.copyOf()
            } else if (!params.isRatiometric && listOf(rRef, gRef, bRef).any { it.isNotEmpty() }) {
                rRef = IntArray(0)
                gRef = IntArray(0)
                bRef = IntArray(0)
            }

            if (params.isRatiometric){
                divide(smoothedR, rRef, dynamicBitShift)
                divide(smoothedG, gRef, dynamicBitShift)
                divide(smoothedB, bRef, dynamicBitShift)
            }

            val (minR, maxR) = findMinMax(smoothedR)
            val (minG, maxG) = findMinMax(smoothedG)
            val (minB, maxB) = findMinMax(smoothedB)

            if (params.isTimeView) {
                val minIntervalNs = 1000000000L / params.sampleRate.toLong()
                if (frameTimestampNs - lastRecordedTimestampNs >= minIntervalNs) {
                    if (timeStampsNs.isEmpty()) initialTimestampNs = frameTimestampNs

                    timeBufferR.add(minR.first)
                    timeBufferG.add(minG.first)
                    timeBufferB.add(minB.first)
                    timeStampsNs.add(frameTimestampNs)
                    lastRecordedTimestampNs = frameTimestampNs

                    if (timeBufferR.size > matTimePoints) {
                        timeBufferR.removeAt(0)
                        timeBufferG.removeAt(0)
                        timeBufferB.removeAt(0)
                        timeStampsNs.removeAt(0)
                    }
                }

                if (!params.isLive && timeStampsNs.isNotEmpty()) {
                    if (params.maxTime < timeStampsNs.first()) {
                        onParametersChange?.invoke(params.copy(isLive = true))
                    }
                }

                _result.value = ProcessingResult(
                    rChannelData = ChannelData(
                        chartData = timeBufferR.toIntArray(),
                        minCursor = Pair(lastRecordedTimestampNs, minR.first),
                        maxCursor = Pair(lastRecordedTimestampNs, maxR.first)
                    ),
                    gChannelData = ChannelData(
                        chartData = timeBufferG.toIntArray(),
                        minCursor = Pair(lastRecordedTimestampNs, minG.first),
                        maxCursor = Pair(lastRecordedTimestampNs, maxG.first)
                    ),
                    bChannelData = ChannelData(
                        chartData = timeBufferB.toIntArray(),
                        minCursor = Pair(lastRecordedTimestampNs, minB.first),
                        maxCursor = Pair(lastRecordedTimestampNs, maxB.first)
                    ),
                    x = timeStampsNs.toLongArray(),
                )
            } else {
                _result.value = ProcessingResult(
                    rChannelData = ChannelData(
                        chartData = smoothedR,
                        minCursor = Pair(minR.first.toLong(), minR.second),
                        maxCursor = Pair(maxR.first.toLong(), maxR.second)
                    ),
                    gChannelData = ChannelData(
                        chartData = smoothedG,
                        minCursor = Pair(minG.first.toLong(), minG.second),
                        maxCursor = Pair(maxG.first.toLong(), maxG.second)
                    ),
                    bChannelData = ChannelData(
                        chartData = smoothedB,
                        minCursor = Pair(minB.first.toLong(), minB.second),
                        maxCursor = Pair(maxB.first.toLong(), maxB.second)
                    ),
                    x = (minCol until maxCol).map { it.toLong() }.toLongArray()
                )
            }

        } catch (e: Exception) {
            Log.e(logTag, "Error processing image", e)
        } finally {
            image.close()
        }
    }

    private fun divide(a: IntArray, b: IntArray, bitShift: Int) {
        if (a.size != b.size) throw IllegalArgumentException("Arrays must have the same length")
        for (i in a.indices) {
            if (b[i] != 0) {
                a[i] = (a[i] shl bitShift) / b[i]
            } else {
                a[i] = 0
            }
        }
    }

    private fun applyMovingAverage(input: IntArray, window: Int): IntArray {
        val n = input.size
        if (window <= 1 || n == 0) return input.copyOf()

        val output = IntArray(n)
        val halfWindow = window shr 1 // Optimized with bit-shift
        val windowSize = (halfWindow shl 1) + 1 // Optimized with bit-shift

        var currentSum = 0
        for (k in -halfWindow..halfWindow) {
            currentSum += input[k.coerceIn(0, n - 1)]
        }
        output[0] = currentSum / windowSize

        for (i in 1 until n) {
            val oldIdx = (i - halfWindow - 1).coerceIn(0, n - 1)
            val newIdx = (i + halfWindow).coerceIn(0, n - 1)
            currentSum = currentSum - input[oldIdx] + input[newIdx]
            output[i] = currentSum / windowSize
        }
        return output
    }

    private fun findMinMax(vector: IntArray): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        if (vector.isEmpty()) return Pair(Pair(0, 0), Pair(0, 0))

        var minIdx = 0
        var minVal = vector[0]
        var maxIdx = 0
        var maxVal = vector[0]

        for (i in 1 until vector.size) {
            val value = vector[i]
            if (value < minVal) {
                minVal = value
                minIdx = i
            }
            if (value > maxVal) {
                maxVal = value
                maxIdx = i
            }
        }
        return Pair(Pair(minIdx, minVal), Pair(maxIdx, maxVal))
    }
}