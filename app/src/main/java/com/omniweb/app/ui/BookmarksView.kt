package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksView(
    database: AppDatabase,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BrowserViewModel? = null
) {
    val context = LocalContext.current
    val bookmarks by database.bookmarkDao().getAllBookmarks().collectAsStateWithLifecycle(initialValue = emptyList())
    var sessions by remember { mutableStateOf<List<com.omniweb.app.data.NamedSession>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel?.let { sessions = it.getAllSessions() }
    }

    val filteredBookmarks = if (searchQuery.isBlank()) {
        bookmarks
    } else {
        bookmarks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true) }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val imported = com.omniweb.app.util.NetscapeBookmarkFormatImporter.import(stream)
                        database.bookmarkDao().insertBookmarks(imported)
                        Toast.makeText(context, "Imported ${imported.size} bookmarks", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val htmlContent = com.omniweb.app.util.BookmarkExporter.exportToHtml(bookmarks)
                        outputStream.write(htmlContent.toByteArray())
                        Toast.makeText(context, "Bookmarks exported successfully as HTML", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val txtContent = com.omniweb.app.util.BookmarkExporter.exportToTxt(bookmarks)
                        outputStream.write(txtContent.toByteArray())
                        Toast.makeText(context, "Bookmarks exported successfully as TXT", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Collections", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Bookmark Options")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Import Bookmarks") },
                                        onClick = {
                                            showMenu = false
                                            importFileLauncher.launch(arrayOf("text/html", "text/plain"))
                                        },
                                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as HTML") },
                                        onClick = {
                                            showMenu = false
                                            exportHtmlLauncher.launch("bookmarks.html")
                                        },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as TXT") },
                                        onClick = {
                                            showMenu = false
                                            exportTxtLauncher.launch("bookmarks.txt")
                                        },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                                    )
                                }
                            }
                        }
                        if (selectedTab == 1 && viewModel != null) {
                            var showSessionDialog by remember { mutableStateOf(false) }
                            IconButton(onClick = { showSessionDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Save Session")
                            }
                            if (showSessionDialog) {
                                var sessionName by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showSessionDialog = false },
                                    title = { Text("Save Session") },
                                    text = { TextField(value = sessionName, onValueChange = { sessionName = it }, placeholder = { Text("Session Name") }) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.saveCurrentSession(sessionName)
                                            scope.launch { sessions = viewModel.getAllSessions() }
                                            showSessionDialog = false
                                        }) { Text("Save") }
                                    }
                                )
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bookmarks") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sessions") })
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search bookmarks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    ) { padding ->
        if (selectedTab == 0) {
            if (filteredBookmarks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "No bookmarks yet" else "No matching bookmarks found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(filteredBookmarks) { bookmark ->
                        ListItem(
                            headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = {
                                    scope.launch { database.bookmarkDao().deleteBookmark(bookmark) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.clickable { onNavigate(bookmark.url) }
                        )
                    }
                }
            }
        } else {
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No saved sessions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(sessions) { session ->
                        ListItem(
                            headlineContent = { Text(session.name) },
                            leadingContent = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = {
                                    viewModel?.deleteSession(session)
                                    scope.launch { sessions = viewModel?.getAllSessions() ?: emptyList() }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel?.restoreSession(session.name)
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    }
}
