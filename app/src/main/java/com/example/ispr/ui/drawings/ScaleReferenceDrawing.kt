package com.example.ispr.ui.drawings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ispr.logic.screen.PhysicalConstants.CM_TO_INCH

@Composable
fun ScaleReferenceDrawing(
    xDpi: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    // Guard against invalid or missing hardware information
    if (xDpi <= 0f) return

    val density = LocalDensity.current

    // Calculate 1 centimeter in physical pixels using the centralized constant
    val pxInCm = xDpi * CM_TO_INCH

    // Transform physical pixels into device-independent pixels (Dp) for rendering
    val dpInCm = with(density) { pxInCm.toDp() }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .width(dpInCm)
                .height(8.dp)
        ) {
            val width = size.width
            val height = size.height
            val strokeWidth = 1.dp.toPx()
            val tickHeight = 6.dp.toPx()

            // Horizontal reference line
            drawLine(
                color = color,
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Vertical boundary ticks (brackets)
            drawLine(
                color = color,
                start = Offset(0f, (height - tickHeight) / 2),
                end = Offset(0f, (height + tickHeight) / 2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = color,
                start = Offset(width, (height - tickHeight) / 2),
                end = Offset(width, (height + tickHeight) / 2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "1 cm",
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}
