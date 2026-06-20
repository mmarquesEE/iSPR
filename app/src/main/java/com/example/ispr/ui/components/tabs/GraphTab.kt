package com.example.ispr.ui.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ispr.logic.processing.ProcessingParameters
import com.example.ispr.logic.processing.ProcessingResult
import com.example.ispr.ui.graphs.Graph
import com.example.ispr.ui.widgets.ColorCheckbox
import com.example.ispr.ui.widgets.LabeledRangeSlider
import com.example.ispr.ui.widgets.LabeledSlider

@Composable
fun GraphTab(
    result: ProcessingResult?,
    params: ProcessingParameters,
    onParamsChange: (ProcessingParameters) -> Unit
) {
    var autoScale by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(0.5f),
            ){
                ColorCheckbox(
                    color = Color(0xFFFF0000),
                    checked = params.isRedEnabled,
                    onCheckedChange = { onParamsChange(params.copy(isRedEnabled = it)) }
                )
                ColorCheckbox(
                    color = Color(0xFF00FF00),
                    checked = params.isGreenEnabled,
                    onCheckedChange = { onParamsChange(params.copy(isGreenEnabled = it)) }
                )
                ColorCheckbox(
                    color = Color(0xFF0000FF),
                    checked = params.isBlueEnabled,
                    onCheckedChange = { onParamsChange(params.copy(isBlueEnabled = it)) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Auto-scale",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = autoScale,
                    onCheckedChange = { autoScale = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fixed height for graph to work well inside vertical scroll
        Graph(
            result = result,
            params = params,
            autoScale = autoScale,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ROI Column Range
        LabeledRangeSlider(
            label = "Column Range (ROI)",
            value = params.minCol.toFloat()..params.maxCol.toFloat(),
            onValueChange = { range ->
                onParamsChange(params.copy(minCol = range.start.toInt(), maxCol = range.endInclusive.toInt()))
            },
            valueRange = 0f..1280f, // Assuming standard max, should be dynamic if possible
            format = "%.0f"
        )

        // Processing Height
        LabeledSlider(
            label = "Processing Height",
            value = params.centerRowsHeight.toFloat(),
            onValueChange = { onParamsChange(params.copy(centerRowsHeight = it.toInt())) },
            valueRange = 1f..200f,
            format = "%.0f"
        )

        // Moving Average Window
        LabeledSlider(
            label = "Smoothing Window",
            value = params.movingAverageWindow.toFloat(),
            onValueChange = { onParamsChange(params.copy(movingAverageWindow = it.toInt())) },
            valueRange = 1f..20f,
            format = "%.0f"
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary info
        if (result != null) {
            Text(
                text = "Min Indices: R:${result.minRedIndex} G:${result.minGreenIndex} B:${result.minBlueIndex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
