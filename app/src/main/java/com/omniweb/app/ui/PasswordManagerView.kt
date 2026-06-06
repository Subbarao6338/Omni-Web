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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val passwords by database.passwordDao().getAllPasswords().collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val filteredPasswords = passwords.filter {
        it.site.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
    }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search passwords") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            if (filteredPasswords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "No saved passwords" else "No matching passwords")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredPasswords) { entry ->
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
                                    val password = try { CryptoUtils.decrypt(entry.encryptedPassword, entry.iv) } catch (e: Exception) { "" }
                                    if (password.isNotEmpty()) {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Password", password))
                                        android.widget.Toast.makeText(context, "Password copied", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
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
}
