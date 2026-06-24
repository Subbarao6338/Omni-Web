package com.omniweb.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlView(
    database: AppDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsState by database.settingsDao().getSettings().collectAsStateWithLifecycle(initialValue = null)
    val settings = settingsState ?: Settings()

    var isUnlocked by remember(settings.parentalPassword) {
        mutableStateOf(settings.parentalPassword == null)
    }
    var passwordInput by remember { mutableStateOf("") }

    var showAddBlockDialog by remember { mutableStateOf(false) }
    var newBlockUrl by remember { mutableStateOf("") }

    val blockedSitesList = remember(settings.blockedSites) {
        try {
            val arr = JSONArray(settings.blockedSites ?: "[]")
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental Controls", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!isUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Enter Parental Password", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (passwordInput == settings.parentalPassword) {
                            isUnlocked = true
                        } else {
                            Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                SettingsSection("Password Protection", Icons.Default.Security) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var newPassword by remember { mutableStateOf("") }
                        Text("Set or change parental password. Leave empty to disable.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            scope.launch {
                                database.settingsDao().updateSettings(settings.copy(parentalPassword = if (newPassword.isBlank()) null else newPassword))
                                Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Save Password")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Blocked Sites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAddBlockDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    items(blockedSitesList) { site ->
                        ListItem(
                            headlineContent = { Text(site) },
                            trailingContent = {
                                IconButton(onClick = {
                                    val newList = blockedSitesList.filter { it != site }
                                    scope.launch {
                                        database.settingsDao().updateSettings(settings.copy(blockedSites = JSONArray(newList).toString()))
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Block Domain") },
            text = {
                OutlinedTextField(
                    value = newBlockUrl,
                    onValueChange = { newBlockUrl = it },
                    placeholder = { Text("example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newBlockUrl.isNotBlank()) {
                        val newList = blockedSitesList + newBlockUrl.trim().lowercase()
                        scope.launch {
                            database.settingsDao().updateSettings(settings.copy(blockedSites = JSONArray(newList).toString()))
                        }
                        newBlockUrl = ""
                        showAddBlockDialog = false
                    }
                }) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}
