package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniweb.app.data.TabInfo

@Composable
fun TreeViewTabSwitcher(
    tabs: List<TabInfo>,
    activeTabId: String,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simple list for now, as we don't have parent-child relationship in TabInfo yet
        items(tabs) { tab ->
            TabTreeItem(
                tab = tab,
                isSelected = tab.id == activeTabId,
                onSelect = { onTabSelect(tab.id) },
                onClose = { onTabClose(tab.id) }
            )
        }
    }
}

@Composable
fun TabTreeItem(
    tab: TabInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tab.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(tab.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Tab")
            }
        }
    }
}
