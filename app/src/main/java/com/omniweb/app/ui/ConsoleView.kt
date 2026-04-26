package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var filterLevel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("ALL") }
    val filteredLogs = if (filterLevel == "ALL") logs else logs.filter { it.level == filterLevel }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            ScrollableTabRow(
                selectedTabIndex = when(filterLevel) {
                    "ALL" -> 0
                    "LOG" -> 1
                    "WARN" -> 2
                    "ERROR" -> 3
                    else -> 0
                },
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White,
                edgePadding = 16.dp
            ) {
                Tab(selected = filterLevel == "ALL", onClick = { filterLevel = "ALL" }, text = { Text("All") })
                Tab(selected = filterLevel == "LOG", onClick = { filterLevel = "LOG" }, text = { Text("Log") })
                Tab(selected = filterLevel == "WARN", onClick = { filterLevel = "WARN" }, text = { Text("Warn") })
                Tab(selected = filterLevel == "ERROR", onClick = { filterLevel = "ERROR" }, text = { Text("Error") })
            }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(filteredLogs) { log ->
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
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
        }
    }
}
