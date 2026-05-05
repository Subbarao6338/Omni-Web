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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omniweb.app.util.CryptoUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.PasswordEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerView(database: AppDatabase, onBack: () -> Unit) {
    val passwords by database.passwordDao().getAllPasswords().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

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
                    var showPassword by remember { mutableStateOf(false) }
                    val decryptedPassword = remember(entry, showPassword) {
                        if (showPassword) {
                            try {
                                CryptoUtils.decrypt(entry.encryptedPassword, entry.iv)
                            } catch (e: Exception) {
                                "Error decrypting"
                            }
                        } else {
                            "••••••••"
                        }
                    }

                    ListItem(
                        headlineContent = { Text(entry.site) },
                        supportingContent = {
                            Column {
                                Text(entry.username)
                                Text(decryptedPassword, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password"
                                    )
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
