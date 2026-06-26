package com.example.ispr.logic.processing

import android.media.Image
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChannelData(
	val chartData: IntArray,
	val minIndex: Int = 0,
	val maxIndex: Int = 0
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as ChannelData
		
		if (minIndex != other.minIndex) return false
		if (maxIndex != other.maxIndex) return false
		if (!chartData.contentEquals(other.chartData)) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = minIndex ?: 0
		result = 31 * result + (maxIndex ?: 0)
		result = 31 * result + chartData.contentHashCode()
		return result
	}
}

data class ProcessingResult(
	val rChannelData: ChannelData,
	val gChannelData: ChannelData,
	val bChannelData: ChannelData,
	val timeStamps: LongArray? = null,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as ProcessingResult
		
		if (rChannelData != other.rChannelData) return false
		if (gChannelData != other.gChannelData) return false
		if (bChannelData != other.bChannelData) return false
		if (!timeStamps.contentEquals(other.timeStamps)) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = rChannelData.hashCode()
		result = 31 * result + gChannelData.hashCode()
		result = 31 * result + bChannelData.hashCode()
		result = 31 * result + (timeStamps?.contentHashCode() ?: 0)
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
	
	private val liveTimeBufferR = mutableListOf<Int>()
	private val liveTimeBufferG = mutableListOf<Int>()
	private val liveTimeBufferB = mutableListOf<Int>()
	private val liveTimeStampsNs = mutableListOf<Long>()
	
	private val frozenTimeBufferR = mutableListOf<Int>()
	private val frozenTimeBufferG = mutableListOf<Int>()
	private val frozenTimeBufferB = mutableListOf<Int>()
	private val frozenTimeStampsNs = mutableListOf<Long>()
	
	private var lastRecordedTimestampNs: Long = 0L
	private var initialTimestampNs: Long = 0L
	
	
	fun updateParameters(newParams: ProcessingParameters) {
		if (newParams.isTimeView != params.isTimeView) {
			liveTimeBufferR.clear()
			liveTimeBufferG.clear()
			liveTimeBufferB.clear()
			liveTimeStampsNs.clear()
			initialTimestampNs = 0L
		}
		if (newParams.isLive != params.isLive) {
			frozenTimeBufferR.clear()
			frozenTimeBufferG.clear()
			frozenTimeBufferB.clear()
			frozenTimeStampsNs.clear()
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
					sumY += yBuffer.get(actualRow * yRowStride + actualCol * yPixelStride)
						.toInt() and 0xFF
					
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
			
			if (params.isRatiometric && listOf(rRef, gRef, bRef).any { it.isEmpty() }) {
				rRef = smoothedR.copyOf()
				gRef = smoothedG.copyOf()
				bRef = smoothedB.copyOf()
			} else if (!params.isRatiometric && listOf(rRef, gRef, bRef).any { it.isNotEmpty() }) {
				rRef = IntArray(0)
				gRef = IntArray(0)
				bRef = IntArray(0)
			}
			
			if (params.isRatiometric) {
				divide(smoothedR, rRef, dynamicBitShift)
				divide(smoothedG, gRef, dynamicBitShift)
				divide(smoothedB, bRef, dynamicBitShift)
			}
			
			val (minIndexR, maxIndexR) = findMinMax(smoothedR)
			val (minIndexG, maxIndexG) = findMinMax(smoothedG)
			val (minIndexB, maxIndexB) = findMinMax(smoothedB)
			
			if (params.isTimeView) {
				val minIntervalNs = 1000000000L / params.sampleRate.toLong()
				if (frameTimestampNs - lastRecordedTimestampNs >= minIntervalNs) {
					if (liveTimeStampsNs.isEmpty()) initialTimestampNs = frameTimestampNs
					
					liveTimeBufferR.add(minIndexR + minCol)
					liveTimeBufferG.add(minIndexG + minCol)
					liveTimeBufferB.add(minIndexB + minCol)
					liveTimeStampsNs.add(frameTimestampNs)
					lastRecordedTimestampNs = frameTimestampNs
					
					if (liveTimeBufferR.size > params.maxTimeBufferSize) {
						liveTimeBufferR.removeAt(0)
						liveTimeBufferG.removeAt(0)
						liveTimeBufferB.removeAt(0)
						liveTimeStampsNs.removeAt(0)
					}
				}
				
				if (
					!params.isLive &&
					liveTimeStampsNs.isNotEmpty() &&
					frozenTimeBufferR.isEmpty() &&
					frozenTimeBufferG.isEmpty() &&
					frozenTimeBufferB.isEmpty() &&
					frozenTimeStampsNs.isEmpty()
				) {
					frozenTimeBufferR.addAll(liveTimeBufferR
						.slice(params.minTimeIdx..params.maxTimeIdx))
					frozenTimeBufferG.addAll(liveTimeBufferG
						.slice(params.minTimeIdx..params.maxTimeIdx))
					frozenTimeBufferB.addAll(liveTimeBufferB
						.slice(params.minTimeIdx..params.maxTimeIdx))
					frozenTimeStampsNs.addAll(liveTimeStampsNs
						.slice(params.minTimeIdx..params.maxTimeIdx))
				}
				
				if (params.isLive) {
					_result.value = ProcessingResult(
						rChannelData = ChannelData(chartData = liveTimeBufferR.toIntArray()),
						gChannelData = ChannelData(chartData = liveTimeBufferG.toIntArray()),
						bChannelData = ChannelData(chartData = liveTimeBufferB.toIntArray()),
						timeStamps = liveTimeStampsNs.toLongArray(),
					)
				} else {
					_result.value = ProcessingResult(
						rChannelData = ChannelData(chartData = frozenTimeBufferR.toIntArray()),
						gChannelData = ChannelData(chartData = frozenTimeBufferG.toIntArray()),
						bChannelData = ChannelData(chartData = frozenTimeBufferB.toIntArray()),
						timeStamps = frozenTimeStampsNs.toLongArray(),
					)
				}
			} else {
				_result.value = ProcessingResult(
					rChannelData = ChannelData(
						chartData = smoothedR, minIndex = minIndexR, maxIndex = maxIndexR
					),
					gChannelData = ChannelData(
						chartData = smoothedG, minIndex = minIndexG, maxIndex = maxIndexG
					),
					bChannelData = ChannelData(
						chartData = smoothedB, minIndex = minIndexB, maxIndex = maxIndexB
					)
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
	
	private fun findMinMax(vector: IntArray): Pair<Int, Int> {
		if (vector.isEmpty()) return Pair(0, 0)
		
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
		return Pair(minIdx, maxIdx)
	}
}