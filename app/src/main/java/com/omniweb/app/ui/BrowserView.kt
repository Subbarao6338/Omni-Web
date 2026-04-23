package com.omniweb.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.HistoryEntry
import com.omniweb.app.data.MediaItem
import com.omniweb.app.data.Settings
import com.omniweb.app.util.OmniDownloadManager
import com.omniweb.app.util.PageUtils
import com.omniweb.app.util.WebAppInterface
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.clip

private val AD_DOMAINS = listOf(
    "doubleclick.net", "googleadservices.com", "adnxs.com",
    "googlesyndication.com", "quantserve.com", "scorecardresearch.com",
    "zedo.com", "amazon-adsystem.com"
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserView(
    url: String,
    onUrlChange: (String) -> Unit,
    onBackToHome: () -> Unit,
    mediaItems: List<MediaItem>,
    onMediaFound: (List<MediaItem>) -> Unit,
    tabs: List<com.omniweb.app.data.TabInfo>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsState by database.settingsDao().getSettings().collectAsState(initial = Settings())
    val settings = settingsState ?: Settings()
    val bookmarks by database.bookmarkDao().getAllBookmarks().collectAsState(initial = emptyList())
    val isBookmarked = bookmarks.any { it.url == url }
    val userScripts by database.userScriptDao().getAllScripts().collectAsState(initial = emptyList())
    val downloadManager = remember { OmniDownloadManager(context) }

    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var urlInput by remember { mutableStateOf(url) }
    var showTools by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }
    var showConsole by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    var showMediaGrabber by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    var aiSummary by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var pageText by remember { mutableStateOf("") }
    var pageSource by remember { mutableStateOf("") }
    val consoleLogs = remember { mutableStateListOf<ConsoleLog>() }

    var isFindMode by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var isDesktopMode by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied. Cannot download.", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onBackToHome()
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 2.dp, modifier = Modifier.statusBarsPadding()) {
                Column {
                    if (isFindMode) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = findQuery,
                                onValueChange = {
                                    findQuery = it
                                    webView?.findAllAsync(it)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                placeholder = { Text("Find in page...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { webView?.findNext(false) }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous") }
                                        IconButton(onClick = { webView?.findNext(true) }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next") }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                )
                            )
                            TextButton(onClick = {
                                isFindMode = false
                                findQuery = ""
                                webView?.clearMatches()
                            }) {
                                Text("Done")
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onBackToHome) { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            TextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                leadingIcon = {
                                    val icon = if (urlInput.startsWith("https")) Icons.Default.Lock else Icons.Default.Info
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (urlInput.startsWith("https")) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingIcon = {
                                    if (urlInput.isNotEmpty()) {
                                        IconButton(onClick = { urlInput = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    var target = urlInput
                                    if (!target.startsWith("http") && !target.startsWith("about:")) target = "https://$target"
                                    webView?.loadUrl(target)
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    if (isBookmarked) {
                                        bookmarks.find { it.url == url }?.let {
                                            database.bookmarkDao().deleteBookmark(it)
                                        }
                                    } else {
                                        database.bookmarkDao().insertBookmark(Bookmark(title = webView?.title ?: urlInput, url = urlInput))
                                    }
                                }
                            }) {
                                Icon(
                                    if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Bookmark",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                if (isLoading) webView?.stopLoading() else webView?.reload()
                            }) {
                                Icon(
                                    if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (isLoading) "Stop" else "Refresh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (isLoading) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), modifier = Modifier.navigationBarsPadding(), contentPadding = PaddingValues(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    NavButton(Icons.Default.Layers, "Tabs", badge = tabs.size) { showTabs = true }
                    NavButton(Icons.Default.VideoLibrary, "Media", badge = mediaItems.size) { showMediaGrabber = true }
                    NavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back") { webView?.goBack() }
                    NavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Forward") { webView?.goForward() }
                    NavButton(Icons.Default.AutoAwesome, "AI") {
                        scope.launch {
                            isAiLoading = true
                            aiSummary = ""
                            showTools = true
                            try {
                                val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = settings.geminiApiKey.ifEmpty { "YOUR_API_KEY" })
                                val response = model.generateContent(content { text("Summarize this web page content concisely: $pageText") })
                                aiSummary = response.text ?: "No summary available."
                            } catch (e: Exception) {
                                aiSummary = "To use AI features, please configure a Gemini API key in Settings. Error: ${e.message}"
                            } finally {
                                isAiLoading = false
                            }
                        }
                    }
                    NavButton(Icons.Default.Download, "Files") { showDownloads = true }
                    NavButton(Icons.Default.MoreVert, "Menu") { showTools = true }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        this.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        addJavascriptInterface(WebAppInterface(
                            onMediaDetected = { onMediaFound(it) },
                            onTextExtracted = { pageText = it }
                        ), "Android")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                if (newProgress == 100) isLoading = false
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    consoleLogs.add(ConsoleLog(it.message(), it.messageLevel().name))
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                                url?.let { urlInput = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let {
                                    onUrlChange(it)
                                    scope.launch {
                                        database.historyDao().insertHistory(HistoryEntry(title = view?.title ?: it, url = it))
                                    }

                                    // Inject Userscripts - already updated from State
                                    userScripts.filter { it.enabled }.forEach { script ->
                                        try {
                                            val pattern = script.matchPattern.replace("*", ".*")
                                            if (it.matches(Regex(pattern))) {
                                                view?.evaluateJavascript("(function() { ${script.script} })();", null)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                // Extract text for AI
                                view?.evaluateJavascript("Android.postText(document.body.innerText)", null)

                                // Inject Media Sniffer
                                view?.evaluateJavascript("""
                                    (function() {
                                        function sniff() {
                                            const media = [];
                                            const seen = new Set();
                                            document.querySelectorAll('video, audio, source, a[href$=".mp4"], a[href$=".m3u8"], a[href$=".mp3"]').forEach(el => {
                                                const src = el.src || el.getAttribute('src') || el.href;
                                                if (src && src.startsWith('http') && !seen.has(src)) {
                                                    seen.add(src);
                                                    media.push({
                                                        id: Math.random().toString(36).substr(2, 9),
                                                        src: src,
                                                        type: src.split('.').pop().split('?')[0] || 'media',
                                                        title: document.title || 'Media File'
                                                    });
                                                }
                                            });
                                            if (media.length > 0) {
                                                Android.postMedia(JSON.stringify(media));
                                            }
                                        }
                                        if (!window.omniSnifferStarted) {
                                            window.omniSnifferStarted = true;
                                            setInterval(sniff, 10000);
                                            sniff();
                                        }
                                    })();
                                """.trimIndent(), null)
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                if (settings.adBlockEnabled) {
                                    val host = request?.url?.host ?: ""
                                    if (AD_DOMAINS.any { host.contains(it) }) {
                                        return WebResourceResponse("text/plain", "UTF-8", null)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                update = { view ->
                    if (view.url != url && !url.startsWith("about:")) {
                        view.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

        }
    }

    if (showTabs) {
        ModalBottomSheet(onDismissRequest = { showTabs = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = {
                        onNewTab()
                        showTabs = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onTabSelected(tab.id)
                                    showTabs = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (tab.id == activeTabId) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (tab.id == activeTabId) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Column(modifier = Modifier.align(Alignment.TopStart)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (tab.id == activeTabId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tab.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = { onCloseTab(tab.id) },
                                    modifier = Modifier.align(Alignment.BottomEnd).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showTools) {
        ModalBottomSheet(onDismissRequest = { showTools = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
                Text("Page Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ToolButton(Icons.Default.AutoAwesome, "AI Summary", Color(0xFF9333EA)) {
                            scope.launch {
                                isAiLoading = true
                                aiSummary = ""
                                try {
                                    val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = settings.geminiApiKey.ifEmpty { "YOUR_API_KEY" })
                                    val response = model.generateContent(content { text("Summarize this web page content concisely: $pageText") })
                                    aiSummary = response.text ?: "No summary available."
                                } catch (e: Exception) {
                                    aiSummary = "To use AI features, please configure a Gemini API key in Settings. Error: ${e.message}"
                                } finally {
                                    isAiLoading = false
                                }
                            }
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Code, "View Source", Color(0xFFEA580C)) {
                            webView?.evaluateJavascript("document.documentElement.outerHTML") { source ->
                                pageSource = source ?: "No source available"
                                showSource = true
                            }
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Terminal, "Console", Color(0xFF10B981)) {
                            showConsole = true
                        }
                    }
                    item {
                        ToolButton(if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone, if (isDesktopMode) "Mobile Site" else "Desktop Site", Color(0xFF6366F1)) {
                            isDesktopMode = !isDesktopMode
                            webView?.settings?.userAgentString = if (isDesktopMode) {
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            } else {
                                null // Use default
                            }
                            webView?.reload()
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Search, "Find", Color(0xFF3B82F6)) {
                            isFindMode = true
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.AddHome, "Add Home", Color(0xFF10B981)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                                if (shortcutManager!!.isRequestPinShortcutSupported) {
                                    val pinShortcutInfo = ShortcutInfo.Builder(context, urlInput)
                                        .setShortLabel(webView?.title ?: "Web Page")
                                        .setIcon(Icon.createWithResource(context, com.omniweb.app.R.mipmap.ic_launcher))
                                        .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(urlInput)))
                                        .build()
                                    shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                                    Toast.makeText(context, "Adding to home screen...", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.PictureAsPdf, "Save PDF", Color(0xFFEF4444)) {
                            webView?.let { PageUtils.saveAsPdf(context, it, it.title ?: "Page") }
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Star, "Bookmarks", Color(0xFFFFB000)) {
                            showBookmarks = true
                        }
                    }
                    item {
                        ToolButton(Icons.Default.History, "History", Color(0xFF607D8B)) {
                            showHistory = true
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Settings, "Settings", Color(0xFF4B5563)) {
                            showSettings = true
                        }
                    }
                }

                if (isAiLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
                if (aiSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("AI Summary", aiSummary)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(aiSummary, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showSource) {
        ViewSourceView(source = pageSource) { showSource = false }
    }

    if (showConsole) {
        ConsoleView(logs = consoleLogs, onClear = { consoleLogs.clear() }) { showConsole = false }
    }

    if (showDownloads) {
        DownloadsView(database = database) { showDownloads = false }
    }

    if (showMediaGrabber) {
        MediaGrabberView(mediaItems = mediaItems, onDownload = { item ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            downloadManager.startDownload(item.src, item.title)
        }) { showMediaGrabber = false }
    }

    if (showSettings) {
        SettingsView(database = database) { showSettings = false }
    }

    if (showBookmarks) {
        BookmarksView(database = database, onNavigate = { webView?.loadUrl(it); showBookmarks = false }, onBack = { showBookmarks = false })
    }

    if (showHistory) {
        HistoryView(database = database, onNavigate = { webView?.loadUrl(it); showHistory = false }, onBack = { showHistory = false })
    }
}

@Composable
fun NavButton(icon: ImageVector, label: String, badge: Int = 0, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (badge > 0) {
                Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), shape = CircleShape) {
                    Text(badge.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 8.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ToolButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
