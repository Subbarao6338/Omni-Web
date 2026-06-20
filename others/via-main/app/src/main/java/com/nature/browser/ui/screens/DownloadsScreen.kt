package com.nature.browser.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val progress: Int,
    val totalSize: Long,
    val downloadedSize: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val downloadManager = remember { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    var downloads by remember { mutableStateOf(listOf<DownloadItem>()) }

    LaunchedEffect(Unit) {
        while (true) {
            downloads = queryDownloads(downloadManager)
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nature's Harvest (Downloads)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Your basket is empty")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                items(downloads) { download ->
                    DownloadListItem(download,
                        onRemove = { downloadManager.remove(download.id) },
                        onToggle = { /* Pause/Resume logic if supported by system DM */ }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadListItem(download: DownloadItem, onRemove: () -> Unit, onToggle: () -> Unit) {
    ListItem(
        headlineContent = { Text(download.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                val statusText = when (download.status) {
                    DownloadManager.STATUS_PENDING -> "Waiting in the stream"
                    DownloadManager.STATUS_RUNNING -> "Gathering... ${download.progress}%"
                    DownloadManager.STATUS_PAUSED -> "Resting"
                    DownloadManager.STATUS_SUCCESSFUL -> "Harvested"
                    DownloadManager.STATUS_FAILED -> "Withered"
                    else -> "Unknown"
                }
                Text(statusText)
                if (download.status == DownloadManager.STATUS_RUNNING) {
                    LinearProgressIndicator(
                        progress = { download.progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (download.status == DownloadManager.STATUS_PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Toggle"
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    )
}

private fun queryDownloads(dm: DownloadManager): List<DownloadItem> {
    val query = DownloadManager.Query()
    val cursor: Cursor = dm.query(query)
    val list = mutableListOf<DownloadItem>()

    if (cursor.moveToFirst()) {
        do {
            val idColumn = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val totalSizeColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val downloadedSizeColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

            if (idColumn != -1 && titleColumn != -1 && statusColumn != -1) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val status = cursor.getInt(statusColumn)
                val totalSize = if (totalSizeColumn != -1) cursor.getLong(totalSizeColumn) else 0L
                val downloadedSize = if (downloadedSizeColumn != -1) cursor.getLong(downloadedSizeColumn) else 0L
                val progress = if (totalSize > 0) ((downloadedSize * 100) / totalSize).toInt() else 0

                list.add(DownloadItem(id, title, status, progress, totalSize, downloadedSize))
            }
        } while (cursor.moveToNext())
    }
    cursor.close()
    return list.asReversed()
}
