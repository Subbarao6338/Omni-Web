package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGrabberView(
    mediaItems: List<MediaItem>,
    onDownload: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    var filterType by remember { mutableStateOf("all") }
    val filteredItems = if (filterType == "all") mediaItems else mediaItems.filter {
        if (filterType == "video") it.type == "video"
        else if (filterType == "audio") it.type == "audio"
        else it.type == "image"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Media Grabber", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${filteredItems.size} items detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (mediaItems.isNotEmpty()) {
                        TextButton(onClick = {
                            mediaItems.take(50).forEach { onDownload(it) }
                        }) {
                            Text("Bulk (50)")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterType == "all", onClick = { filterType = "all" }, label = { Text("All") })
                FilterChip(selected = filterType == "video", onClick = { filterType = "video" }, label = { Text("Videos") })
                FilterChip(selected = filterType == "audio", onClick = { filterType = "audio" }, label = { Text("Audio") })
                FilterChip(selected = filterType == "image", onClick = { filterType = "image" }, label = { Text("Images") })
            }

            if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No media detected on this page", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Try scrolling down to load more content", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredItems) { item ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onDownload(item) }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val ext = item.src.substringAfterLast(".", "").uppercase().take(4)
                                        if (ext.isNotEmpty()) {
                                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                                Text(ext, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                },
                                supportingContent = { Text(item.src, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                                leadingContent = {
                                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (item.type) {
                                                "video" -> Icons.Default.Movie
                                                "audio" -> Icons.Default.MusicNote
                                                "image" -> Icons.Default.Image
                                                else -> Icons.Default.InsertDriveFile
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { onDownload(item) }) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
