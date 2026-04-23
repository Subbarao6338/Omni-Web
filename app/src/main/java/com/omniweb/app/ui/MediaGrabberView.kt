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
        if (filterType == "video") it.type == "mp4" || it.type == "m3u8" || it.type == "youtube"
        else it.type == "jpg" || it.type == "png" || it.type == "webp"
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredItems) { item ->
                        ListItem(
                            headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(item.src, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            leadingContent = {
                                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (item.type) {
                                            "mp4", "m3u8" -> Icons.Default.Movie
                                            "youtube" -> Icons.Default.SmartDisplay
                                            "jpg", "png", "webp" -> Icons.Default.Image
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = if (item.type == "youtube") Color.Red else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { onDownload(item) }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable { onDownload(item) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
