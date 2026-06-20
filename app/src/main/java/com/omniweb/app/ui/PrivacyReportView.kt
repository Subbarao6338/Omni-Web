package com.omniweb.app.ui

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyReportView(
    blockedTrackers: List<String>,
    onBack: () -> Unit
) {
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
                        Text("Protection Active", style = MaterialTheme.typography.titleMedium)
                        Text("${blockedTrackers.size} trackers/ads blocked on this page")
                    }
                }
            }

            if (blockedTrackers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trackers detected on this page.")
                }
            } else {
                LazyColumn {
                    items(blockedTrackers) { tracker ->
                        val parts = tracker.split(" ", limit = 2)
                        val category = parts.getOrNull(0) ?: "Unknown"
                        val domain = parts.getOrNull(1) ?: tracker

                        ListItem(
                            headlineContent = { Text(domain) },
                            supportingContent = { Text(category) },
                            trailingContent = {
                                Text("Blocked", color = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
            }
        }
    }
}
