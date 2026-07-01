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
    val roots = tabs.filter { it.parentTabId == null || tabs.none { t -> t.id == it.parentTabId } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        roots.forEach { root ->
            item(key = root.id) {
                TabTreeItem(
                    tab = root,
                    isSelected = root.id == activeTabId,
                    onSelect = { onTabSelect(root.id) },
                    onClose = { onTabClose(root.id) },
                    depth = 0
                )
            }
            val children = getChildrenRecursive(root.id, tabs)
            items(children, key = { it.first.id }) { (child, depth) ->
                TabTreeItem(
                    tab = child,
                    isSelected = child.id == activeTabId,
                    onSelect = { onTabSelect(child.id) },
                    onClose = { onTabClose(child.id) },
                    depth = depth
                )
            }
        }
    }
}

private fun getChildrenRecursive(parentId: String, allTabs: List<TabInfo>, depth: Int = 1): List<Pair<TabInfo, Int>> {
    val result = mutableListOf<Pair<TabInfo, Int>>()
    val children = allTabs.filter { it.parentTabId == parentId }
    children.forEach { child ->
        result.add(child to depth)
        result.addAll(getChildrenRecursive(child.id, allTabs, depth + 1))
    }
    return result
}

@Composable
fun TabTreeItem(
    tab: TabInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    depth: Int
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 20).dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
