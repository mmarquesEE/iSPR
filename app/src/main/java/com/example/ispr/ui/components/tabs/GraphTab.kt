package com.example.ispr.ui.components.tabs

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.ispr.ui.widgets.ColorCheckbox
import com.example.ispr.ui.widgets.LabeledSlider

@Composable
fun GraphTab(
    result: ProcessingResult?,
    params: ProcessingParameters,
    onParamsChange: (ProcessingParameters) -> Unit,
    activeResolution: android.util.Size? = null,
    cameraSettings: com.example.ispr.logic.camera.CameraSettings? = null
) {
    var isRedEnabled by remember { mutableStateOf(true) }
    var isGreenEnabled by remember { mutableStateOf(true) }
    var isBlueEnabled by remember { mutableStateOf(true) }

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
                    checked = isRedEnabled,
                    onCheckedChange = { isRedEnabled = it }
                )
                ColorCheckbox(
                    color = Color(0xFF00FF00),
                    checked = isGreenEnabled,
                    onCheckedChange = { isGreenEnabled = it }
                )
                ColorCheckbox(
                    color = Color(0xFF0000FF),
                    checked = isBlueEnabled,
                    onCheckedChange = { isBlueEnabled = it }
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

        val maxFps = cameraSettings?.fpsRange?.upper?.toFloat() ?: 60f
        val showGraphOverlay = remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ){
            /*Graph(
                result = result,
                params = params,
                autoScale = autoScale,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = { showGraphOverlay.value = true }
                    )
            )*/
            if (showGraphOverlay.value)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(0.5f))
                        .clickable(
                            onClick = { showGraphOverlay.value = false }
                        )
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ){
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 50.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ratiometric")
                            Switch(
                                checked = params.isRatiometric,
                                onCheckedChange = { onParamsChange(params.copy(isRatiometric = it)) }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 50.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Time View")
                            Switch(
                                checked = params.isTimeView,
                                onCheckedChange = { onParamsChange(params.copy(isTimeView = it, sampleRate = maxFps)) }
                            )
                        }
                        if (params.isTimeView)
                            LabeledSlider(
                                label = "Sample Rate (Hz)",
                                value = params.sampleRate,
                                onValueChange = { onParamsChange(params.copy(sampleRate = it)) },
                                valueRange = 1f..maxFps,
                                format = "%.1f"
                            )
                    }
                }
        }

        /*var rangeSliderVal by remember { mutableStateOf(0f..1f) }
        var minTime by remember { mutableFloatStateOf(0f) }
        var maxTime by remember { mutableFloatStateOf(0f) }

        LabeledRangeSlider(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterHorizontally),
            label = "",
            format = if (params.isTimeView) "%.1fs" else "%.0f",
            value = rangeSliderVal,
            valueRange = 0f..1f,
            onValueChange = { range ->

                rangeSliderVal = range

                if(!params.isTimeView) {
                    if (activeResolution != null)
                        onParamsChange(params.copy(
                            minCol = (range.start * activeResolution.width.toFloat()).toInt(),
                            maxCol = (range.endInclusive * activeResolution.width.toFloat()).toInt()
                        ))
                } else if (result != null && result.timeLabels != null) {
                    if (params.isLive) {
                        minTime = result.initialTimestampS
                        maxTime = result.timeLabels.max()
                        Log.d("Uepaa", String.format("%.1f, %.1f", minTime, maxTime))
                        onParamsChange(
                            params.copy(
                                isLive = false,
                                minTime = minTime,
                                maxTime = maxTime
                            )
                        )
                    } else if (range.start <= 0.05f && range.endInclusive >= 0.95f)
                        onParamsChange(
                            params.copy(
                                isLive = true
                            )
                        )
                    else
                        onParamsChange(params.copy(
                            minTime = range.start * minTime,
                            maxTime = range.endInclusive * maxTime
                        ))
                }
            }
        )*/

        Spacer(modifier = Modifier.height(20.dp))

        // Moving Average Window
        LabeledSlider(
            label = "Smoothing Window",
            value = params.movingAverageWindow.toFloat(),
            onValueChange = { onParamsChange(params.copy(movingAverageWindow = it.toInt())) },
            valueRange = 1f..20f,
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

        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary info
        if (result != null) {
            Text(
                text = "Min Indices: " +
                        "R:${result.rChannelData.minCursor.first} " +
                        "G:${result.gChannelData.minCursor.first} " +
                        "B:${result.bChannelData.minCursor.first}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
