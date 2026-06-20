package com.nature.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class TrackerInfo(val name: String, val category: String, val blocked: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyReportScreen(onBack: () -> Unit) {
    val trackers = remember {
        listOf(
            TrackerInfo("Google Analytics", "Tracking"),
            TrackerInfo("Facebook Pixel", "Advertising"),
            TrackerInfo("AdRoll", "Advertising"),
            TrackerInfo("Hotjar", "Analytics")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Strict Protection Active", style = MaterialTheme.typography.titleMedium)
                        Text("${trackers.size} trackers blocked on this page")
                    }
                }
            }

            LazyColumn {
                items(trackers) { tracker ->
                    ListItem(
                        headlineContent = { Text(tracker.name) },
                        supportingContent = { Text(tracker.category) },
                        trailingContent = {
                            Text("Blocked", color = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }
        }
    }
}
