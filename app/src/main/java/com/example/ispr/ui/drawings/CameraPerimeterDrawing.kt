package com.example.ispr.ui.drawings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ispr.logic.screen.CameraCutoutInfo

@Composable
fun CameraPerimeterDrawing(
    cutout: CameraCutoutInfo?,
    modifier: Modifier = Modifier
) {
    if (cutout == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val lensDiameter = minOf(cutout.width, cutout.height)
        val radius = (lensDiameter / 2f) + 4f

        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(cutout.centerX - radius, cutout.centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
