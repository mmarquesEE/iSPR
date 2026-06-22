package com.example.ispr.ui.widgets

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
    var settings by remember { mutableStateOf(CameraSettings()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 1. Camera Stream via SurfaceView
        AndroidView(
            modifier = Modifier
                .fillMaxHeight()
                .clipToBounds()
                .aspectRatio(1f),
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            cameraHardwareManager.setPreviewSurface(holder.surface)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int, width: Int, height: Int
                        ) {}

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
                .background(
                    Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small
                )
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
                    settings = it
                    cameraHardwareManager.updateSettings(it)
                },
                hardwareInfo = cameraHardwareInfo,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * Overlay UI for manual camera hardware adjustments.
 */
@Composable
fun CameraControlOverlay(
    settings: CameraSettings,
    onSettingsChange: (CameraSettings) -> Unit,
    hardwareInfo: CameraHardwareInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .padding(top = 64.dp) // Offset for the settings button
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

        Spacer(modifier = Modifier.height(16.dp))

        if (!settings.isAuto && hardwareInfo != null) {
            // ISO Slider
            hardwareInfo.rawIsoRange?.let { range ->
                LabeledSlider(
                    label = "ISO",
                    value = settings.iso.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(iso = it.toInt())) },
                    valueRange = range.lower.toFloat()..range.upper.toFloat(),
                    format = "%.0f",
                    enabled = true
                )
            }

            // Exposure Time Slider (converted to ms for readability)
            hardwareInfo.rawExposureRange?.let { range ->
                val currentMs = settings.exposureTimeNs / 1_000_000f
                val minMs = range.lower / 1_000_000f
                val maxMs = (range.upper / 1_000_000f).coerceAtMost(100f) // Cap at 100ms for slider usability

                LabeledSlider(
                    label = "Exposure (ms)",
                    value = currentMs,
                    onValueChange = { onSettingsChange(settings.copy(exposureTimeNs = (it * 1_000_000).toLong())) },
                    valueRange = minMs..maxMs,
                    format = "%.2f ms",
                    enabled = true
                )
            }
        } else if (!settings.isAuto) {
            Text("Hardware info unavailable for manual control", color = Color.Gray)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
