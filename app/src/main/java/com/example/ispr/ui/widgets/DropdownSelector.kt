package com.example.ispr.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/**
 * A highly compact, reusable dropdown selector for camera configurations.
 */
@Composable
fun <T> DropdownSelector(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select..."
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = selectedItem?.let { itemLabel(it) } ?: placeholder

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        // wrapContentSize ensures the DropdownMenu anchors to the actual width of the selector
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            Surface(
                modifier = Modifier.widthIn(min = 140.dp, max = 220.dp),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                color = Color.Black.copy(alpha = 0.4f),
                onClick = { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            // Using standard DropdownMenu for maximum compatibility with custom layouts
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF2A2A2A))
                    .widthIn(min = 140.dp, max = 220.dp)
                    .heightIn(max = 200.dp) // Respect parent bounds by limiting height
            ) {
                if (items.isEmpty()) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = "No options available",
                                color = Color.Gray, 
                                style = MaterialTheme.typography.bodySmall 
                            ) 
                        },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = itemLabel(item),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                ) 
                            },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
