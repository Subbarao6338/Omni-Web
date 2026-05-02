package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.PasswordEntry
import com.omniweb.app.util.CryptoUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerView(database: AppDatabase, onBack: () -> Unit) {
    val passwords by database.passwordDao().getAllPasswords().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var visiblePasswords by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (passwords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No saved passwords")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(passwords) { entry ->
                    val isVisible = visiblePasswords.contains(entry.id)
                    ListItem(
                        headlineContent = { Text(entry.site) },
                        supportingContent = {
                            Column {
                                Text("User: ${entry.username}")
                                val decryptedPassword = remember(entry.id, isVisible) {
                                    if (isVisible) {
                                        try {
                                            CryptoUtils.decrypt(entry.password, entry.iv)
                                        } catch (e: Exception) {
                                            "Error decrypting"
                                        }
                                    } else {
                                        "••••••••"
                                    }
                                }
                                Text("Pass: $decryptedPassword")
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    visiblePasswords = if (isVisible) visiblePasswords - entry.id else visiblePasswords + entry.id
                                }) {
                                    Icon(if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle Visibility")
                                }
                                IconButton(onClick = {
                                    scope.launch { database.passwordDao().deletePassword(entry) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
