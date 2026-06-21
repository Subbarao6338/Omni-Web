package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagerView(
    onProfileSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val profiles = remember { mutableStateListOf("default") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Profiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Profile")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(profiles) { profileId ->
                ListItem(
                    headlineContent = { Text(profileId) },
                    trailingContent = {
                        if (profileId != "default") {
                            IconButton(onClick = {
                                profiles.remove(profileId)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onProfileSelected(profileId) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Profile") },
            text = {
                TextField(value = newProfileName, onValueChange = { newProfileName = it }, placeholder = { Text("Profile Name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newProfileName.isNotBlank()) {
                        profiles.add(newProfileName)
                        showAddDialog = false
                        newProfileName = ""
                    }
                }) { Text("Create") }
            }
        )
    }
}
