package com.nature.browser.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SourceViewer(source: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        Text(
            "Page Source",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                text = source,
                color = Color(0xFFCE9178),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Close")
        }
    }
}
