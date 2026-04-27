package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Settings
import com.omniweb.app.data.Shortcut
import com.omniweb.app.util.UrlUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    onNavigate: (String) -> Unit,
    viewModel: BrowserViewModel,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: Settings()
    val history by database.historyDao().getAllHistory().collectAsState(initial = emptyList())
    val mostVisited by database.historyDao().getMostVisited().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId

    var query by remember { mutableStateOf("") }
    var showTabs by remember { mutableStateOf(false) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var newShortcutTitle by remember { mutableStateOf("") }
    var newShortcutUrl by remember { mutableStateOf("") }

    val shortcutsState by database.shortcutDao().getAllShortcuts().collectAsState(initial = emptyList())
    val shortcuts = if (shortcutsState.isEmpty()) {
        listOf(
            Shortcut(title = "Google", url = "https://www.google.com"),
            Shortcut(title = "YouTube", url = "https://www.youtube.com"),
            Shortcut(title = "GitHub", url = "https://www.github.com"),
            Shortcut(title = "Reddit", url = "https://www.reddit.com"),
            Shortcut(title = "Wikipedia", url = "https://www.wikipedia.org"),
            Shortcut(title = "Amazon", url = "https://www.amazon.com"),
            Shortcut(title = "X", url = "https://x.com"),
            Shortcut(title = "Instagram", url = "https://www.instagram.com")
        )
    } else {
        shortcutsState
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.navigationBarsPadding(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    NavButton(Icons.Default.Layers, "Tabs", badge = tabs.size) { showTabs = true }
                    NavButton(Icons.Default.Star, "Bookmarks") { onOpenBookmarks() }
                    NavButton(Icons.Default.History, "History") { onOpenHistory() }
                    NavButton(Icons.Default.Download, "Files") { onOpenDownloads() }
                    NavButton(Icons.Default.Settings, "Settings") { onOpenSettings() }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(top = 80.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Omni Browser", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(48.dp))

        val focusManager = LocalFocusManager.current
        TextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.updateSuggestions(it)
            },
            placeholder = { Text("Search or type URL", fontSize = 16.sp) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotEmpty()) {
                    val target = UrlUtils.resolveUrl(query, settings.searchEngine)
                    if (target == "about:home") {
                        query = ""
                    } else {
                        onNavigate(target)
                    }
                }
                focusManager.clearFocus()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (MaterialTheme.colorScheme.surface == Color(0xFF121212)) Color(0xFF1E1E1E) else Color.White,
                unfocusedContainerColor = if (MaterialTheme.colorScheme.surface == Color(0xFF121212)) Color(0xFF2C2C2C) else Color(0xFFE5E7EB),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )

        val suggestions by viewModel.searchSuggestions
        if (suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    suggestions.forEach { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(suggestion.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            leadingContent = { Icon(if (suggestion.isHistory) Icons.Default.History else Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.clickable {
                                val target = UrlUtils.resolveUrl(suggestion.url, settings.searchEngine)
                                if (target != "about:home") {
                                    onNavigate(target)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (mostVisited.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Most Visited", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(mostVisited) { entry ->
                        Card(
                            modifier = Modifier.width(120.dp).clickable { onNavigate(entry.url) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(entry.title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(entry.title, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        } else if (history.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Recent History", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(history.take(4)) { entry ->
                        Card(
                            modifier = Modifier.width(140.dp).clickable { onNavigate(entry.url) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(entry.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
             Text("Shortcuts", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.heightIn(max = 2000.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            userScrollEnabled = false
        ) {
            items(shortcuts) { shortcut ->
                ShortcutItem(
                    shortcut,
                    onClick = { onNavigate(shortcut.url) },
                    onLongClick = {
                        scope.launch {
                            database.shortcutDao().deleteShortcut(shortcut)
                        }
                    }
                )
            }
            item { AddShortcutItem(onClick = { showAddShortcutDialog = true }) }
        }
        }
    }

    if (showAddShortcutDialog) {
        AlertDialog(
            onDismissRequest = { showAddShortcutDialog = false },
            title = { Text("Add Shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newShortcutTitle,
                        onValueChange = { newShortcutTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newShortcutUrl,
                        onValueChange = { newShortcutUrl = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newShortcutTitle.isNotEmpty() && newShortcutUrl.isNotEmpty()) {
                            val url = if (newShortcutUrl.startsWith("http")) newShortcutUrl else "https://$newShortcutUrl"
                            scope.launch {
                                database.shortcutDao().insertShortcut(com.omniweb.app.data.Shortcut(title = newShortcutTitle, url = url))
                                newShortcutTitle = ""
                                newShortcutUrl = ""
                                showAddShortcutDialog = false
                            }
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddShortcutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTabs) {
        ModalBottomSheet(onDismissRequest = { showTabs = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = {
                        viewModel.createTab()
                        showTabs = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(tabs) { tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (tab.id == activeTabId) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    viewModel.selectTab(tab.id)
                                    showTabs = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = if (tab.id == activeTabId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tab.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tab.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { viewModel.closeTab(tab.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
