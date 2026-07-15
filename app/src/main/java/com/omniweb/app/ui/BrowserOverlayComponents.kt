package com.omniweb.app.ui

import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    onClearData: () -> Unit,
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

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Custom User Agent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = currentSettings.customUserAgent ?: "",
                    onValueChange = { onUpdate(currentSettings.copy(customUserAgent = it.ifBlank { null })) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Default") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }

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
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        val cookies = cookieManager.getCookie(host)
                        if (cookies != null) {
                            val cookieArray = cookies.split(";")
                            for (cookie in cookieArray) {
                                val parts = cookie.split("=")
                                if (parts.isNotEmpty()) {
                                    cookieManager.setCookie(host, parts[0].trim() + "=; Max-Age=0")
                                }
                            }
                        }
                        cookieManager.flush()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Default.Cookie, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Cookies", fontSize = 12.sp)
                }
                Button(
                    onClick = onClearData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Data", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyReportView(
    blockedTrackers: List<String>,
    onBack: () -> Unit
) {
    val ads = blockedTrackers.filter { it.startsWith("[Ad]") }
    val analytics = blockedTrackers.filter { it.startsWith("[Analytics]") }
    val social = blockedTrackers.filter { it.startsWith("[Social]") }
    val cryptomining = blockedTrackers.filter { it.startsWith("[Cryptomining]") }
    val fingerprinting = blockedTrackers.filter { it.startsWith("[Fingerprinting]") }
    val others = blockedTrackers.filter {
        !it.startsWith("[Ad]") && !it.startsWith("[Analytics]") &&
        !it.startsWith("[Social]") && !it.startsWith("[Cryptomining]") &&
        !it.startsWith("[Fingerprinting]")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("${blockedTrackers.size} Trackers Blocked", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF10B981))
                        Text("Omni Browser is protecting your privacy", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Blocked Content Types", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (blockedTrackers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trackers detected on this page.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (ads.isNotEmpty()) {
                        item { TrackerCategoryItem("Ads", ads.size, Color(0xFFEF4444), Icons.Default.AdUnits, ads.map { it.removePrefix("[Ad] ") }) }
                    }
                    if (analytics.isNotEmpty()) {
                        item { TrackerCategoryItem("Analytics", analytics.size, Color(0xFF3B82F6), Icons.Default.Analytics, analytics.map { it.removePrefix("[Analytics] ") }) }
                    }
                    if (social.isNotEmpty()) {
                        item { TrackerCategoryItem("Social", social.size, Color(0xFF8B5CF6), Icons.Default.People, social.map { it.removePrefix("[Social] ") }) }
                    }
                    if (cryptomining.isNotEmpty()) {
                        item { TrackerCategoryItem("Cryptomining", cryptomining.size, Color(0xFFF59E0B), Icons.Default.CurrencyBitcoin, cryptomining.map { it.removePrefix("[Cryptomining] ") }) }
                    }
                    if (fingerprinting.isNotEmpty()) {
                        item { TrackerCategoryItem("Fingerprinting", fingerprinting.size, Color(0xFF10B981), Icons.Default.Fingerprint, fingerprinting.map { it.removePrefix("[Fingerprinting] ") }) }
                    }
                    if (others.isNotEmpty()) {
                        item { TrackerCategoryItem("Other", others.size, Color(0xFF6B7280), Icons.Default.MoreHoriz, others) }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerCategoryItem(name: String, count: Int, color: Color, icon: ImageVector, items: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(count.toString(), fontWeight = FontWeight.ExtraBold, color = color)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Text(item, fontSize = 11.sp, modifier = Modifier.padding(start = 36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
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
    onExplain: () -> Unit,
    onSearch: () -> Unit,
    onCopyAsMarkdown: () -> Unit,
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

            ListItem(
                headlineContent = { Text("Search for Selection") },
                leadingContent = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.clickable { onSearch(); onDismiss() }
            )

            ListItem(
                headlineContent = { Text("Explain Selection (AI)") },
                leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                modifier = Modifier.clickable { onExplain(); onDismiss() }
            )

            ListItem(
                headlineContent = { Text("Copy as Markdown") },
                leadingContent = { Icon(Icons.Default.Description, null) },
                modifier = Modifier.clickable { onCopyAsMarkdown(); onDismiss() }
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
    settings: com.omniweb.app.data.Settings,
    onUpdateSettings: (com.omniweb.app.data.Settings) -> Unit,
    onExportMarkdown: () -> Unit,
    onClose: () -> Unit
) {
    val fontSize = settings.readerFontSize
    val theme = settings.readerTheme
    val fontFamilyType = settings.readerFontFamily

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveTheme = if (theme == "system") (if (isSystemDark) "dark" else "light") else theme

    val (backgroundColor, textColor) = when (effectiveTheme) {
        "dark" -> Color(0xFF121212) to Color(0xFFE0E0E0)
        "sepia" -> Color(0xFFFBF0D9) to Color(0xFF5B4636)
        else -> Color(0xFFFFFFFF) to Color(0xFF1A1A1A)
    }

    var lineSpacing by remember { mutableFloatStateOf(1.6f) }
    var letterSpacing by remember { mutableFloatStateOf(0f) }

    val fontStack = when(fontFamilyType) {
        "serif" -> "serif"
        "mono" -> "monospace"
        else -> "sans-serif"
    }

    val readerHtml = """
        <html>
        <head>
            <style>
                body {
                    background-color: ${String.format("#%06X", (backgroundColor.value.toLong() and 0xFFFFFF))};
                    color: ${String.format("#%06X", (textColor.value.toLong() and 0xFFFFFF))};
                    font-family: $fontStack;
                    font-size: ${fontSize}px;
                    line-height: $lineSpacing;
                    letter-spacing: ${letterSpacing}px;
                    padding: 24px;
                    margin: 0;
                    transition: all 0.3s ease;
                }
                h1.reader-title {
                    font-size: 1.5em;
                    font-weight: 900;
                    line-height: 1.2;
                    margin-bottom: 24px;
                }
                img {
                    max-width: 100%;
                    height: auto;
                    border-radius: 8px;
                    margin: 16px 0;
                }
                pre, code {
                    background-color: rgba(0,0,0,0.05);
                    padding: 4px;
                    border-radius: 4px;
                    font-family: monospace;
                }
                blockquote {
                    border-left: 4px solid #ccc;
                    padding-left: 16px;
                    margin-left: 0;
                    font-style: italic;
                }
                a {
                    color: inherit;
                    text-decoration: underline;
                }
            </style>
        </head>
        <body>
            <h1 class='reader-title'>$title</h1>
            $content
        </body>
        </html>
    """.trimIndent()

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
                    IconButton(onClick = onExportMarkdown) {
                        Icon(Icons.Default.Description, contentDescription = "Export to MD")
                    }
                    IconButton(onClick = {
                        val nextTheme = when(theme) {
                            "system" -> "light"
                            "light" -> "sepia"
                            "sepia" -> "dark"
                            else -> "system"
                        }
                        onUpdateSettings(settings.copy(readerTheme = nextTheme))
                    }) {
                        val icon = when(theme) {
                            "system" -> Icons.Default.SettingsSuggest
                            "light" -> Icons.Default.LightMode
                            "sepia" -> Icons.AutoMirrored.Filled.MenuBook
                            else -> Icons.Default.DarkMode
                        }
                        Icon(icon, contentDescription = "Toggle Theme")
                    }
                    IconButton(onClick = {
                        onUpdateSettings(settings.copy(readerFontSize = (fontSize + 2f).coerceAtMost(32f)))
                    }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "Increase Font")
                    }
                    IconButton(onClick = {
                        onUpdateSettings(settings.copy(readerFontSize = (fontSize - 2f).coerceAtLeast(12f)))
                    }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "Decrease Font")
                    }
                    IconButton(onClick = {
                        val nextFont = when(fontFamilyType) {
                            "serif" -> "sans"
                            "sans" -> "mono"
                            else -> "serif"
                        }
                        onUpdateSettings(settings.copy(readerFontFamily = nextFont))
                    }) {
                        Icon(Icons.Default.FontDownload, contentDescription = "Toggle Font")
                    }
                    IconButton(onClick = {
                        lineSpacing = if (lineSpacing > 2.0f) 1.2f else lineSpacing + 0.2f
                    }) {
                        Icon(Icons.Default.FormatLineSpacing, contentDescription = "Line Spacing")
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
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    setBackgroundColor(backgroundColor.value.toInt())
                    this.settings.apply {
                        javaScriptEnabled = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, readerHtml, "text/html", "UTF-8", null)
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        )
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
