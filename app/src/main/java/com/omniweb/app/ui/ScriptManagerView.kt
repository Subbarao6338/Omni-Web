package com.omniweb.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val scripts by database.userScriptDao().getAllScripts().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var scriptToEdit by remember { mutableStateOf<UserScript?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Userscripts & Bookmarklets") },
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
        if (scripts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No scripts added", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(scripts) { script ->
                    ScriptItem(
                        script = script,
                        onDelete = { scope.launch { database.userScriptDao().deleteScript(script) } },
                        onEdit = { scriptToEdit = script }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddScriptDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, code, pattern ->
                scope.launch {
                    database.userScriptDao().insertScript(UserScript(name = name, script = code, matchPattern = pattern))
                }
                showAddDialog = false
            }
        )
    }

    if (scriptToEdit != null) {
        AddScriptDialog(
            script = scriptToEdit,
            onDismiss = { scriptToEdit = null },
            onConfirm = { name, code, pattern ->
                scope.launch {
                    database.userScriptDao().insertScript(scriptToEdit!!.copy(name = name, script = code, matchPattern = pattern))
                }
                scriptToEdit = null
            }
        )
    }
}

@Composable
fun ScriptItem(script: UserScript, onDelete: () -> Unit, onEdit: () -> Unit) {
    ListItem(
        headlineContent = { Text(script.name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(script.matchPattern, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Row {
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
fun AddScriptDialog(script: UserScript? = null, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(script?.name ?: "") }
    var code by remember { mutableStateOf(script?.script ?: "") }
    var pattern by remember { mutableStateOf(script?.matchPattern ?: "*") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (script == null) "Add Script" else "Edit Script") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                TextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Match Pattern (URL)") }, modifier = Modifier.fillMaxWidth())
                TextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("JavaScript Code") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, code, pattern) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
