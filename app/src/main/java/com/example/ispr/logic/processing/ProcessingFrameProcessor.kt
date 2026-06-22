package com.example.ispr.logic.processing

import android.media.Image
import android.util.Log
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect


/**
 * Type of data contained in the ProcessingResult.
 */
enum class ResultType {
    RAW,
    RATIOMETRIC
}

/**
 * Result of the image processing pipeline.
 */
data class ProcessingResult(
    val redVector: FloatArray,
    val greenVector: FloatArray,
    val blueVector: FloatArray,
    val type: ResultType,
    val columns: IntRange,
    val minRedIndex: Int,
    val minRedValue: Float,
    val minGreenIndex: Int,
    val minGreenValue: Float,
    val minBlueIndex: Int,
    val minBlueValue: Float
)

/**
 * Handles real-time image processing on camera frames.
 * Extracts intensity profiles for R, G, and B channels within a specified ROI.
 */
class ProcessingFrameProcessor {
    private val TAG = "ProcessingFrameProcessor"

    private val _result = MutableStateFlow<ProcessingResult?>(null)
    val result = _result.asStateFlow()

    private var params = ProcessingParameters()
    
    private var refRed: FloatArray? = null
    private var refGreen: FloatArray? = null
    private var refBlue: FloatArray? = null

    private var shouldCaptureReference = false

    /**
     * Updates the processing parameters.
     */
    fun updateParameters(newParams: ProcessingParameters) {
        if (params != newParams) {
            params = newParams
            clearReference()
        }
    }

    /**
     * Requests a reference capture on the next processed frame.
     */
    fun captureReference() {
        shouldCaptureReference = true
    }

    /**
     * Clears current reference vectors.
     */
    fun clearReference() {
        refRed = null
        refGreen = null
        refBlue = null
    }

    /**
     * Processes a single YUV_420_888 frame.
     */
    fun processImage(image: Image) {
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
            for (i in 0 until colCount) {
                val actualCol = minCol + i
                var sumY = 0L
                var sumU = 0L
                var sumV = 0L

                for (j in 0 until rowCount) {
                    val actualRow = minRow + j

                    // Y plane
                    sumY += yBuffer.get(actualRow * yRowStride + actualCol * yPixelStride).toInt() and 0xFF

                    // U and V planes
                    val uvRow = actualRow / 2
                    val uvCol = actualCol / 2
                    val uvIndex = uvRow * uvRowStride + uvCol * uvPixelStride

                    sumU += uBuffer.get(uvIndex).toInt() and 0xFF
                    sumV += vBuffer.get(uvIndex).toInt() and 0xFF
                }

                // Average YUV for the column band
                val avgY = sumY.toDouble() / rowCount
                val avgU = sumU.toDouble() / rowCount
                val avgV = sumV.toDouble() / rowCount

                // 3. Convert aggregate YUV to RGB (Standard BT.601)
                val uNorm = avgU - 128.0
                val vNorm = avgV - 128.0

                rVector[i] = (avgY + 1.402 * vNorm).toFloat().coerceIn(0f, 255f)
                gVector[i] = (avgY - 0.344136 * uNorm - 0.714136 * vNorm).toFloat().coerceIn(0f, 255f)
                bVector[i] = (avgY + 1.772 * uNorm).toFloat().coerceIn(0f, 255f)
            }

            val smoothedR = applyMovingAverage(rVector, params.movingAvgSpaceWinSize)
            val smoothedG = applyMovingAverage(gVector, params.movingAvgSpaceWinSize)
            val smoothedB = applyMovingAverage(bVector, params.movingAvgSpaceWinSize)

            // 4. Reference Capture Logic
            if (shouldCaptureReference) {
                refRed = smoothedR.copyOf()
                refGreen = smoothedG.copyOf()
                refBlue = smoothedB.copyOf()
                shouldCaptureReference = false
            }

            // 5. Ratiometric vs RAW path
            val rRef = refRed
            val gRef = refGreen
            val bRef = refBlue

            val finalR: FloatArray
            val finalG: FloatArray
            val finalB: FloatArray
            val type: ResultType

            if (rRef != null && gRef != null && bRef != null) {
                finalR = divide(smoothedR, rRef)
                finalG = divide(smoothedG, gRef)
                finalB = divide(smoothedB, bRef)
                type = ResultType.RATIOMETRIC
            } else {
                finalR = smoothedR
                finalG = smoothedG
                finalB = smoothedB
                type = ResultType.RAW
            }

            normalize(finalR)
            normalize(finalG)
            normalize(finalB)

            // 6. Minimum Detection
            val (minIdxR, minValR) = findMin(finalR)
            val (minIdxG, minValG) = findMin(finalG)
            val (minIdxB, minValB) = findMin(finalB)

            _result.value = ProcessingResult(
                redVector = finalR,
                greenVector = finalG,
                blueVector = finalB,
                type = type,
                columns = minCol until maxCol,
                minRedIndex = minIdxR,
                minRedValue = minValR,
                minGreenIndex = minIdxG,
                minGreenValue = minValG,
                minBlueIndex = minIdxB,
                minBlueValue = minValB
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            image.close()
        }
    }

    private fun applyMovingAverage(input: FloatArray, window: Int): FloatArray {
        if (window <= 1) return input.copyOf()

        val n = input.size
        val output = FloatArray(n)
        val halfWindow = window / 2

        for (i in 0 until n) {
            var sum = 0f
            for (k in -halfWindow..halfWindow) {
                val index = (i + k).coerceIn(0, n - 1)
                sum += input[index]
            }
            output[i] = sum / (2 * halfWindow + 1)
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
    
    private fun divide(vectorA: FloatArray, vectorB: FloatArray): FloatArray {
        if (vectorA.isEmpty() || vectorB.isEmpty() || vectorA.size != vectorB.size)
            return vectorA.copyOf()
        
        val vectorC = FloatArray(vectorA.size)
        for (i in vectorA.indices) {
            val denom = vectorB[i]
            vectorC[i] = if (denom != 0f) vectorA[i] / denom else 1f
        }
        return vectorC
    }
}
