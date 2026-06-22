package com.example.ispr.ui.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale


data class GraphData(
    val color: Color,
    val enabled: Boolean,
    val data: FloatArray,
    val dataMin: Float,
    val dataMinIndex: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphData

        if (enabled != other.enabled) return false
        if (dataMin != other.dataMin) return false
        if (dataMinIndex != other.dataMinIndex) return false
        if (color != other.color) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + dataMin.hashCode()
        result = 31 * result + dataMinIndex
        result = 31 * result + color.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@Composable
fun Graph(
    graphs: List<GraphData>?,
    modifier: Modifier = Modifier,
    isTimeSeries: Boolean = false,
    indexes: IntRange,
    autoScale: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit
) {
    var showOverlay by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
    val axisColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)

    Box(
        modifier = modifier.clickable { showOverlay = true }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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

            if (graphs != null) {
                val xTicks = listOf(0f, 0.5f, 1f)

                val minY: Float
                val maxY: Float

                if (autoScale) {
                    minY = graphs.minOfOrNull { it.data.minOrNull()?: 0f } ?: 0f
                    maxY = graphs.maxOfOrNull { it.data.maxOrNull()?: 0f } ?: 0f
                } else {
                    minY = 0f
                    maxY = 1f
                }

                val rangeY = (maxY - minY).let { if (it <= 0) 1f else it }
                val yTicks = listOf(minY, minY + rangeY * 0.5f, maxY)

                xTicks.forEachIndexed { index, tick ->
                    val x = marginL + (tick * graphW)
                    val colIdx = indexes.first + (tick * (indexes.last - indexes.first + 1)).toInt()
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
                    drawLine(
                        axisColor,
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

                graphs.forEach {
                    if (it.enabled) {
                        drawVector(
                            it.data, it.color,
                            width = graphW,
                            height = graphH,
                            offsetX = marginL,
                            minY, rangeY
                        )
                        if(!isTimeSeries) {
                            drawMinMarkers(
                                it.dataMinIndex, it.dataMin, it.color,
                                vectorSize = it.data.size,
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

        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = true, onClick = { showOverlay = false}) // Consume clicks
            ) {
                IconButton(
                    onClick = { showOverlay = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close overlay",
                        tint = Color.White
                    )
                }
                overlay()
            }
        }
    }
}

/*

// Reference Controls
Row(
    modifier = Modifier
        .align(Alignment.Center)
        .background(
            color = Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        )
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text(
        text = "RATIOMETRIC MODE",
        color = Color.White,
        style = MaterialTheme.typography.labelLarge
    )
    Switch(
        checked = result?.type == ResultType.RATIOMETRIC,
        onCheckedChange = onReferenceToggle
    )
}
* */