package com.omniweb.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.PasswordEntry
import com.omniweb.app.data.UserScript
import com.omniweb.app.util.UrlUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyReportDialog(
    blockedTrackers: List<String>,
    onDismiss: () -> Unit
) {
    val ads = blockedTrackers.filter { it.startsWith("[Ad]") }
    val analytics = blockedTrackers.filter { it.startsWith("[Analytics]") }
    val social = blockedTrackers.filter { it.startsWith("[Social]") }
    val others = blockedTrackers.filter { !it.startsWith("[Ad]") && !it.startsWith("[Analytics]") && !it.startsWith("[Social]") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Report")
            }
        },
        text = {
            Column {
                Text("${blockedTrackers.size} trackers blocked on this page", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                if (blockedTrackers.isEmpty()) {
                    Text("No trackers detected. This site respects your privacy!")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        if (ads.isNotEmpty()) {
                            item { Text("Ads (${ads.size})", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.padding(vertical = 4.dp)) }
                            items(ads) { Text(it.removePrefix("[Ad] "), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) }
                        }
                        if (analytics.isNotEmpty()) {
                            item { Text("Analytics (${analytics.size})", fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6), modifier = Modifier.padding(vertical = 4.dp)) }
                            items(analytics) { Text(it.removePrefix("[Analytics] "), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) }
                        }
                        if (social.isNotEmpty()) {
                            item { Text("Social (${social.size})", fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), modifier = Modifier.padding(vertical = 4.dp)) }
                            items(social) { Text(it.removePrefix("[Social] "), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) }
                        }
                        if (others.isNotEmpty()) {
                            item { Text("Other (${others.size})", fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(vertical = 4.dp)) }
                            items(others) { Text(it, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PasswordSaveDialog(
    site: String,
    user: String,
    pass: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Password?") },
        text = { Text("Would you like to save the password for $user on $site?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No thanks") }
        }
    )
}

@Composable
fun AddBookmarkletDialog(
    script: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmarklet?") },
        text = { Text("This looks like a bookmarklet. Would you like to add it to your script manager?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextMenuSheet(
    result: WebView.HitTestResult,
    onNewTab: (String) -> Unit,
    onDownload: (String, String) -> Unit,
    onAddBookmarklet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
            val extra = result.extra
            when (result.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    Text("Link Options", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ListItem(
                        headlineContent = { Text("Open in New Tab") },
                        leadingContent = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable {
                            extra?.let { onNewTab(it) }
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Copy Link Address") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        modifier = Modifier.clickable {
                            extra?.let {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", it))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    )
                    if (extra != null && UrlUtils.isBookmarklet(extra)) {
                        ListItem(
                            headlineContent = { Text("Add to Bookmarklets") },
                            leadingContent = { Icon(Icons.Default.Javascript, contentDescription = null) },
                            modifier = Modifier.clickable {
                                onAddBookmarklet(extra)
                                onDismiss()
                            }
                        )
                    }
                }
                WebView.HitTestResult.IMAGE_TYPE -> {
                    Text("Image Options", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ListItem(
                        headlineContent = { Text("Download Image") },
                        leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                        modifier = Modifier.clickable {
                            extra?.let { onDownload(it, "Image") }
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Open Image in New Tab") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable {
                            extra?.let { onNewTab(it) }
                            onDismiss()
                        }
                    )
                }
                WebView.HitTestResult.PHONE_TYPE -> {
                    ListItem(
                        headlineContent = { Text("Call $extra") },
                        leadingContent = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$extra"))
                            context.startActivity(intent)
                            onDismiss()
                        }
                    )
                }
                WebView.HitTestResult.EMAIL_TYPE -> {
                    ListItem(
                        headlineContent = { Text("Email $extra") },
                        leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$extra"))
                            context.startActivity(intent)
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeView(title: String, content: String, onClose: () -> Unit) {
    var fontSize by remember { mutableFloatStateOf(18f) }
    var theme by remember { mutableStateOf("light") } // "light", "dark", "sepia"
    var isSerif by remember { mutableStateOf(true) }

    val (backgroundColor, textColor) = when (theme) {
        "dark" -> Color(0xFF121212) to Color(0xFFE0E0E0)
        "sepia" -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
        else -> Color(0xFFFFFFFF) to Color(0xFF1A1A1A)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        theme = when(theme) {
                            "light" -> "sepia"
                            "sepia" -> "dark"
                            else -> "light"
                        }
                    }) {
                        val icon = when(theme) {
                            "light" -> Icons.Default.MenuBook
                            "sepia" -> Icons.Default.DarkMode
                            else -> Icons.Default.LightMode
                        }
                        Icon(icon, contentDescription = "Toggle Theme")
                    }
                    IconButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(32f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "Increase Font")
                    }
                    IconButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(12f) }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "Decrease Font")
                    }
                    IconButton(onClick = { isSerif = !isSerif }) {
                        Icon(if (isSerif) Icons.Default.FontDownload else Icons.Default.FontDownloadOff, contentDescription = "Toggle Font")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor,
                    actionIconContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = (fontSize * 1.5).sp,
                fontWeight = FontWeight.Black,
                lineHeight = (fontSize * 1.8).sp,
                color = textColor,
                fontFamily = if (isSerif) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(24.dp))
            val cleanContent = content.replace(Regex("<[^>]*>"), "")
            Text(
                text = cleanContent,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6).sp,
                color = textColor.copy(alpha = 0.9f),
                fontFamily = if (isSerif) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.SansSerif
            )
        }
    }
}
