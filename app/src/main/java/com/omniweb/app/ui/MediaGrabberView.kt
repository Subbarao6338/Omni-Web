package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onDownload: (List<MediaItem>) -> Unit,
    onBack: () -> Unit
) {
    var filterType by remember { mutableStateOf("all") }
    val selectedItems = remember { mutableStateListOf<String>() }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            val links = mediaItems.filter { it.id in selectedItems }.joinToString("\n") { it.src }
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Media Links", links))
                            android.widget.Toast.makeText(context, "Copied ${selectedItems.size} links", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Selected Links")
                        }
                        IconButton(onClick = {
                            val toDownload = mediaItems.filter { it.id in selectedItems }
                            onDownload(toDownload)
                            selectedItems.clear()
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download Selected")
                        }
                    }
                    if (mediaItems.isNotEmpty()) {
                        TextButton(onClick = {
                            if (selectedItems.size == filteredItems.size) {
                                selectedItems.clear()
                            } else {
                                selectedItems.clear()
                                selectedItems.addAll(filteredItems.map { it.id })
                            }
                        }) {
                            Text(if (selectedItems.size == filteredItems.size) "Deselect All" else "Select All")
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredItems) { item ->
                        val isSelected = item.id in selectedItems
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSelected, onCheckedChange = {
                                        if (it) selectedItems.add(item.id) else selectedItems.remove(item.id)
                                    })
                                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (item.type) {
                                                "video" -> Icons.Default.Movie
                                                "audio" -> Icons.Default.MusicNote
                                                "image" -> Icons.Default.Image
                                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { onDownload(listOf(item)) }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                if (isSelected) selectedItems.remove(item.id) else selectedItems.add(item.id)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
