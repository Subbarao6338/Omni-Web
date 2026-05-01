package com.omniweb.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Settings
import com.omniweb.app.util.BackupManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsView(database: AppDatabase, onBack: () -> Unit, onOpenScripts: () -> Unit = {}, onOpenPasswords: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsState by database.settingsDao().getSettings().collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    val settings = settingsState ?: Settings()

    var showAddEngineDialog by remember { mutableStateOf(false) }
    var newEngineName by remember { mutableStateOf("") }
    var newEngineUrl by remember { mutableStateOf("") }

    val customEngines = remember(settings.customSearchEngines) {
        try {
            val list = mutableListOf<Pair<String, String>>()
            if (!settings.customSearchEngines.isNullOrBlank()) {
                val array = JSONArray(settings.customSearchEngines)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(obj.getString("name") to obj.getString("url"))
                }
            }
            list
        } catch (e: Exception) {
            emptyList<Pair<String, String>>()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val path = it.toString()
            scope.launch {
                database.settingsDao().updateSettings(settings.copy(downloadPath = path))
                Toast.makeText(context, "Download path updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("General", Icons.Default.Search) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search Engine", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    val engines = listOf(
                        "Google" to "https://www.google.com/search?q=",
                        "DuckDuckGo" to "https://duckduckgo.com/?q=",
                        "Bing" to "https://www.bing.com/search?q=",
                        "Yahoo" to "https://search.yahoo.com/search?p="
                    ) + customEngines

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        engines.forEach { (name, url) ->
                            FilterChip(
                                selected = settings.searchEngine == url,
                                onClick = {
                                    scope.launch { database.settingsDao().updateSettings(settings.copy(searchEngine = url)) }
                                },
                                label = { Text(name) }
                            )
                        }
                        AssistChip(
                            onClick = { showAddEngineDialog = true },
                            label = { Text("Add Custom") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text("Clear data on exit") },
                    supportingContent = { Text("Automatically clear history and cache when app is closed") },
                    trailingContent = {
                        Switch(
                            checked = settings.clearDataOnExit,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    database.settingsDao().updateSettings(settings.copy(clearDataOnExit = enabled))
                                }
                            }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text("Restore tabs on start") },
                    supportingContent = { Text("Continue where you left off") },
                    trailingContent = {
                        Switch(
                            checked = settings.restoreTabsOnStart,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    database.settingsDao().updateSettings(settings.copy(restoreTabsOnStart = enabled))
                                }
                            }
                        )
                    }
                )
            }

            SettingsSection("Downloads", Icons.Default.Download) {
                ListItem(
                    headlineContent = { Text("Download Path") },
                    supportingContent = { Text(settings.downloadPath ?: "Default (Downloads folder)") },
                    trailingContent = {
                        Row {
                            if (settings.downloadPath != null) {
                                IconButton(onClick = {
                                    scope.launch {
                                        database.settingsDao().updateSettings(settings.copy(downloadPath = null))
                                        Toast.makeText(context, "Reset to default download path", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Path")
                                }
                            }
                            IconButton(onClick = {
                                folderPickerLauncher.launch(null)
                            }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Change Folder")
                            }
                        }
                    }
                )
            }

            SettingsSection("Appearance", Icons.Default.Palette) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption("Light", settings.themeMode == "light", Modifier.weight(1f)) {
                            scope.launch { database.settingsDao().updateSettings(settings.copy(themeMode = "light")) }
                        }
                        ThemeOption("Dark", settings.themeMode == "dark", Modifier.weight(1f)) {
                            scope.launch { database.settingsDao().updateSettings(settings.copy(themeMode = "dark")) }
                        }
                        ThemeOption("System", settings.themeMode == "system", Modifier.weight(1f)) {
                            scope.launch { database.settingsDao().updateSettings(settings.copy(themeMode = "system")) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Accent Color", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    val colors = listOf("#3B82F6", "#8B5CF6", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#6366F1")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(colors) { colorHex ->
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (settings.accentColor == colorHex) 3.dp else 0.dp,
                                        color = if (settings.accentColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        scope.launch {
                                            database.settingsDao().updateSettings(settings.copy(accentColor = colorHex))
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            SettingsSection("Privacy & Security", Icons.Default.Shield) {
                ListItem(
                    headlineContent = { Text("Passwords") },
                    supportingContent = { Text("Manage saved credentials") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onOpenPasswords() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text("Ad Blocking") },
                    supportingContent = { Text("Block ads and trackers") },
                    trailingContent = {
                        Switch(
                            checked = settings.adBlockEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    database.settingsDao().updateSettings(settings.copy(adBlockEnabled = enabled))
                                }
                            }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text("JavaScript") },
                    supportingContent = { Text("Enable execution of scripts on websites") },
                    trailingContent = {
                        Switch(
                            checked = settings.javaScriptEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    database.settingsDao().updateSettings(settings.copy(javaScriptEnabled = enabled))
                                }
                            }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text("Block Third-Party Cookies") },
                    supportingContent = { Text("Improved privacy by preventing cross-site tracking") },
                    trailingContent = {
                        Switch(
                            checked = settings.blockThirdPartyCookies,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    database.settingsDao().updateSettings(settings.copy(blockThirdPartyCookies = enabled))
                                }
                            }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text("Clear Browsing Data", color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("History, Cache, and Cookies") },
                    modifier = Modifier.clickable {
                        scope.launch {
                            database.historyDao().clearHistory()
                            android.webkit.WebStorage.getInstance().deleteAllData()
                            android.webkit.CookieManager.getInstance().removeAllCookies(null)
                            Toast.makeText(context, "Browsing data cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            SettingsSection("Advanced", Icons.Default.Build) {
                ListItem(
                    headlineContent = { Text("Userscripts") },
                    supportingContent = { Text("Manage custom JS injections") },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onOpenScripts() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom User Agent", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.customUserAgent ?: "",
                        onValueChange = {
                            scope.launch {
                                database.settingsDao().updateSettings(settings.copy(customUserAgent = it.ifBlank { null }))
                            }
                        },
                        placeholder = { Text("Leave empty for default") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            SettingsSection("Data Management", Icons.Default.ImportExport) {
                ListItem(
                    headlineContent = { Text("Export Data") },
                    supportingContent = { Text("Copy settings and bookmarks to clipboard") },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch {
                            val bookmarks = database.bookmarkDao().getAllBookmarks().firstOrNull() ?: emptyList()
                                val json = BackupManager.exportData(bookmarks, settings)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("OmniBackup", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Data exported to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Export")
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("Import Data") },
                    supportingContent = { Text("Restore data from clipboard") },
                    trailingContent = {
                        IconButton(onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val json = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (json != null) {
                                    scope.launch {
                                        val newBookmarks = BackupManager.importBookmarks(json)
                                        val newSettings = BackupManager.importSettings(json, settings)

                                        database.settingsDao().updateSettings(newSettings)
                                        newBookmarks.forEach { database.bookmarkDao().insertBookmark(it) }

                                        Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: Invalid format", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Import")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAddEngineDialog) {
        AlertDialog(
            onDismissRequest = { showAddEngineDialog = false },
            title = { Text("Add Custom Search Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newEngineName,
                        onValueChange = { newEngineName = it },
                        label = { Text("Engine Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEngineUrl,
                        onValueChange = { newEngineUrl = it },
                        label = { Text("Query URL (use %s for query)") },
                        placeholder = { Text("https://example.com/search?q=%s") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newEngineName.isNotBlank() && newEngineUrl.isNotBlank()) {
                        scope.launch {
                            val array = try {
                                if (settings.customSearchEngines != null) JSONArray(settings.customSearchEngines) else JSONArray()
                            } catch (e: Exception) {
                                JSONArray()
                            }
                            val obj = org.json.JSONObject()
                            obj.put("name", newEngineName)
                            obj.put("url", newEngineUrl.replace("%s", ""))
                            array.put(obj)

                            database.settingsDao().updateSettings(settings.copy(
                                customSearchEngines = array.toString()
                            ))
                            newEngineName = ""
                            newEngineUrl = ""
                            showAddEngineDialog = false
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEngineDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
