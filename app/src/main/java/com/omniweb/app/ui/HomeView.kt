package com.omniweb.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by database.historyDao().getAllHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    val mostVisited by database.historyDao().getMostVisited().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var showTabs by remember { mutableStateOf(false) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var newShortcutTitle by remember { mutableStateOf("") }
    var newShortcutUrl by remember { mutableStateOf("") }

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val shortcutsState by database.shortcutDao().getAllShortcuts().collectAsStateWithLifecycle(initialValue = emptyList())
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Omni Browser",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                val lastTab = tabs.find { it.id == activeTabId }
                if (lastTab != null && lastTab.url != "about:home") {
                    ElevatedCard(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .clickable { onNavigate(lastTab.url) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (lastTab.faviconBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = lastTab.faviconBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CONTINUE BROWSING", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                Text(lastTab.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(lastTab.url, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                val focusManager = LocalFocusManager.current
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.updateSuggestions(it)
                        },
                        placeholder = { Text("Search or type URL", fontSize = 16.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        leadingIcon = {
                            val icon = when {
                                settings.searchEngine.contains("google") -> Icons.Default.Search
                                settings.searchEngine.contains("duckduckgo") -> Icons.Default.Shield
                                settings.searchEngine.contains("bing") -> Icons.Default.TravelExplore
                                else -> Icons.Default.Language
                            }
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    viewModel.updateSuggestions("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (query.isNotEmpty()) {
                                val target = UrlUtils.resolveUrl(query, settings.searchEngine)
                                if (target != "about:home") {
                                    onNavigate(target)
                                }
                            }
                            focusManager.clearFocus()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    )

                    val suggestions by viewModel.searchSuggestions
                    if (suggestions.isNotEmpty() && query.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                suggestions.forEach { suggestion ->
                                    ListItem(
                                        headlineContent = { Text(suggestion.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        supportingContent = { Text(suggestion.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                        leadingContent = {
                                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (suggestion.isHistory) Icons.Default.History else Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            val target = UrlUtils.resolveUrl(suggestion.url, settings.searchEngine)
                                            if (target != "about:home") {
                                                onNavigate(target)
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (shortcuts.isNotEmpty()) {
                item {
                    SectionHeader("Shortcuts", onAction = { showAddShortcutDialog = true }, actionIcon = Icons.Default.Add)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 400.dp).padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        userScrollEnabled = false
                    ) {
                        itemsIndexed(shortcuts) { index, shortcut ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(index * 50L)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f)
                            ) {
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
                        }
                        item {
                            AddShortcutItem(onClick = { showAddShortcutDialog = true })
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                SectionHeader("News Feed")
                Box(modifier = Modifier.heightIn(max = 400.dp).padding(horizontal = 16.dp)) {
                    RSSView(onNavigate = onNavigate)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (mostVisited.isNotEmpty()) {
                item {
                    SectionHeader("Frequently Visited")
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.heightIn(max = 200.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            userScrollEnabled = false
                        ) {
                            items(mostVisited.take(8)) { entry ->
                                val tabInfo = tabs.find { it.url == entry.url }
                                MostVisitedCard(entry, tabInfo?.faviconBitmap) { onNavigate(entry.url) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                val readingList by database.readingListDao().getAllEntries().collectAsStateWithLifecycle(initialValue = emptyList())
                if (readingList.isNotEmpty()) {
                    SectionHeader("Reading List")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(readingList) { entry ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable { onNavigate(entry.filePath ?: entry.url) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(entry.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            if (history.isNotEmpty()) {
                item {
                    SectionHeader("Recent Activity", onAction = {
                        showClearHistoryDialog = true
                    }, actionIcon = Icons.Default.ClearAll)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(history.take(10)) { entry ->
                            Card(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { onNavigate(entry.url) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(entry.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(entry.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History?") },
            text = { Text("Are you sure you want to clear your entire browsing history? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { database.historyDao().clearHistory() }
                    showClearHistoryDialog = false
                }) { Text("Clear All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
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

@Composable
fun SectionHeader(title: String, onAction: (() -> Unit)? = null, actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        if (onAction != null && actionIcon != null) {
            IconButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}


@Composable
fun MostVisitedCard(entry: com.omniweb.app.data.MostVisitedEntry, favicon: android.graphics.Bitmap?, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() }
    ) {
        ElevatedCard(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (favicon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = favicon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    Text(entry.title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(entry.title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
