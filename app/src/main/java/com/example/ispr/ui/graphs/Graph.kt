package com.example.ispr.ui.graphs


import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ispr.logic.processing.ChannelData
import java.util.Locale

@Composable
fun Graph(
    xData: LongArray,
    vararg yData: ChannelData,
    modifier: Modifier = Modifier,
    enabledChannels: List<Boolean> = listOf(true ,true, true),
    drawMinCursors: Boolean = false,
    normalizeYData: Boolean = false,
    isXDataTimestamp: Boolean = false,
    colors: List<Color> = listOf(
        Color(0xFFFF0000),
        Color(0xFF00FF00),
        Color(0xFF0000FF)
    )

) {
    val axisColor = MaterialTheme.colorScheme.onBackground
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = axisColor
    )

    val minX = xData.first().toFloat()
    val maxX = xData.last().toFloat()
    val xScale = maxX - minX

    val minY = yData.minOf { it.minCursor.second }.toFloat()
    val maxY = yData.maxOf { it.maxCursor.second }.toFloat()
    val yScale = maxY - minY

    Canvas(modifier = modifier) {
        // X axis
        drawLine(
            color = axisColor,
            start = Offset(x = 0f, 0f), end = Offset(x = size.width, 0f))

        // Y axis
        drawLine(
            color = axisColor,
            start = Offset(x = 0f, y = 0f), end = Offset(x = 0f, y = size.height))

        // Tick drawing
        floatArrayOf(0f, 0.5f, 1f).forEach {
            val xTick = (it * xScale + minX) / (if (isXDataTimestamp) 1000000000f else 1f)
            val yTick = ((1f - it) * yScale + minY) / (if (normalizeYData) 1f * maxY else 1f)

            val xTickText = String.format(
                Locale.US, format = if (isXDataTimestamp) "%.1fs" else "%.0f", xTick)

            val yTickText = String.format(
                Locale.US, format = if (normalizeYData) "%.1f" else "%.0f", yTick)

            val textSizeX = textMeasurer.measure(xTickText, textStyle).size.width
            val textSizeY = textMeasurer.measure(yTickText, textStyle).size.height

            // X ticks
            drawLine(
                color = axisColor,
                start = Offset(x = it * size.width, y = size.height),
                end = Offset(x = it * size.width, y = size.height + 5.dp.toPx())
            )
            drawText(
                textMeasurer = textMeasurer,
                text = xTickText,
                topLeft = Offset(
                    x = it * (size.width - textSizeX), y = size.height - 7.dp.toPx())
            )

            // Y ticks
            drawLine(
                color = axisColor,
                start = Offset(x = 0f, y = it * size.height),
                end = Offset(x = 5.dp.toPx(), y = it * size.height)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = yTickText,
                topLeft = Offset(x = 7.dp.toPx(), y = it * (size.height - textSizeY))
            )
        }

        yData.forEachIndexed { index, channelData ->
            if (enabledChannels[index]) {
                val arrSize = xData.size
                val path = Path()
                path.moveTo(
                    x = 0f,
                    y = size.height * (
                            ( maxY - channelData.chartData.first() ) / yScale )
                )
                for (i in 1 until arrSize) {
                    path.lineTo(
                        x = size.width * ((xData[i] - minX) / xScale),
                        y = size.height * (
                                ( maxY - channelData.chartData[i] ) / yScale )
                    )
                }
                drawPath(path = path, color = colors[index], style = Stroke(width = 2.dp.toPx()))

                if(drawMinCursors){
                    val xConst = size.width * (channelData.minCursor.first / (arrSize - 1) )
                    val yConst = size.height * (
                            ( maxY - channelData.minCursor.second ) / yScale )

                    drawLine(
                        color = colors[index],
                        start = Offset(x = 0f,y = yConst), end = Offset(x = size.width, y = yConst),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(10f, 10f))
                    )
                    drawLine(
                        color = colors[index],
                        start = Offset(x = xConst, y = 0f), end = Offset(x = xConst, y = size.height),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(10f, 10f))
                    )
                }
            }
        }
    }
}