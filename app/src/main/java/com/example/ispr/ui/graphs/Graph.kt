package com.example.ispr.ui.graphs


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ispr.logic.processing.ChannelData
import com.example.ispr.ui.theme.ISPRTheme
import java.util.Locale

/**
 * Graph.kt
 * This component performs the sensing data plotting using a Canvas.
 * It supports two main types of visualizations:
 * 1. **SPR Curve**: Displays the intensity across a range of pixels. Index filtering is
 *    typically not required here as ROI selection is handled by the logic layer.
 * 2. **Time Series**: Tracks resonance parameters (like minimum position) over time.
 *    In this mode, [timeIndexRange] can be used to filter the displayed time window.
 */

/**
 * A customizable Graph component for displaying SPR-related data.
 *
 * @param yData Variadic list of [ChannelData] containing the sensor readings for each channel.
 * @param colRange The primary axis range. For SPR curves, this defines the X-axis (pixel indices).
 *                 For time-series data, this defines the Y-axis (resonance values).
 * @param modifier [Modifier] to be applied to the Canvas.
 * @param timeData Optional array of timestamps (in nanoseconds) used for time-series plotting.
 * @param enabledChannels A list of booleans indicating which channels should be rendered.
 * @param colors A list of [Color]s to use for each respective channel's plot.
 */

@Composable
fun Graph(
	vararg yData: ChannelData,
	colRange: Pair<Int, Int>,
	modifier: Modifier = Modifier,
	timeData: LongArray? = null,
	enabledChannels: List<Boolean> = listOf(true, true, true),
	colors: List<Color> = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))
) {
	val axisColor = MaterialTheme.colorScheme.onBackground
	val textMeasurer = rememberTextMeasurer()
	val textStyle = MaterialTheme.typography.labelSmall.copy(
		fontSize = 10.sp,
		color = axisColor.copy(alpha = 0.9f)
	)
	
	val isTimeSeries = timeData != null
	
	val minX = if (isTimeSeries) timeData.first() else colRange.first.toLong()
	val maxX = if (isTimeSeries) timeData.last() else colRange.second.toLong()
	val xScale = maxX - minX
	
	val minY =
		if (isTimeSeries) colRange.first else yData.minOf { it.chartData[it.minIndex] }
	val maxY =
		if (isTimeSeries) colRange.second else yData.maxOf { it.chartData[it.maxIndex] }
	val yScale = maxY - minY
	
	val formatStringX = if (isTimeSeries) "%.1fs" else "%.0f"
	val formatStringY = if (!isTimeSeries) "%.1f" else "%.0f"
	
	val tickLineLength = 5.dp
	
	Canvas(modifier = modifier) {
		// X axis
		drawLine(
			color = axisColor,
			start = Offset(x = 0f, size.height), end = Offset(x = size.width, size.height)
		)
		
		// Y axis
		drawLine(
			color = axisColor,
			start = Offset(x = 0f, y = 0f), end = Offset(x = 0f, y = size.height)
		)
		
		// Graph Origin
		drawText(
			textMeasurer = textMeasurer,
			style = textStyle,
			text = String.format(
				Locale.US,
				format = "($formatStringX, $formatStringY)",
				minX / (if (isTimeSeries) 1000000000f else 1f),
				minY / (if (!isTimeSeries) 1f * maxY else 1f)
			),
			topLeft = Offset(x = 2.dp.toPx(), y = size.height - 18.dp.toPx())
		)
		
		// Tick drawing
		floatArrayOf(0.5f, 1f).forEach {
			val xTick = (it * xScale + minX) / (if (isTimeSeries) 1000000000f else 1f)
			val yTick = (it * yScale + minY) / (if (!isTimeSeries) 1f * maxY else 1f)
			
			val xTickText = String.format(Locale.US, formatStringX, xTick)
			val yTickText = String.format(Locale.US, formatStringY, yTick)
			
			val textSizeX = textMeasurer.measure(xTickText, textStyle).size
			val textSizeY = textMeasurer.measure(yTickText, textStyle).size
			
			// X ticks
			drawLine(
				color = axisColor,
				start = Offset(x = it * size.width, y = size.height),
				end = Offset(x = it * size.width, y = size.height - tickLineLength.toPx())
			)
			drawText(
				textMeasurer = textMeasurer,
				style = textStyle,
				text = xTickText,
				topLeft = Offset(
					x = it * (size.width - textSizeX.width),
					y = size.height - textSizeX.height - tickLineLength.toPx()
				)
			)
			
			// Y ticks
			drawLine(
				color = axisColor,
				start = Offset(x = 0f, y = (1f - it) * size.height),
				end = Offset(x = tickLineLength.toPx(), y = (1f - it) * size.height)
			)
			drawText(
				textMeasurer = textMeasurer,
				style = textStyle,
				text = yTickText,
				topLeft = Offset(
					x = tickLineLength.toPx() + 3.dp.toPx(),
					y = (1f - it) * (size.height - textSizeY.height)
				)
			)
		}
		
		yData.forEachIndexed { index, channelData ->
			if (enabledChannels[index]) {
				val arrSize = channelData.chartData.size
				val path = Path()
				path.moveTo(
					x = 0f,
					y = size.height * (
							(maxY - channelData.chartData.first()) / yScale.toFloat())
				)
				for (i in 1 until arrSize) {
					path.lineTo(
						x = size.width * (
                            if(isTimeSeries) (timeData[i] - minX) / xScale.toFloat()
                            else i / (arrSize - 1).toFloat()
                        ),
						y = size.height * (
								(maxY - channelData.chartData[i]) / yScale.toFloat())
					)
				}
				drawPath(path = path, color = colors[index], style = Stroke(width = 1.dp.toPx()))
				
				if (!isTimeSeries) {
					val xConst = size.width * (channelData.minIndex / (arrSize - 1f))
					
					drawLine(
						color = colors[index],
						start = Offset(x = xConst, y = 0f),
						end = Offset(x = xConst, y = size.height),
						pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
							intervals = floatArrayOf(10f, 10f)
						)
					)
				}
			}
		}
	}
}

@Preview(showBackground = false, widthDp = 400, heightDp = 300)
@Composable
fun GraphPreview() {
	// Mock data for the preview
	val timeData = LongArray(10) { it.toLong() * 1000000000L }
	val mockChannel = ChannelData(
		chartData = intArrayOf(20, 15, 30, 25, 40, 10, 35, 50, 45, 60),
		minIndex = 5,
		maxIndex = 9
	)
	
	ISPRTheme {
		Graph(
			yData = arrayOf(mockChannel),
            colRange = Pair(20, 60),
			modifier = Modifier.padding(16.dp),
		)
	}
}