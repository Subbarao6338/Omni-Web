package com.omniweb.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.UserScript
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptManagerView(database: AppDatabase, onBack: () -> Unit) {
    val scripts by database.userScriptDao().getAllScripts().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var scriptToEdit by remember { mutableStateOf<UserScript?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Script Manager") },
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
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Userscripts") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bookmarklets") })
                }
            }
        }
    ) { padding ->
        val filteredScripts = scripts.filter {
            if (selectedTab == 0) it.type == "userscript" else it.type == "bookmarklet"
        }

        if (filteredScripts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No ${if (selectedTab == 0) "userscripts" else "bookmarklets"} found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(filteredScripts) { script ->
                    ScriptItem(
                        script = script,
                        onDelete = { scope.launch { database.userScriptDao().deleteScript(script) } },
                        onEdit = { scriptToEdit = script },
                        onToggle = { enabled ->
                            scope.launch { database.userScriptDao().insertScript(script.copy(enabled = enabled)) }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddScriptDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, code, pattern, type, runAt ->
                scope.launch {
                    database.userScriptDao().insertScript(UserScript(name = name, script = code, matchPattern = pattern, type = type, runAt = runAt))
                }
                showAddDialog = false
            },
            onLibraryRequest = {
                val library = listOf(
                    UserScript(name = "Dark Reader Light", script = "document.documentElement.style.filter = 'invert(1) hue-rotate(180deg)';", matchPattern = "*", type = "userscript"),
                    UserScript(name = "Block Popups", script = "window.open = function() { return null; };", matchPattern = "*", type = "userscript"),
                    UserScript(name = "Force Zoom", script = "document.querySelector('meta[name=viewport]').setAttribute('content', 'width=device-width, initial-scale=1.0, user-scalable=yes');", matchPattern = "*", type = "userscript")
                )
                scope.launch {
                    library.forEach { database.userScriptDao().insertScript(it) }
                }
            }
        )
    }

    if (scriptToEdit != null) {
        AddScriptDialog(
            script = scriptToEdit,
            onDismiss = { scriptToEdit = null },
            onConfirm = { name, code, pattern, type, runAt ->
                scope.launch {
                    database.userScriptDao().insertScript(scriptToEdit!!.copy(name = name, script = code, matchPattern = pattern, type = type, runAt = runAt))
                }
                scriptToEdit = null
            }
        )
    }
}

@Composable
fun ScriptItem(script: UserScript, onDelete: () -> Unit, onEdit: () -> Unit, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(script.name, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                Text(script.matchPattern, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (script.type == "userscript") {
                    Text("Runs at: ${script.runAt.uppercase()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = script.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = Modifier.clickable { onEdit() }
    )
}

@Composable
fun AddScriptDialog(script: UserScript? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, String, String) -> Unit, onLibraryRequest: () -> Unit = {}) {
    var name by remember { mutableStateOf(script?.name ?: "") }
    var code by remember { mutableStateOf(script?.script ?: "") }
    var pattern by remember { mutableStateOf(script?.matchPattern ?: "*") }
    var type by remember { mutableStateOf(script?.type ?: "userscript") }
    var runAt by remember { mutableStateOf(script?.runAt ?: "end") }

    fun parseMetadata(js: String) {
        val lines = js.split("\n")
        var inMeta = false
        for (line in lines) {
            if (line.contains("==UserScript==")) inMeta = true
            if (line.contains("==/UserScript==")) break
            if (inMeta) {
                if (line.contains("@name")) name = line.substringAfter("@name").trim()
                if (line.contains("@match") || line.contains("@include")) {
                    val p = line.substringAfter("@match").substringAfter("@include").trim()
                    if (pattern == "*") pattern = p else if (!pattern.contains(p)) pattern += ", $p"
                }
                if (line.contains("@run-at")) {
                    val r = line.substringAfter("@run-at").trim()
                    runAt = if (r.contains("start")) "start" else "end"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (script == null) "Add Script" else "Edit Script") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == "userscript", onClick = { type = "userscript" })
                    Text("Userscript", modifier = Modifier.clickable { type = "userscript" })
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = type == "bookmarklet", onClick = { type = "bookmarklet" })
                    Text("Bookmarklet", modifier = Modifier.clickable { type = "bookmarklet" })
                }

                if (type == "userscript") {
                    Text("Injection Point:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = runAt == "start", onClick = { runAt = "start" })
                        Text("Document Start", modifier = Modifier.clickable { runAt = "start" })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = runAt == "end", onClick = { runAt = "end" })
                        Text("Document End", modifier = Modifier.clickable { runAt = "end" })
                    }
                }

                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                if (type == "userscript") {
                    TextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Match Pattern (URL)") }, modifier = Modifier.fillMaxWidth())
                }
                TextField(
                    value = code,
                    onValueChange = {
                        code = it
                        if (script == null) parseMetadata(it)
                    },
                    label = { Text("JavaScript Code") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 15
                )

                if (script == null) {
                    TextButton(onClick = { onLibraryRequest(); onDismiss() }) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from Library")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, code, pattern, type, runAt) }) { Text(if (script == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
