package com.omniweb.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.TabInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<TabInfo>,
    recentlyClosedTabs: List<TabInfo> = emptyList(),
    activeTabId: String,
    onTabSelect: (String) -> Unit,
    onTabRestore: (TabInfo) -> Unit = {},
    onTabClose: (String) -> Unit,
    onCloseAll: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showCloseAllDialog by remember { mutableStateOf(false) }
    var useTreeView by remember { mutableStateOf(false) }

    if (showCloseAllDialog) {
        AlertDialog(
            onDismissRequest = { showCloseAllDialog = false },
            title = { Text("Close All Tabs?") },
            text = { Text("Are you sure you want to close all open tabs?") },
            confirmButton = {
                TextButton(onClick = {
                    onCloseAll()
                    showCloseAllDialog = false
                }) { Text("Close All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Row {
                    IconButton(onClick = { showCloseAllDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Close All Tabs", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { onNewTab(true) }) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "New Incognito Tab")
                    }
                    IconButton(onClick = { useTreeView = !useTreeView }) {
                        Icon(if (useTreeView) Icons.Default.GridView else Icons.Default.AccountTree, contentDescription = "Toggle View")
                    }
                    IconButton(onClick = { onNewTab(false) }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (useTreeView) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    TreeViewTabSwitcher(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelect = { onTabSelect(it); onDismiss() },
                        onTabClose = onTabClose
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    TabGridSwitcher(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelect = { onTabSelect(it); onDismiss() },
                        onTabClose = onTabClose
                    )
                }
            }

            if (recentlyClosedTabs.isNotEmpty() && !useTreeView) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recently Closed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    items(recentlyClosedTabs) { tab ->
                        ListItem(
                            headlineContent = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(tab.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            leadingContent = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingContent = {
                                IconButton(onClick = { onTabRestore(tab) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore")
                                }
                            },
                            modifier = Modifier.clickable { onTabRestore(tab) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
