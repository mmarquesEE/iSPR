package com.example.ispr.ui.components.tabs

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
import com.example.ispr.ui.graphs.Graph
import com.example.ispr.ui.widgets.ColorCheckbox
import com.example.ispr.ui.widgets.LabeledRangeSlider
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
	
	var normalizeGraph by remember { mutableStateOf(false) }
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
			) {
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
					text = "Normalized",
					style = MaterialTheme.typography.labelMedium,
					modifier = Modifier.padding(end = 8.dp)
				)
				Switch(
					checked = normalizeGraph,
					onCheckedChange = { normalizeGraph = it }
				)
			}
		}
		
		Spacer(modifier = Modifier.height(16.dp))
		
		val maxFps = cameraSettings?.fpsRange?.upper?.toFloat() ?: 60f
		var showGraphOverlay by remember { mutableStateOf(false) }
		
		var timeIdxSel by remember { mutableStateOf(0f..1f) }
		var colRangeSel by remember { mutableStateOf(0f..1f) }
		
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp)
		) {
			if (result != null) {
				Graph(
					result.rChannelData,
					result.gChannelData,
					result.bChannelData,
					colRange = Pair(params.minCol, params.maxCol),
					enabledChannels = listOf(isRedEnabled, isGreenEnabled, isBlueEnabled),
					timeData = result.timeStamps,
					modifier = Modifier.fillMaxSize()
				)
			}
			if (showGraphOverlay)
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(MaterialTheme.colorScheme.background.copy(0.5f))
						.clickable(
							onClick = { showGraphOverlay = false }
						)
				) {
					Column(
						modifier = Modifier
							.fillMaxSize(),
						verticalArrangement = Arrangement.Top
					) {
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
								onCheckedChange = {
									onParamsChange(params.copy(isRatiometric = it))
								}
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
								onCheckedChange = {
									onParamsChange(
										params.copy(isTimeView = it, sampleRate = maxFps)
									)
								}
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
		
		LabeledRangeSlider(
			value = if (params.isTimeView) timeIdxSel else colRangeSel,
			onValueChange = {
				if (!params.isTimeView) {
					val nCols = params.maxCol - params.minCol
					onParamsChange(
						params.copy(
							minCol = (it.start * (activeResolution?.width ?: nCols)).toInt(),
							maxCol = (it.endInclusive * (activeResolution?.width ?: nCols)).toInt()
						)
					)
					colRangeSel = it
				} else {
					if (!params.isLive) {
						if (it.start < 0.05f || it.endInclusive > 0.95f) {
							onParamsChange(
								params.copy(
									isLive = true,
									minTimeIdx = 0,
									maxTimeIdx = params.maxTimeBufferSize - 1
								)
							)
							timeIdxSel = 0f..1f
						} else {
							onParamsChange(
								params.copy(
									minTimeIdx = (it.start * (params.maxTimeBufferSize - 1)).toInt(),
									maxTimeIdx = (it.endInclusive * (params.maxTimeBufferSize - 1)).toInt()
								)
							)
							timeIdxSel = it
						}
					} else {
						onParamsChange(
							params.copy(
								isLive = false,
								minTimeIdx = (0.1f * (params.maxTimeBufferSize - 1)).toInt(),
								maxTimeIdx = (0.9f * (params.maxTimeBufferSize - 1)).toInt()
							)
						)
						timeIdxSel = 0.1f..0.9f
					}
				}
				
			},
		)
		
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
						"R:${result.rChannelData.minIndex} " +
						"G:${result.gChannelData.minIndex} " +
						"B:${result.bChannelData.minIndex}",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}
