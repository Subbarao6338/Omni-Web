package com.nature.browser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nature.browser.BrowserStorage
import com.nature.browser.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storage: BrowserStorage,
    onBack: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit
) {
    var homepage by remember { mutableStateOf(storage.homepage) }
    var searchEngine by remember { mutableStateOf(storage.searchEngine) }
    var isAdBlockEnabled by remember { mutableStateOf(storage.isAdBlockEnabled) }
    var isDarkMode by remember { mutableStateOf(storage.isDarkMode) }
    var isDesktopMode by remember { mutableStateOf(storage.isDesktopMode) }
    var selectedTheme by remember { mutableStateOf(storage.appTheme) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SettingsSectionTitle("Appearance")
                ThemeSelectionItem(
                    selectedTheme = selectedTheme,
                    onThemeSelected = {
                        selectedTheme = it
                        storage.appTheme = it
                        onThemeChanged(it)
                    }
                )
                SettingsSwitchItem(
                    label = "Dark Mode",
                    checked = isDarkMode,
                    onCheckedChange = {
                        isDarkMode = it
                        storage.isDarkMode = it
                    }
                )
            }

            item {
                SettingsSectionTitle("General")
                SettingsTextFieldItem(
                    label = "Homepage",
                    value = homepage,
                    onValueChange = {
                        homepage = it
                        storage.homepage = it
                    }
                )
                SearchEngineSelector(
                    currentEngineUrl = searchEngine,
                    engines = storage.getSearchEngines(),
                    onEngineSelected = {
                        searchEngine = it
                        storage.searchEngine = it
                    },
                    onAddEngine = { name, url ->
                        storage.addSearchEngine(name, url)
                    }
                )
            }

            item {
                SettingsSectionTitle("Browsing")
                SettingsSwitchItem(
                    label = "Ad Block",
                    checked = isAdBlockEnabled,
                    onCheckedChange = {
                        isAdBlockEnabled = it
                        storage.isAdBlockEnabled = it
                    }
                )
                SettingsSwitchItem(
                    label = "Desktop Mode",
                    checked = isDesktopMode,
                    onCheckedChange = {
                        isDesktopMode = it
                        storage.isDesktopMode = it
                    }
                )
            }

            item {
                SettingsSectionTitle("About")
                ListItem(
                    headlineContent = { Text("Nature Browser") },
                    supportingContent = { Text("Version 1.0") }
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionItem(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppTheme.entries) { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) },
                    label = { Text(theme.name) }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SearchEngineSelector(
    currentEngineUrl: String,
    engines: List<Pair<String, String>>,
    onEngineSelected: (String) -> Unit,
    onAddEngine: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Search Engine", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        engines.forEach { (name, url) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onEngineSelected(url) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = currentEngineUrl == url, onClick = { onEngineSelected(url) })
                Spacer(modifier = Modifier.width(8.dp))
                Text(name)
            }
        }
        TextButton(onClick = { showAddDialog = true }) {
            Text("+ Add Custom Search Engine")
        }

        if (showAddDialog) {
            var newName by remember { mutableStateOf("") }
            var newUrl by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Search Engine") },
                text = {
                    Column {
                        TextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
                        TextField(value = newUrl, onValueChange = { newUrl = it }, label = { Text("Search URL (with %s for query)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotEmpty() && newUrl.isNotEmpty()) {
                            onAddEngine(newName, newUrl)
                            showAddDialog = false
                        }
                    }) { Text("Add") }
                }
            )
        }
    }
}

@Composable
fun SettingsTextFieldItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
