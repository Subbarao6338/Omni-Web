package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniweb.app.data.TabInfo

@Composable
fun TreeViewTabSwitcher(
    tabs: List<TabInfo>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit
) {
    val rootTabs = tabs.filter { it.parentTabId == null }

    LazyColumn {
        items(rootTabs) { tab ->
            TabTreeItem(tab, tabs, activeTabId, onTabSelected, onTabClosed, depth = 0)
        }
    }
}

@Composable
fun TabTreeItem(
    tab: TabInfo,
    allTabs: List<TabInfo>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    depth: Int
) {
    var expanded by remember { mutableStateOf(true) }
    val children = allTabs.filter { it.parentTabId == tab.id }

    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        ListItem(
            headlineContent = { Text(tab.title, maxLines = 1) },
            supportingContent = { Text(tab.url, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                if (children.isNotEmpty()) {
                    Icon(
                        if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            },
            modifier = Modifier.clickable { onTabSelected(tab.id) },
            colors = if (tab.id == activeTabId) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors()
        )

        if (expanded && children.isNotEmpty()) {
            children.forEach { child ->
                TabTreeItem(child, allTabs, activeTabId, onTabSelected, onTabClosed, depth + 1)
            }
        }
    }
}
