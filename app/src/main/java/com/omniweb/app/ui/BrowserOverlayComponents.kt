package com.omniweb.app.ui

import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.PerSiteSettings
import com.omniweb.app.data.UserScript

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSettingsDialog(
    host: String,
    settings: PerSiteSettings?,
    onUpdate: (PerSiteSettings) -> Unit,
    onViewPrivacyReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentSettings = settings ?: PerSiteSettings(host)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
            Text("Site Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(host, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            ListItem(
                headlineContent = { Text("Desktop Mode") },
                trailingContent = {
                    Switch(checked = currentSettings.desktopMode, onCheckedChange = {
                        onUpdate(currentSettings.copy(desktopMode = it))
                    })
                },
                leadingContent = { Icon(Icons.Default.Computer, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Ad Blocking") },
                trailingContent = {
                    Switch(checked = currentSettings.adBlockEnabled, onCheckedChange = {
                        onUpdate(currentSettings.copy(adBlockEnabled = it))
                    })
                },
                leadingContent = { Icon(Icons.Default.Shield, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("JavaScript") },
                trailingContent = {
                    Switch(checked = currentSettings.javaScriptEnabled, onCheckedChange = {
                        onUpdate(currentSettings.copy(javaScriptEnabled = it))
                    })
                },
                leadingContent = { Icon(Icons.Default.Javascript, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onViewPrivacyReport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Privacy Report")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextMenuSheet(
    result: WebView.HitTestResult,
    onOpenInNewTab: (String) -> Unit,
    onOpenInBackground: (String) -> Unit,
    onCopyAddress: (String) -> Unit,
    onDownload: (String) -> Unit,
    onHighlight: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
            val extra = result.extra
            when (result.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    Text("Link Options", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ListItem(
                        headlineContent = { Text("Open in New Tab") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable { extra?.let(onOpenInNewTab); onDismiss() }
                    )
                    ListItem(
                        headlineContent = { Text("Open in Background") },
                        leadingContent = { Icon(Icons.Default.Tab, contentDescription = null) },
                        modifier = Modifier.clickable { extra?.let(onOpenInBackground); onDismiss() }
                    )
                    ListItem(
                        headlineContent = { Text("Copy Link Address") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        modifier = Modifier.clickable { extra?.let(onCopyAddress); onDismiss() }
                    )
                }
            }

            ListItem(
                headlineContent = { Text("Highlight Selection") },
                    leadingContent = { Icon(Icons.Default.BorderColor, null) },
                modifier = Modifier.clickable { onHighlight(); onDismiss() }
            )

            when (result.type) {
                WebView.HitTestResult.IMAGE_TYPE -> {
                    Text("Image Options", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ListItem(
                        headlineContent = { Text("Download Image") },
                        leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                        modifier = Modifier.clickable { extra?.let(onDownload); onDismiss() }
                    )
                    ListItem(
                        headlineContent = { Text("Open Image in New Tab") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable { extra?.let(onOpenInNewTab); onDismiss() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeView(
    title: String,
    content: String,
    onClose: () -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(18f) }
    var theme by remember { mutableStateOf("system") } // "light", "dark", "sepia", "system"
    var fontFamilyType by remember { mutableStateOf("serif") } // "serif", "sans", "mono"

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveTheme = if (theme == "system") (if (isSystemDark) "dark" else "light") else theme

    val (backgroundColor, textColor) = when (effectiveTheme) {
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
                            "system" -> "light"
                            "light" -> "sepia"
                            "sepia" -> "dark"
                            else -> "system"
                        }
                    }) {
                        val icon = when(theme) {
                            "system" -> Icons.Default.SettingsSuggest
                            "light" -> Icons.Default.LightMode
                            "sepia" -> Icons.AutoMirrored.Filled.MenuBook
                            else -> Icons.Default.DarkMode
                        }
                        Icon(icon, contentDescription = "Toggle Theme")
                    }
                    IconButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(32f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "Increase Font")
                    }
                    IconButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(12f) }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "Decrease Font")
                    }
                    IconButton(onClick = {
                        fontFamilyType = when(fontFamilyType) {
                            "serif" -> "sans"
                            "sans" -> "mono"
                            else -> "serif"
                        }
                    }) {
                        Icon(Icons.Default.FontDownload, contentDescription = "Toggle Font")
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
                fontFamily = when(fontFamilyType) {
                    "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                    "mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
                    else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            val cleanContent = content
                .replace(Regex("<p.*?>", RegexOption.IGNORE_CASE), "\n\n")
                .replace(Regex("<br.*?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<h[1-6].*?>(.*?)</h[1-6]>", RegexOption.IGNORE_CASE), "\n\n# $1\n\n")
                .replace(Regex("<li.*?>", RegexOption.IGNORE_CASE), "\n• ")
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            Text(
                text = cleanContent,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6).sp,
                color = textColor.copy(alpha = 0.9f),
                fontFamily = when(fontFamilyType) {
                    "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                    "mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
                    else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(
    onNewTab: () -> Unit,
    onSaveToReadingList: () -> Unit,
    onFindInPage: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onReaderMode: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            ListItem(
                headlineContent = { Text("New Tab") },
                leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.clickable { onNewTab(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Save to Reading List") },
                leadingContent = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
                modifier = Modifier.clickable { onSaveToReadingList(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Find in Page") },
                leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.clickable { onFindInPage(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Toggle Desktop Mode") },
                leadingContent = { Icon(Icons.Default.Computer, contentDescription = null) },
                modifier = Modifier.clickable { onDesktopModeToggle(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Reader Mode") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                modifier = Modifier.clickable { onReaderMode(); onDismiss() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
