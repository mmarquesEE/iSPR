package com.example.ispr.ui.widgets

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ispr.logic.camera.CameraHardwareInfo
import com.example.ispr.logic.camera.CameraHardwareManager
import com.example.ispr.logic.camera.CameraSettings

@Composable
fun CameraPreview(
	cameraHardwareManager: CameraHardwareManager,
	cameraHardwareInfo: CameraHardwareInfo?,
	modifier: Modifier = Modifier
) {
	var showOverlay by remember { mutableStateOf(false) }
	val settings by cameraHardwareManager.settings.collectAsState()
	
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(Color.Black),
		contentAlignment = Alignment.Center
	) {
		// 1. Camera Stream via SurfaceView
		// Restoring Aspect Ratio 1 to prevent distortion as per user feedback
		AndroidView(
			modifier = Modifier
				.aspectRatio(1f)
				.fillMaxHeight()
				.clipToBounds(),
			factory = { context ->
				SurfaceView(context).apply {
					holder.addCallback(object : SurfaceHolder.Callback {
						override fun surfaceCreated(holder: SurfaceHolder) {
							cameraHardwareManager.setPreviewSurface(holder.surface)
						}
						
						override fun surfaceChanged(
							holder: SurfaceHolder,
							format: Int,
							width: Int,
							height: Int
						) {
						}
						
						override fun surfaceDestroyed(holder: SurfaceHolder) {
							cameraHardwareManager.setPreviewSurface(null)
						}
					})
				}
			},
		)
		
		// 2. Settings Toggle Button
		IconButton(
			onClick = { showOverlay = !showOverlay },
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(16.dp)
				.background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small)
		) {
			Icon(
				imageVector = Icons.Default.Settings,
				contentDescription = "Camera Settings",
				tint = Color.White
			)
		}
		
		// 3. Transparent Overlay for Controls
		if (showOverlay) {
			CameraControlOverlay(
				settings = settings,
				onSettingsChange = {
					cameraHardwareManager.updateSettings(it)
				},
				hardwareInfo = cameraHardwareInfo,
				onClose = { showOverlay = false },
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraControlOverlay(
	settings: CameraSettings,
	onSettingsChange: (CameraSettings) -> Unit,
	hardwareInfo: CameraHardwareInfo?,
	onClose: () -> Unit,
	modifier: Modifier = Modifier
) {
	// Dimmed background that closes on click - Now contains the menu for a unified look
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(Color.Black.copy(alpha = 0.6f))
			.padding(top = 50.dp)
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = onClose
			)
	) {
		// Control Panel - Semi-transparent Surface to see preview behind it
		Column(
			modifier = Modifier
				.padding(16.dp)
				.fillMaxHeight()
				.verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.End,
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "Manual Control",
					style = MaterialTheme.typography.titleMedium,
					color = Color.White
				)
				Switch(
					checked = !settings.isAuto,
					onCheckedChange = { onSettingsChange(settings.copy(isAuto = !it)) }
				)
			}
			
			Spacer(modifier = Modifier.height(5.dp))
			
			if (!settings.isAuto && hardwareInfo != null) {
				// AE Mode Selection
				DropdownSelector(
					label = "AE Mode",
					items = hardwareInfo.rawAeModes,
					selectedItem = settings.aeMode,
					itemLabel = { mode ->
						hardwareInfo.autoExposureModes.getOrNull(
							hardwareInfo.rawAeModes.indexOf(
								mode
							)
						) ?: "Unknown"
					},
					onItemSelected = { onSettingsChange(settings.copy(aeMode = it)) }
				)
				
				// ISO Slider (Only if AE is OFF)
				if (settings.aeMode == 0) {
					hardwareInfo.rawIsoRange?.let { range ->
						LabeledSlider(
							label = "ISO",
							value = settings.iso.toFloat(),
							onValueChange = { onSettingsChange(settings.copy(iso = it.toInt())) },
							valueRange = range.lower.toFloat()..range.upper.toFloat(),
							numberFormat = "%.0f",
							enabled = true
						)
					}
					
					// Exposure Time Slider (converted to ms for readability)
					hardwareInfo.rawExposureRange?.let { range ->
						val currentMs = settings.exposureTimeNs / 1_000_000f
						val minMs = range.lower / 1_000_000f
						
						// Limit exposure time based on current FPS range to prevent FPS drop
						val maxExposureMsByFps =
							settings.fpsRange?.let { 1000f / it.upper } ?: 33.3f
						val maxMs = (range.upper / 1_000_000f).coerceAtMost(maxExposureMsByFps)
						
						LabeledSlider(
							label = "Exposure (ms)",
							value = currentMs.coerceIn(minMs, maxMs),
							onValueChange = { onSettingsChange(settings.copy(exposureTimeNs = (it * 1_000_000).toLong())) },
							valueRange = minMs..maxMs,
							numberFormat = "%.2f ms",
							enabled = true
						)
					}
				}
				
				// AF Mode Selection
				DropdownSelector(
					label = "AF Mode",
					items = hardwareInfo.rawAfModes,
					selectedItem = settings.afMode,
					itemLabel = { mode ->
						hardwareInfo.autoFocusModes.getOrNull(hardwareInfo.rawAfModes.indexOf(mode))
							?: "Unknown"
					},
					onItemSelected = { onSettingsChange(settings.copy(afMode = it)) }
				)
				
				// Focus Slider (Only if AF is OFF)
				val maxFocus = hardwareInfo.minFocusDistance ?: 0f
				if (settings.afMode == 0 && maxFocus > 0f) {
					LabeledSlider(
						label = "Lens Focus",
						value = settings.focusDistance,
						onValueChange = { onSettingsChange(settings.copy(focusDistance = it)) },
						valueRange = 0f..maxFocus, // 0.0 (infinity) to minFocusDistance (closest)
						steps = (maxFocus / 0.02f).toInt().coerceAtLeast(0),
						numberFormat = "%.2f dpt",
						enabled = true
					)
				}
				
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					// Resolution Selection
					DropdownSelector(
						label = "Resolution",
						items = hardwareInfo.supportedResolutions,
						selectedItem = settings.resolution,
						itemLabel = { "${it.width} x ${it.height}" },
						onItemSelected = { onSettingsChange(settings.copy(resolution = it)) },
						placeholder = hardwareInfo.maxResolution
					)
					
					// FPS Range Selection
					DropdownSelector(
						label = "FPS Range",
						items = hardwareInfo.getSupportedFpsRangesFor(settings.resolution),
						selectedItem = settings.fpsRange,
						itemLabel = { "${it.lower} - ${it.upper} FPS" },
						onItemSelected = { onSettingsChange(settings.copy(fpsRange = it)) }
					)
				}
			} else if (!settings.isAuto) {
				Text("Hardware info unavailable for manual control", color = Color.Gray)
			}
		}
		
	}
}
