package com.example.ispr.ui.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ispr.logic.processing.ProcessingParameters
import com.example.ispr.logic.processing.ProcessingResult
import java.util.Locale

@Composable
fun Graph(
    result: ProcessingResult?,
    modifier: Modifier = Modifier,
    params: ProcessingParameters,
    autoScale: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
    val axisColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val marginL = 5.dp.toPx()
        val marginB = 20.dp.toPx()
        val graphW = size.width - marginL
        val graphH = size.height - marginB

        // Draw background grid/axes
        // Y-Axis
        drawLine(
            axisColor,
            start = androidx.compose.ui.geometry.Offset(marginL, 0f),
            end = androidx.compose.ui.geometry.Offset(marginL, graphH),
            strokeWidth = 1.dp.toPx()
        )
        // X-Axis
        drawLine(
            axisColor,
            start = androidx.compose.ui.geometry.Offset(marginL, graphH),
            end = androidx.compose.ui.geometry.Offset(size.width, graphH),
            strokeWidth = 1.dp.toPx()
        )

        if (result != null) {
            val xTicks = listOf(0f, 0.5f, 1f)

            val minY: Float
            val maxY: Float

            if (autoScale || result.isTimeView) {
                val rMin = if (params.isRedEnabled) result.RChannelY.minOrNull() ?: Float.MAX_VALUE else Float.MAX_VALUE
                val gMin = if (params.isGreenEnabled) result.GChannelY.minOrNull() ?: Float.MAX_VALUE else Float.MAX_VALUE
                val bMin = if (params.isBlueEnabled) result.BChannelY.minOrNull() ?: Float.MAX_VALUE else Float.MAX_VALUE
                
                val rMax = if (params.isRedEnabled) result.RChannelY.maxOrNull() ?: Float.MIN_VALUE else Float.MIN_VALUE
                val gMax = if (params.isGreenEnabled) result.GChannelY.maxOrNull() ?: Float.MIN_VALUE else Float.MIN_VALUE
                val bMax = if (params.isBlueEnabled) result.BChannelY.maxOrNull() ?: Float.MIN_VALUE else Float.MIN_VALUE

                minY = minOf(rMin, gMin, bMin).let { if (it == Float.MAX_VALUE) 0f else it }
                maxY = maxOf(rMax, gMax, bMax).let { if (it == Float.MIN_VALUE) 1f else it }
            } else {
                minY = 0f
                maxY = 1f
            }

            val rangeY = (maxY - minY).let { if (it <= 0) 1f else it }
            val yTicks = listOf(minY, minY + rangeY * 0.5f, maxY)

            xTicks.forEachIndexed { index, tick ->
                val x = marginL + (tick * graphW)
                
                val label = if (result.isTimeView && result.timeLabels != null && result.timeLabels.isNotEmpty()) {
                    val firstTs = result.timeLabels.first()
                    val lastTs = result.timeLabels.last()
                    val currentTs = firstTs + (tick * (lastTs - firstTs)).toLong()
                    String.format(Locale.US, "%.1fs", (currentTs - firstTs) / 1_000_000_000.0)
                } else {
                    val colIdx = result.X.first + (tick * (result.X.last - result.X.first)).toInt()
                    colIdx.toString()
                }
                
                val textXOffset = if (index == 0) 5.dp.toPx() else if (index == 2) -35.dp.toPx() else -15.dp.toPx()

                drawLine(
                    axisColor,
                    start = androidx.compose.ui.geometry.Offset(x, graphH),
                    end = androidx.compose.ui.geometry.Offset(x, graphH - 5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                if (index > 0)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = labelStyle,
                        topLeft = androidx.compose.ui.geometry.Offset(x + textXOffset, graphH - 20.dp.toPx())
                    )
            }

            yTicks.forEach { tick ->
                val normalizedTick = (tick - minY) / rangeY
                val y = graphH - (normalizedTick * graphH)
                val textYOffset = if (tick == minY) -15.dp.toPx() else if (tick == maxY) 0.dp.toPx() else -7.dp.toPx()

                drawLine(axisColor,
                    start = androidx.compose.ui.geometry.Offset(marginL, y),
                    end = androidx.compose.ui.geometry.Offset(marginL + 4.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx()
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format(Locale.US, "%.2f", tick),
                    style = labelStyle,
                    topLeft = androidx.compose.ui.geometry.Offset(marginL + 7.dp.toPx(), y + textYOffset)
                )
            }

            // Draw Data Vectors
            if (params.isRedEnabled) {
                drawVector(
                    result.RChannelY, Color.Red,
                    width=graphW,
                    height = graphH,
                    offsetX = marginL,
                    minY, rangeY
                )
                if (!result.isTimeView) {
                    drawMinMarkers(
                        result.RCursorX, result.RCursorY, Color.Red,
                        vectorSize = result.RChannelY.size,
                        width = graphW,
                        height = graphH,
                        offsetX = marginL,
                        minY, rangeY
                    )
                }
            }
            if (params.isGreenEnabled) {
                drawVector(
                    result.GChannelY, Color.Green,
                    width = graphW,
                    height = graphH,
                    offsetX = marginL, minY, rangeY
                )
                if (!result.isTimeView) {
                    drawMinMarkers(
                        result.GCursorX, result.GCursorY, Color.Green,
                        vectorSize = result.GChannelY.size,
                        width = graphW, graphH,
                        offsetX = marginL, minY, rangeY
                    )
                }
            }
            if (params.isBlueEnabled) {
                drawVector(
                    result.BChannelY, Color.Blue,
                    width = graphW,
                    height = graphH,
                    offsetX = marginL,
                    minY, rangeY
                )
                if (!result.isTimeView) {
                    drawMinMarkers(
                        result.BCursorX, result.BCursorY, Color.Blue,
                        vectorSize = result.BChannelY.size,
                        width = graphW,
                        height = graphH,
                        offsetX = marginL,
                        minY, rangeY
                    )
                }
            }
        }
    }
}