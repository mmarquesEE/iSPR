package com.example.ispr.ui.components.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ispr.logic.camera.CameraHardwareInfo
import com.example.ispr.logic.screen.ScreenHardwareInfo
import com.example.ispr.ui.widgets.InfoBlock
import com.example.ispr.ui.widgets.InfoBlockData

@Composable
fun InfoTab(
    cameraInfo: CameraHardwareInfo?,
    screenInfo: ScreenHardwareInfo
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Display",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoBlock(listOf(
            InfoBlockData(
                title = "Display",
                info = screenInfo.toInfoRowContentList()
            )
        ))

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Camera Info Section
        Text(
            text = "Front Camera",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (cameraInfo != null) {
            InfoBlock(listOf(
                InfoBlockData(
                    title = "Camera",
                    info = cameraInfo.toInfoRowContentList()
                )
            ))
        } else {
            Text(
                text = "Camera information not available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
