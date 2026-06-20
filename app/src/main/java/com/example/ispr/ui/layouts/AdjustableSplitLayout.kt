package com.example.ispr.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun AdjustableSplitLayout(
    topContent: @Composable BoxScope.() -> Unit,
    bottomContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    initialTopFraction: Float = 0.5f
) {
    var topFraction by remember { mutableFloatStateOf(initialTopFraction) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeight = constraints.maxHeight.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Container - Height changes but top-left is fixed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(topFraction.coerceAtLeast(0.01f))
            ) {
                topContent()
            }

            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth().height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(7.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = MaterialTheme.shapes.small
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaFraction = dragAmount.y / maxHeight
                                topFraction = (topFraction + deltaFraction).coerceIn(0.1f, 0.9f)
                            }
                        }
                )
            }

            // Bottom Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight((1f - topFraction).coerceAtLeast(0.01f))
            ) {
                bottomContent()
            }
        }
    }
}
