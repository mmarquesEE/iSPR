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

            if (autoScale) {
                minY = minOf(
                    a = result.redVector.minOrNull() ?: 0f,
                    b = result.greenVector.minOrNull() ?: 0f,
                    c = result.blueVector.minOrNull() ?: 0f
                )
                maxY = maxOf(
                    a = result.redVector.maxOrNull() ?: 0f,
                    b = result.greenVector.maxOrNull() ?: 0f,
                    c = result.blueVector.maxOrNull() ?: 0f
                )
            } else {
                minY = 0f
                maxY = 1f
            }

            val rangeY = (maxY - minY).let { if (it <= 0) 1f else it }
            val yTicks = listOf(minY, minY + rangeY * 0.5f, maxY)

            xTicks.forEachIndexed { index, tick ->
                val x = marginL + (tick * graphW)
                val colIdx = result.columns.first + (tick * (result.columns.last - result.columns.first + 1)).toInt()
                val textXOffset = if (index == 0) 5.dp.toPx() else if (index == 2) -25.dp.toPx() else -10.dp.toPx()

                drawLine(
                    axisColor,
                    start = androidx.compose.ui.geometry.Offset(x, graphH),
                    end = androidx.compose.ui.geometry.Offset(x, graphH + 4.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = colIdx.toString(),
                    style = labelStyle,
                    topLeft = androidx.compose.ui.geometry.Offset(x + textXOffset, graphH + 5.dp.toPx())
                )
            }

            yTicks.forEach { tick ->
                val normalizedTick = (tick - minY) / rangeY
                val y = graphH - (normalizedTick * graphH)
                drawLine(axisColor,
                    start = androidx.compose.ui.geometry.Offset(marginL, y),
                    end = androidx.compose.ui.geometry.Offset(marginL + 4.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx()
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format(Locale.US, "%.2f", tick),
                    style = labelStyle,
                    topLeft = androidx.compose.ui.geometry.Offset(marginL + 7.dp.toPx(), y - 14.dp.toPx())
                )
            }

            // Draw Data Vectors
            if (params.isRedEnabled) {
                drawVector(
                    result.redVector, Color.Red,
                    width=graphW,
                    height = graphH,
                    offsetX = marginL,
                    minY, rangeY
                )
                drawMinMarkers(
                    result.minRedIndex, result.minRedValue, Color.Red,
                    vectorSize = result.redVector.size,
                    width = graphW,
                    height = graphH,
                    offsetX = marginL,
                    minY, rangeY
                )
            }
            if (params.isGreenEnabled) {
                drawVector(
                    result.greenVector, Color.Green,
                    width = graphW,
                    height = graphH,
                    offsetX = marginL, minY, rangeY
                )
                drawMinMarkers(
                    result.minGreenIndex, result.minGreenValue, Color.Green,
                    vectorSize = result.greenVector.size,
                    width = graphW, graphH,
                    offsetX = marginL, minY, rangeY
                )
            }
            if (params.isBlueEnabled) {
                drawVector(
                    result.blueVector, Color.Blue,
                    width = graphW,
                    height = graphH,
                    offsetX = marginL,
                    minY, rangeY
                )
                drawMinMarkers(
                    result.minBlueIndex, result.minBlueValue, Color.Blue,
                    vectorSize = result.blueVector.size,
                    width = graphW,
                    height = graphH,
                    offsetX = marginL,
                    minY, rangeY
                )
            }
        }
    }
}