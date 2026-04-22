package com.omniweb.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Settings
import com.omniweb.app.util.BookmarkExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(database: AppDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsState by database.settingsDao().getSettings().collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    val settings = settingsState ?: Settings()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCategory("General", Icons.Default.Search)

            ListItem(
                headlineContent = { Text("Search Engine") },
                supportingContent = { Text(if (settings.searchEngine.contains("google")) "Google" else "Custom") },
                trailingContent = {
                    Switch(
                        checked = settings.searchEngine.contains("google"),
                        onCheckedChange = { isGoogle ->
                            scope.launch {
                                database.settingsDao().updateSettings(
                                    settings.copy(searchEngine = if (isGoogle) "https://www.google.com/search?q=" else "https://duckduckgo.com/?q=")
                                )
                            }
                        }
                    )
                }
            )

            SettingsCategory("Privacy & Security", Icons.Default.Shield)

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

            SettingsCategory("Appearance", Icons.Default.Palette)

            ListItem(
                headlineContent = { Text("Dark Mode") },
                trailingContent = {
                    Switch(
                        checked = settings.darkMode,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                database.settingsDao().updateSettings(settings.copy(darkMode = enabled))
                            }
                        }
                    )
                }
            )

            SettingsCategory("AI Features", Icons.Default.AutoAwesome)

            var keyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }

            ListItem(
                headlineContent = { Text("Gemini API Key") },
                supportingContent = {
                    TextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("Enter your API key") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    database.settingsDao().updateSettings(settings.copy(geminiApiKey = keyInput))
                                    Toast.makeText(context, "API Key saved", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                    )
                }
            )

            SettingsCategory("Data Management", Icons.Default.ImportExport)

            ListItem(
                headlineContent = { Text("Export Bookmarks") },
                supportingContent = { Text("Copy bookmarks JSON to clipboard") },
                trailingContent = {
                    IconButton(onClick = {
                        scope.launch {
                            database.bookmarkDao().getAllBookmarks().collect { bookmarks ->
                                val json = BookmarkExporter.exportToJson(bookmarks)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Bookmarks", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Bookmarks exported to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Export")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsCategory(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
