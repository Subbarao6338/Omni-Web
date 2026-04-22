package com.omniweb.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.DownloadTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsView(database: AppDatabase, onBack: () -> Unit) {
    val downloads by database.downloadDao().getAllDownloads().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No downloads yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(downloads) { task ->
                    DownloadItem(task)
                }
            }
        }
    }
}

@Composable
fun DownloadItem(task: DownloadTask) {
    ListItem(
        headlineContent = { Text(task.title, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                Text(task.url, maxLines = 1, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (task.totalSize > 0) task.downloadedSize.toFloat() / task.totalSize else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        trailingContent = {
            Text(if (task.totalSize > 0) "${task.downloadedSize / 1024} KB / ${task.totalSize / 1024} KB" else "Starting...")
        }
    )
}
