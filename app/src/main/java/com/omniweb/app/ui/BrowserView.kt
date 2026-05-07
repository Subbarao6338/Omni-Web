package com.omniweb.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.HistoryEntry
import com.omniweb.app.data.MediaItem
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.data.ReadingListEntry
import com.omniweb.app.util.AdBlockManager
import com.omniweb.app.util.OmniDownloadManager
import com.omniweb.app.util.PageUtils
import com.omniweb.app.util.UrlUtils
import com.omniweb.app.util.WebAppInterface
import com.omniweb.app.util.CryptoUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserView(
    activeTab: TabInfo,
    onBackToHome: () -> Unit,
    viewModel: BrowserViewModel,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: Settings()
    val bookmarks by database.bookmarkDao().getAllBookmarks().collectAsState(initial = emptyList())
    val isBookmarked = bookmarks.any { it.url == activeTab.url }
    val userScripts by database.userScriptDao().getAllScripts().collectAsState(initial = emptyList())
    val downloadManager = remember { OmniDownloadManager(context) }

    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOfFirst { it.id == activeTab.id }.coerceAtLeast(0),
        pageCount = { tabs.size }
    )

    LaunchedEffect(activeTab.id) {
        val index = tabs.indexOfFirst { it.id == activeTab.id }
        if (index != -1 && pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page >= 0 && page < tabs.size) {
                viewModel.selectTab(tabs[page].id)
            }
        }
    }

    var urlInput by remember { mutableStateOf(activeTab.url) }

    var showTools by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }

    var showSource by remember { mutableStateOf(false) }
    var showConsole by remember { mutableStateOf(false) }
    var showMediaGrabber by remember { mutableStateOf(false) }
    var showBookmarklets by remember { mutableStateOf(false) }

    var pageText by remember { mutableStateOf("") }
    var pageSource by remember { mutableStateOf("") }
    val consoleLogs = remember { mutableStateListOf<ConsoleLog>() }

    var isFindMode by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchStatus by remember { mutableStateOf("") }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isForceDark by remember { mutableStateOf(false) }
    var isReaderMode by remember { mutableStateOf(false) }
    var readerContent by remember { mutableStateOf("") }

    var showPrivacyReport by remember { mutableStateOf(false) }
    var showSiteSettings by remember { mutableStateOf(false) }

    var passwordToSave by remember { mutableStateOf<Triple<String, String, String>?>(null) } // site, user, pass

    var showAddBookmarkletDialog by remember { mutableStateOf<String?>(null) }

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuResult by remember { mutableStateOf<WebView.HitTestResult?>(null) }

    var showQuickActions by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied. Cannot download.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(activeTab.id) {
        urlInput = activeTab.url
    }

    BackHandler {
        val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
        if (currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else {
            onBackToHome()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                if (activeTab.isLoading && !isFindMode) {
                    LinearProgressIndicator(
                        progress = { activeTab.progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
                BrowserAddressBar(
                    urlInput = urlInput,
                    onUrlChange = {
                        urlInput = it
                        viewModel.updateSuggestions(it)
                    },
                    onGo = {
                        val input = urlInput.trim()
                        if (input.isNotEmpty()) {
                            val target = UrlUtils.resolveUrl(input, settings.searchEngine)
                            if (target == "about:home") onBackToHome() else viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                        }
                        viewModel.updateSuggestions("")
                    },
                    onRefresh = { viewModel.getOrCreateWebView(activeTab.id, context).reload() },
                    onStop = { viewModel.getOrCreateWebView(activeTab.id, context).stopLoading() },
                    isLoading = activeTab.isLoading,
                    pageFavicon = activeTab.faviconBitmap,
                    onPrivacyClick = { showSiteSettings = true },
                    onBookmarkClick = {
                        scope.launch {
                            if (isBookmarked) {
                                bookmarks.find { it.url == activeTab.url }?.let { database.bookmarkDao().deleteBookmark(it) }
                            } else {
                                database.bookmarkDao().insertBookmark(Bookmark(title = activeTab.title ?: urlInput, url = urlInput))
                            }
                        }
                    },
                    isBookmarked = isBookmarked,
                    isFindMode = isFindMode,
                    findQuery = findQuery,
                    onFindQueryChange = {
                        findQuery = it
                        viewModel.getOrCreateWebView(activeTab.id, context).findAllAsync(it)
                    },
                    onFindNext = { forward -> viewModel.getOrCreateWebView(activeTab.id, context).findNext(forward) },
                    findMatchStatus = findMatchStatus,
                    onCloseFind = {
                        isFindMode = false
                        findQuery = ""
                        findMatchStatus = ""
                        viewModel.getOrCreateWebView(activeTab.id, context).clearMatches()
                    },
                    onHomeClick = onBackToHome,
                    suggestions = if (urlInput != activeTab.url) viewModel.searchSuggestions.value else emptyList(),
                    onSuggestionClick = { suggestion ->
                        val target = UrlUtils.resolveUrl(suggestion.url, settings.searchEngine)
                        if (target == "about:home") onBackToHome() else {
                            urlInput = target
                            viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                        }
                        viewModel.updateSuggestions("")
                    },
                    blockedCount = synchronized(viewModel.blockedTrackersByTab) { viewModel.blockedTrackersByTab[activeTab.id]?.size ?: 0 }
                )
            }
        },
        bottomBar = {
            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
            BrowserBottomBar(
                tabCount = viewModel.tabs.size,
                mediaCount = activeTab.detectedMedia.size,
                onShowTabs = { showTabs = true },
            onNewTab = { showQuickActions = true },
                onShowMedia = { showMediaGrabber = true },
                onBack = { if (currentWebView.canGoBack()) currentWebView.goBack() else onBackToHome() },
                onForward = { if (currentWebView.canGoForward()) currentWebView.goForward() },
                onShowDownloads = onOpenDownloads,
                onShowMenu = { showTools = true }
            )
        }
    ) { padding ->
    val lifecycleOwner = LocalLifecycleOwner.current
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.padding(padding).fillMaxSize(),
        userScrollEnabled = false
    ) { pageIndex ->
        val tab = tabs[pageIndex]
        WebViewContainer(
            tab = tab,
            viewModel = viewModel,
            settings = settings,
            onLoginDetected = { site, user, pass ->
                passwordToSave = Triple(site, user, pass)
            },
            onBookmarkletDetected = { url ->
                showAddBookmarkletDialog = url
            },
            onTextExtracted = { text ->
                if (tab.id == activeTab.id) pageText = text
            },
            onScrollChanged = { x, y ->
                viewModel.updateTabScroll(tab.id, x, y)
            },
            onContextMenu = { result ->
                contextMenuResult = result
                showContextMenu = true
            },
            onProgressChanged = { progress ->
                tab.progress = progress
            },
            onTitleReceived = { title ->
                tab.title = title
                viewModel.updateTabInDb(tab)
                if (!tab.isIncognito) {
                    scope.launch {
                        database.historyDao().insertHistory(HistoryEntry(title = title, url = tab.url))
                    }
                }
            },
            onIconReceived = { icon ->
                tab.faviconBitmap = icon
            },
            onConsoleLog = { msg, level ->
                consoleLogs.add(ConsoleLog(msg, level))
            }
        )
    }

    if (showTabs) {
        val activeId by viewModel.activeTabId.collectAsState()
        TabSwitcherSheet(
            tabs = viewModel.tabs,
            activeTabId = activeId,
            onTabSelect = { viewModel.selectTab(it) },
            onTabClose = { id ->
                viewModel.closeTab(id)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Tab closed",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreLastClosedTab()
                    }
                }
            },
            onCloseAll = {
                viewModel.tabs.toList().forEach { viewModel.closeTab(it.id) }
                showTabs = false
            },
            onNewTab = { viewModel.createTab(isIncognito = it) },
            onDismiss = { showTabs = false }
        )
    }

    if (showTools) {
        ModalBottomSheet(onDismissRequest = { showTools = false }, containerColor = MaterialTheme.colorScheme.surface) {
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
                        ToolButton(Icons.Default.Add, "New Tab", Color(0xFF10B981)) {
                            viewModel.createTab()
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.VisibilityOff, "New Incognito", Color(0xFF6366F1)) {
                            viewModel.createTab(isIncognito = true)
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Share, "Share", Color(0xFF3B82F6)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            currentWebView.url?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, it)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Link"))
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.ContentCopy, "Copy URL", Color(0xFF8B5CF6)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            currentWebView.url?.let {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", it))
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Print, "Print", Color(0xFF4B5563)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                            val printAdapter = currentWebView.createPrintDocumentAdapter("Document")
                            printManager.print("Omni Browser Document", printAdapter, null)
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Translate, "Translate", Color(0xFF3B82F6)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            val targetUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(currentWebView.url)}"
                            currentWebView.loadUrl(targetUrl)
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Javascript, "Bookmarklets", Color(0xFFFACC15)) {
                            showBookmarklets = true
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Archive, "Save MHTML", Color(0xFF8B5CF6)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            scope.launch {
                                val path = PageUtils.saveAsMhtml(context, currentWebView, currentWebView.title ?: "Page")
                                database.readingListDao().insertEntry(com.omniweb.app.data.ReadingListEntry(title = activeTab.title, url = activeTab.url, filePath = path))
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Description, "Save MD", Color(0xFF10B981)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            currentWebView.evaluateJavascript("document.documentElement.outerHTML") { source: String? ->
                                val cleanSource = if (source != null && source.startsWith("\"") && source.endsWith("\"")) {
                                    source.substring(1, source.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                } else {
                                    source ?: ""
                                }
                                PageUtils.saveAsMarkdown(context, cleanSource, currentWebView.title ?: "Page")
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Code, "View Source", Color(0xFFEA580C)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            currentWebView.evaluateJavascript("document.documentElement.outerHTML") { source: String? ->
                                pageSource = source ?: "No source available"
                                showSource = true
                                showTools = false
                            }
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Terminal, "Console", Color(0xFF10B981)) {
                            showConsole = true
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone, if (isDesktopMode) "Mobile Site" else "Desktop Site", Color(0xFF6366F1)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            isDesktopMode = !isDesktopMode
                            currentWebView.settings.userAgentString = if (isDesktopMode) {
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            } else {
                                null // Use default
                            }
                            currentWebView.reload()
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
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                                if (shortcutManager!!.isRequestPinShortcutSupported) {
                                    val pinShortcutInfo = ShortcutInfo.Builder(context, urlInput)
                                        .setShortLabel(currentWebView.title ?: "Web Page")
                                        .setIcon(if (activeTab.faviconBitmap != null) Icon.createWithBitmap(activeTab.faviconBitmap) else Icon.createWithResource(context, com.omniweb.app.R.mipmap.ic_launcher))
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
                        ToolButton(Icons.Default.CameraAlt, "Full Shot", Color(0xFF06B6D4)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            PageUtils.takeFullPageScreenshot(context, currentWebView, currentWebView.title ?: "Page")
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.PictureAsPdf, "Save PDF", Color(0xFFEF4444)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            PageUtils.saveAsPdf(context, currentWebView, currentWebView.title ?: "Page")
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Star, "Bookmarks", Color(0xFFFFB000)) {
                            onOpenBookmarks()
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.History, "History", Color(0xFF607D8B)) {
                            onOpenHistory()
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Settings, "Settings", Color(0xFF4B5563)) {
                            onOpenSettings()
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(if (isForceDark) Icons.Default.LightMode else Icons.Default.DarkMode, if (isForceDark) "Force Light" else "Force Dark", Color(0xFF1E293B)) {
                            isForceDark = !isForceDark
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.VerticalAlignBottom, "Auto Scroll", Color(0xFF0EA5E9)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            currentWebView.evaluateJavascript("window.startOmniScroll()", null)
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.AutoMirrored.Filled.MenuBook, "Reader Mode", Color(0xFFEA580C)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            currentWebView.evaluateJavascript("document.documentElement.outerHTML") { source: String? ->
                                val cleanSource = if (source != null && source.startsWith("\"") && source.endsWith("\"")) {
                                    source.substring(1, source.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                } else {
                                    source ?: ""
                                }
                                readerContent = PageUtils.extractArticleContent(cleanSource)
                                isReaderMode = true
                            }
                            showTools = false
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

    if (showMediaGrabber) {
        MediaGrabberView(mediaItems = activeTab.detectedMedia, onDownload = { item ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            downloadManager.startDownload(item.src, item.title)
        }) { showMediaGrabber = false }
    }

    if (showBookmarklets) {
        val bookmarklets = userScripts.filter { it.type == "bookmarklet" && it.enabled }
        ModalBottomSheet(onDismissRequest = { showBookmarklets = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
                val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                Text("Bookmarklets", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                if (bookmarklets.isEmpty()) {
                    Text("No bookmarklets found. Add them in Settings > Script Manager.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(bookmarklets) { bookmarklet ->
                            ListItem(
                                headlineContent = { Text(bookmarklet.name) },
                                modifier = Modifier.clickable {
                                    currentWebView.evaluateJavascript("(function() { ${bookmarklet.script} })();", null)
                                    showBookmarklets = false
                                },
                                leadingContent = { Icon(Icons.Default.Javascript, contentDescription = null, tint = Color(0xFFFACC15)) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (isReaderMode) {
        ReaderModeView(
            title = activeTab.title ?: "Reader Mode",
            content = readerContent,
            onClose = { isReaderMode = false }
        )
    }

    if (showSiteSettings) {
        val host = Uri.parse(activeTab.url).host ?: "Local"
        val perSiteSettings by database.perSiteSettingsDao().getSettingsForHost(host).collectAsState(initial = null)
        SiteSettingsDialog(
            host = host,
            settings = perSiteSettings,
            onUpdate = { viewModel.updatePerSiteSettings(it); viewModel.getOrCreateWebView(activeTab.id, context).reload() },
            onViewPrivacyReport = { showPrivacyReport = true; showSiteSettings = false },
            onDismiss = { showSiteSettings = false }
        )
    }

    if (showPrivacyReport) {
        val blockedTrackers = synchronized(viewModel.blockedTrackersByTab) {
            viewModel.blockedTrackersByTab[activeTab.id]?.toList() ?: emptyList()
        }
        PrivacyReportDialog(
            blockedTrackers = blockedTrackers,
            onDismiss = { showPrivacyReport = false }
        )
    }

    if (showQuickActions) {
        QuickActionsSheet(
            onNewTab = { viewModel.createTab() },
            onSaveToReadingList = {
                scope.launch {
                    val path = PageUtils.saveAsMhtml(context, viewModel.getOrCreateWebView(activeTab.id, context), activeTab.title)
                    database.readingListDao().insertEntry(ReadingListEntry(title = activeTab.title, url = activeTab.url, filePath = path))
                }
            },
            onFindInPage = { isFindMode = true },
            onDesktopModeToggle = {
                isDesktopMode = !isDesktopMode
                viewModel.getOrCreateWebView(activeTab.id, context).apply {
                    this.settings.userAgentString = if (isDesktopMode) "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" else null
                    reload()
                }
            },
            onReaderMode = {
                viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("document.documentElement.outerHTML") { source ->
                    readerContent = PageUtils.extractArticleContent(source ?: "")
                    isReaderMode = true
                }
            },
            onDismiss = { showQuickActions = false }
        )
    }

    if (passwordToSave != null) {
        val (site, user, pass) = passwordToSave!!
        AlertDialog(
            onDismissRequest = { passwordToSave = null },
            title = { Text("Save Password?") },
            text = { Text("Would you like to save the password for $user on $site?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val (encrypted, iv) = CryptoUtils.encrypt(pass)
                        database.passwordDao().insertPassword(com.omniweb.app.data.PasswordEntry(site = site, username = user, encryptedPassword = encrypted, iv = iv))
                        Toast.makeText(context, "Password saved", Toast.LENGTH_SHORT).show()
                    }
                    passwordToSave = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { passwordToSave = null }) { Text("No thanks") }
            }
        )
    }

    if (showAddBookmarkletDialog != null) {
        val script = showAddBookmarkletDialog!!
        AlertDialog(
            onDismissRequest = { showAddBookmarkletDialog = null },
            title = { Text("Add Bookmarklet?") },
            text = { Text("This looks like a bookmarklet. Would you like to add it to your script manager?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.userScriptDao().insertScript(
                            com.omniweb.app.data.UserScript(
                                name = "Imported Bookmarklet",
                                script = script.substringAfter("javascript:"),
                                type = "bookmarklet",
                                enabled = true
                            )
                        )
                        Toast.makeText(context, "Added to bookmarklets", Toast.LENGTH_SHORT).show()
                    }
                    showAddBookmarkletDialog = null
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkletDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showContextMenu && contextMenuResult != null) {
        ContextMenuSheet(
            result = contextMenuResult!!,
            onOpenInNewTab = { viewModel.createTab(it) },
            onOpenInBackground = { url ->
                val currentTabId = activeTab.id
                viewModel.createTab(url)
                viewModel.selectTab(currentTabId)
            },
            onCopyAddress = { url ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", url))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onDownload = { url -> downloadManager.startDownload(url, "Image") },
            onAddBookmarklet = { script ->
                scope.launch {
                    database.userScriptDao().insertScript(com.omniweb.app.data.UserScript(name = "Saved Bookmarklet", script = script.substringAfter("javascript:"), type = "bookmarklet", enabled = true))
                    Toast.makeText(context, "Added to bookmarklets", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showContextMenu = false }
        )
    }
}
}
