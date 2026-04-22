package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ConsoleLog(val message: String, val level: String, val timestamp: Long = System.currentTimeMillis())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleView(logs: List<ConsoleLog>, onClear: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            items(logs) { log ->
                Text(
                    text = "[${log.level}] ${log.message}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = TextStyle(
                        color = when(log.level) {
                            "ERROR" -> Color.Red
                            "WARN" -> Color.Yellow
                            else -> Color.Green
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
                Divider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}
