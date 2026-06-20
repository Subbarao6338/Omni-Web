package com.nature.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun VideoSpeedController(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Popup(onDismissRequest = onDismiss, alignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FAF8).copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Flow Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2A9D8F)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        SpeedButton(
                            speed = speed,
                            isSelected = speed == currentSpeed,
                            onClick = { onSpeedChange(speed) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Return to Stream", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SpeedButton(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (isSelected) Color(0xFF2A9D8F) else Color.White.copy(alpha = 0.6f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${speed}x",
            color = if (isSelected) Color.White else Color(0xFF2A9D8F),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
