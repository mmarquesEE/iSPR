package com.example.ispr.ui.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ispr.logic.screen.ScreenSourceConfiguration
import com.example.ispr.ui.widgets.LabeledSlider

@Composable
fun SourceConfigurationTab(
	sourceConfig: ScreenSourceConfiguration,
	onSourceConfigChange: (ScreenSourceConfiguration) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(16.dp)
	) {
		// --- Source (Screen Probe) Section ---
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.End
		) {
			Text(text = "Flat Source", style = MaterialTheme.typography.bodyMedium)
			Spacer(modifier = Modifier.width(8.dp))
			Switch(
				checked = sourceConfig.isFlatSource,
				onCheckedChange = { onSourceConfigChange(sourceConfig.copy(isFlatSource = it)) }
			)
		}
		
		Spacer(modifier = Modifier.height(8.dp))
		
		if (sourceConfig.isFlatSource) {
			LabeledSlider(
				label = "R",
				value = sourceConfig.red,
				onValueChange = { onSourceConfigChange(sourceConfig.copy(red = it)) },
				valueRange = 0f..1f,
				numberFormat = "%.2f"
			)
			LabeledSlider(
				label = "G",
				value = sourceConfig.green,
				onValueChange = { onSourceConfigChange(sourceConfig.copy(green = it)) },
				valueRange = 0f..1f,
				numberFormat = "%.2f"
			)
			LabeledSlider(
				label = "B",
				value = sourceConfig.blue,
				onValueChange = { onSourceConfigChange(sourceConfig.copy(blue = it)) },
				valueRange = 0f..1f,
				numberFormat = "%.2f"
			)
		} else {
			LabeledSlider(
				label = "Frame Rate",
				value = sourceConfig.frameRate,
				onValueChange = { onSourceConfigChange(sourceConfig.copy(frameRate = it)) },
				valueRange = 1f..120f,
				numberFormat = "%.0f fps"
			)
		}
	}
}
