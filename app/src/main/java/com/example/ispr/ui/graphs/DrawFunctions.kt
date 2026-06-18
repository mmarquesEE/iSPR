package com.example.ispr.ui.graphs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMinMarkers(
    index: Int,
    value: Float,
    color: Color,
    vectorSize: Int,
    width: Float,
    height: Float,
    offsetX: Float,
    minY: Float,
    rangeY: Float
) {
    if (vectorSize < 2) return
    val dx = width / (vectorSize - 1)
    val x = offsetX + (index * dx)
    val y = height - (((value - minY) / rangeY) * height)

    val markerColor = color.copy(alpha = 0.5f)
    val strokeWidth = 1.dp.toPx()

    // Horizontal line
    drawLine(
        color = markerColor,
        start = androidx.compose.ui.geometry.Offset(offsetX, y),
        end = androidx.compose.ui.geometry.Offset(offsetX + width, y),
        strokeWidth = strokeWidth,
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )

    // Vertical line
    drawLine(
        color = markerColor,
        start = androidx.compose.ui.geometry.Offset(x, 0f),
        end = androidx.compose.ui.geometry.Offset(x, height),
        strokeWidth = strokeWidth,
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVector(
    vector: FloatArray,
    color: Color,
    width: Float,
    height: Float,
    offsetX: Float,
    minY: Float = 0f,
    rangeY: Float = 1f
) {
    if (vector.size < 2) return

    val path = Path()
    val dx = width / (vector.size - 1)

    fun normalize(v: Float) = (v - minY) / rangeY

    path.moveTo(offsetX, height - (normalize(vector[0]) * height))

    for (i in 1 until vector.size) {
        path.lineTo(offsetX + (i * dx), height - (normalize(vector[i]) * height))
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx())
    )
}
