package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Settings
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineManagerView(database: AppDatabase, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settingsState by database.settingsDao().getSettings().collectAsState(initial = Settings())
    val settings = settingsState ?: Settings()

    var showAddDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    val customEngines = remember(settings.customSearchEngines) {
        val list = mutableListOf<Pair<String, String>>()
        try {
            if (!settings.customSearchEngines.isNullOrBlank()) {
                val array = JSONArray(settings.customSearchEngines)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(obj.getString("name") to obj.getString("url"))
                }
            }
        } catch (e: Exception) {}
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Engines", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text("Default Engines", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            val defaultEngines = listOf(
                "Google" to "https://www.google.com/search?q=",
                "DuckDuckGo" to "https://duckduckgo.com/?q=",
                "Brave" to "https://search.brave.com/search?q=",
                "Bing" to "https://www.bing.com/search?q=",
                "Yahoo" to "https://search.yahoo.com/search?p=",
                "Baidu" to "https://www.baidu.com/s?wd=",
                "Ecosia" to "https://www.ecosia.org/search?q=",
                "Yandex" to "https://yandex.ru/yandsearch?lr=21411&text=",
                "DuckDuckGo (Lite)" to "https://duckduckgo.com/lite/?q="
            )

            itemsIndexed(defaultEngines) { _, (name, url) ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(url) },
                    trailingContent = {
                        RadioButton(selected = settings.searchEngine == url, onClick = {
                            scope.launch { database.settingsDao().updateSettings(settings.copy(searchEngine = url)) }
                        })
                    },
                    modifier = Modifier.clickable {
                        scope.launch { database.settingsDao().updateSettings(settings.copy(searchEngine = url)) }
                    }
                )
            }

            item {
                Text("Custom Engines", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            if (customEngines.isEmpty()) {
                item {
                    Text("No custom engines added", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                itemsIndexed(customEngines) { index, (name, url) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(url) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = settings.searchEngine == url, onClick = {
                                    scope.launch { database.settingsDao().updateSettings(settings.copy(searchEngine = url)) }
                                })
                                IconButton(onClick = {
                                    scope.launch {
                                        val array = JSONArray(settings.customSearchEngines)
                                        val newArray = JSONArray()
                                        for (i in 0 until array.length()) {
                                            if (i != index) newArray.put(array.get(i))
                                        }
                                        val nextEngine = if (settings.searchEngine == url) "https://www.google.com/search?q=" else settings.searchEngine
                                        database.settingsDao().updateSettings(settings.copy(
                                            customSearchEngines = newArray.toString(),
                                            searchEngine = nextEngine
                                        ))
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            scope.launch { database.settingsDao().updateSettings(settings.copy(searchEngine = url)) }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Search Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Search URL") },
                        placeholder = { Text("https://example.com/search?q=") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Search term will be appended to the end of the URL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank() && urlInput.isNotBlank()) {
                        scope.launch {
                            val array = if (settings.customSearchEngines.isNullOrBlank()) JSONArray() else JSONArray(settings.customSearchEngines)
                            val obj = JSONObject()
                            obj.put("name", nameInput)
                            obj.put("url", urlInput)
                            array.put(obj)
                            database.settingsDao().updateSettings(settings.copy(customSearchEngines = array.toString()))
                            nameInput = ""
                            urlInput = ""
                            showAddDialog = false
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
