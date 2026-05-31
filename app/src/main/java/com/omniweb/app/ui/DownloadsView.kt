package com.omniweb.app.ui

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.DownloadTask
import com.omniweb.app.util.LogUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadsView(database: AppDatabase, onBack: () -> Unit) {
    val downloads by database.downloadDao().getAllDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val categories = listOf("All", "Videos", "Audio", "Images", "Documents", "Other")

    val filteredDownloads = downloads.filter { task ->
        val matchesSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.url.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "Videos" -> listOf("mp4", "webm", "mkv", "mov").any { task.title.lowercase().endsWith(it) }
            "Audio" -> listOf("mp3", "wav", "m4a", "ogg", "aac").any { task.title.lowercase().endsWith(it) }
            "Images" -> listOf("jpg", "jpeg", "png", "webp", "gif").any { task.title.lowercase().endsWith(it) }
            "Documents" -> listOf("pdf", "doc", "docx", "txt", "md").any { task.title.lowercase().endsWith(it) }
            else -> !listOf("mp4", "webm", "mkv", "mov", "mp3", "wav", "m4a", "ogg", "aac", "jpg", "jpeg", "png", "webp", "gif", "pdf", "doc", "docx", "txt", "md").any { task.title.lowercase().endsWith(it) }
        }
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Downloads", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (downloads.any { it.status == 8 }) {
                            TextButton(onClick = {
                                scope.launch {
                                    database.downloadDao().deleteFinishedDownloads()
                                    Toast.makeText(context, "Finished downloads cleared", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Clear Finished", fontSize = 12.sp)
                            }
                        }
                        if (downloads.isNotEmpty()) {
                            IconButton(onClick = {
                                scope.launch {
                                    downloads.forEach { database.downloadDao().deleteDownload(it) }
                                    Toast.makeText(context, "Download history cleared", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search downloads...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (filteredDownloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (searchQuery.isEmpty()) Icons.Default.Download else Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isEmpty()) "No downloads yet" else "No matching downloads found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(filteredDownloads) { task ->
                    DownloadItem(
                        task = task,
                        onOpen = {
                            if (task.filePath != null) {
                                try {
                                    val uri = if (task.filePath.startsWith("content://")) {
                                        Uri.parse(task.filePath)
                                    } else {
                                        val file = File(task.filePath)
                                        if (file.exists()) {
                                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        } else {
                                            null
                                        }
                                    }

                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "File not found or not accessible", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    LogUtils.e("Failed to open downloaded file", e)
                                    Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "File path is missing", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                database.downloadDao().deleteDownload(task)
                                // Optionally delete the file from storage
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun DownloadItem(task: DownloadTask, onOpen: () -> Unit, onDelete: () -> Unit) {
    val progress = if (task.totalSize > 0) task.downloadedSize.toFloat() / task.totalSize else 0f
    val isComplete = task.status == DownloadManager.STATUS_SUCCESSFUL || (task.totalSize > 0 && task.downloadedSize >= task.totalSize)
    val isFailed = task.status == DownloadManager.STATUS_FAILED

    ListItem(
        headlineContent = { Text(task.title, fontWeight = FontWeight.Bold, maxLines = 1) },
        supportingContent = {
            Column {
                Text(task.url, maxLines = 1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (!isComplete && !isFailed) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = when {
                            isFailed -> "Failed"
                            isComplete -> "Complete"
                            task.status == DownloadManager.STATUS_PAUSED -> "Paused"
                            else -> "${(progress * 100).toInt()}%"
                        },
                        fontSize = 11.sp,
                        color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (task.totalSize > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${task.downloadedSize / 1024} KB / ${task.totalSize / 1024} KB", fontSize = 11.sp)
                            if (!isComplete && !isFailed && task.status == DownloadManager.STATUS_RUNNING) {
                                val speedText = if (task.downloadSpeed > 1024 * 1024) "${String.format("%.1f", task.downloadSpeed / (1024f * 1024f))} MB/s" else "${task.downloadSpeed / 1024} KB/s"
                                val etaText = if (task.estimatedTimeRemaining > 60) "${task.estimatedTimeRemaining / 60}m ${task.estimatedTimeRemaining % 60}s" else "${task.estimatedTimeRemaining}s"
                                Text("$speedText - $etaText left", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        trailingContent = {
            Row {
                if (isComplete) {
                    IconButton(onClick = onOpen) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
