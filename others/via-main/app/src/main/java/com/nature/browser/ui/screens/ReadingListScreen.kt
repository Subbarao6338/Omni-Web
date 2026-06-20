package com.nature.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nature.browser.BrowserViewModel
import com.nature.browser.db.ReadingListEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit,
    onItemClick: (ReadingListEntity) -> Unit
) {
    val readingList by viewModel.readingList.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kept Stones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (readingList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Your reading list is empty", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(readingList) { item ->
                    ReadingListItemRow(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
fun ReadingListItemRow(item: ReadingListEntity, onClick: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))

    ListItem(
        headlineContent = { Text(item.title, maxLines = 1) },
        supportingContent = { Text("${item.url} • $date", maxLines = 1) },
        leadingContent = { Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = Color(0xFF2A9D8F)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun OfflineBanner(date: Long, onRetry: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(date))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF9F6EF),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.OfflineBolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF264653))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Offline — cached from $dateStr",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF264653),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2A9D8F))
            }
        }
    }
}
