package com.omniweb.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.HistoryEntry
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.util.OmniDownloadManager
import com.omniweb.app.util.PageUtils
import com.omniweb.app.util.UrlUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var passwordToSave by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showAddBookmarkletDialog by remember { mutableStateOf<String?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuResult by remember { mutableStateOf<WebView.HitTestResult?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(activeTab.id) { urlInput = activeTab.url }

    BackHandler {
        val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
        if (currentWebView.canGoBack()) currentWebView.goBack() else onBackToHome()
    }

    Scaffold(
        topBar = {
            Column {
                if (activeTab.isLoading && !isFindMode) {
                    LinearProgressIndicator(
                        progress = { activeTab.progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                BrowserAddressBar(
                    urlInput = urlInput,
                    onUrlChange = {
                        urlInput = it
                        viewModel.updateSuggestions(it)
                    },
                    onGo = {
                        val target = UrlUtils.resolveUrl(urlInput, settings.searchEngine)
                        if (target == "about:home") onBackToHome() else viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                        viewModel.updateSuggestions("")
                    },
                    onRefresh = { viewModel.getOrCreateWebView(activeTab.id, context).reload() },
                    onStop = { viewModel.getOrCreateWebView(activeTab.id, context).stopLoading() },
                    isLoading = activeTab.isLoading,
                    pageFavicon = activeTab.faviconBitmap,
                    onPrivacyClick = { showPrivacyReport = true },
                    onBookmarkClick = {
                        scope.launch {
                            if (isBookmarked) {
                                bookmarks.find { it.url == activeTab.url }?.let { database.bookmarkDao().deleteBookmark(it) }
                            } else {
                                database.bookmarkDao().insertBookmark(Bookmark(title = activeTab.title, url = activeTab.url))
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
                        viewModel.getOrCreateWebView(activeTab.id, context).clearMatches()
                    },
                    onHomeClick = onBackToHome,
                    suggestions = if (urlInput != activeTab.url) viewModel.searchSuggestions.value else emptyList(),
                    onSuggestionClick = {
                        val target = UrlUtils.resolveUrl(it.url, settings.searchEngine)
                        if (target == "about:home") onBackToHome() else viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                        viewModel.updateSuggestions("")
                    },
                    blockedCount = viewModel.blockedTrackersByTab[activeTab.id]?.size ?: 0
                )
            }
        },
        bottomBar = {
            BrowserBottomBar(
                tabCount = viewModel.tabs.size,
                mediaCount = activeTab.detectedMedia.size,
                onShowTabs = { showTabs = true },
                onNewTab = { viewModel.createTab() },
                onShowMedia = { showMediaGrabber = true },
                onBack = {
                    val wv = viewModel.getOrCreateWebView(activeTab.id, context)
                    if (wv.canGoBack()) wv.goBack() else onBackToHome()
                },
                onForward = {
                    val wv = viewModel.getOrCreateWebView(activeTab.id, context)
                    if (wv.canGoForward()) wv.goForward()
                },
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
            val currentWebView = remember(tab.id) { viewModel.getOrCreateWebView(tab.id, context) }

            DisposableEffect(tab.id, lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> currentWebView.onResume()
                        Lifecycle.Event.ON_PAUSE -> currentWebView.onPause()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val pullToRefreshState = rememberPullToRefreshState()
            if (pullToRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    currentWebView.reload()
                    delay(500)
                    while (tab.isLoading) delay(100)
                    pullToRefreshState.endRefresh()
                }
            }

            Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
                WebViewContainer(
                    tab = tab,
                    settings = settings,
                    webView = currentWebView,
                    isForceDark = isForceDark,
                    onUrlChanged = { url ->
                        tab.url = url
                        if (tab.id == activeTab.id) urlInput = url
                        if (!tab.isIncognito && url.startsWith("http")) {
                            scope.launch { database.historyDao().insertHistory(HistoryEntry(title = tab.title, url = url)) }
                        }
                    },
                    onTitleChanged = { title ->
                        tab.title = title
                        viewModel.updateTabInDb(tab)
                    },
                    onProgressChanged = { progress ->
                        tab.progress = progress
                        tab.isLoading = progress < 1f
                    },
                    onFaviconChanged = { tab.faviconBitmap = it },
                    onMediaDetected = {
                        tab.detectedMedia.clear()
                        tab.detectedMedia.addAll(it)
                    },
                    onLoginFormDetected = { site, user, pass ->
                        passwordToSave = Triple(site, user, pass)
                    },
                    onConsoleMessage = { msg, level ->
                        consoleLogs.add(ConsoleLog(msg, level))
                    },
                    onScrollChanged = { x, y ->
                        viewModel.updateTabScroll(tab.id, x, y)
                    },
                    onLongClick = { result ->
                        contextMenuResult = result
                        showContextMenu = true
                    },
                    onBookmarkletDetected = { showAddBookmarkletDialog = it },
                    onTextExtracted = { /* Optional usage */ },
                    onBlockedTracker = { tracker ->
                        synchronized(viewModel.blockedTrackersByTab) {
                            viewModel.blockedTrackersByTab.getOrPut(tab.id) { mutableSetOf() }.add(tracker)
                        }
                    }
                )
                PullToRefreshContainer(state = pullToRefreshState, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }

    // Overlays
    if (showTabs) {
        TabSwitcherSheet(
            tabs = viewModel.tabs,
            activeTabId = viewModel.activeTabId.value,
            onTabSelect = { viewModel.selectTab(it) },
            onTabClose = { viewModel.closeTab(it) },
            onCloseAll = { viewModel.tabs.toList().forEach { viewModel.closeTab(it.id) } },
            onNewTab = { viewModel.createTab(isIncognito = it) },
            onDismiss = { showTabs = false }
        )
    }

    if (showTools) {
        BrowserToolsSheet(
            onAction = { action ->
                val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                when(action) {
                    "new_tab" -> viewModel.createTab()
                    "new_incognito" -> viewModel.createTab(isIncognito = true)
                    "share" -> currentWebView.url?.let {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, it)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Link"))
                    }
                    "copy_url" -> currentWebView.url?.let {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", it))
                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                    }
                    "find" -> isFindMode = true
                    "desktop" -> {
                        isDesktopMode = !isDesktopMode
                        currentWebView.settings.userAgentString = if (isDesktopMode) "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" else null
                        currentWebView.reload()
                    }
                    "force_dark" -> isForceDark = !isForceDark
                    "reader" -> currentWebView.evaluateJavascript("document.documentElement.outerHTML") { source ->
                        readerContent = PageUtils.extractArticleContent(source ?: "")
                        isReaderMode = true
                    }
                    "view_source" -> currentWebView.evaluateJavascript("document.documentElement.outerHTML") { source ->
                        pageSource = source ?: ""
                        showSource = true
                    }
                    "console" -> showConsole = true
                    "bookmarks" -> onOpenBookmarks()
                    "history" -> onOpenHistory()
                    "settings" -> onOpenSettings()
                }
                showTools = false
            },
            isDesktop = isDesktopMode,
            isDark = isForceDark,
            onDismiss = { showTools = false }
        )
    }

    if (showPrivacyReport) {
        PrivacyReportDialog(
            blockedTrackers = viewModel.blockedTrackersByTab[activeTab.id]?.toList() ?: emptyList(),
            onDismiss = { showPrivacyReport = false }
        )
    }

    if (passwordToSave != null) {
        val (site, user, pass) = passwordToSave!!
        PasswordSaveDialog(
            site = site, user = user, pass = pass,
            onConfirm = {
                scope.launch {
                    val (encryptedPass, iv) = com.omniweb.app.util.CryptoUtils.encrypt(pass)
                    database.passwordDao().insertPassword(com.omniweb.app.data.PasswordEntry(site = site, username = user, password = encryptedPass, iv = iv))
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
                passwordToSave = null
            },
            onDismiss = { passwordToSave = null }
        )
    }

    if (showAddBookmarkletDialog != null) {
        val script = showAddBookmarkletDialog!!
        AddBookmarkletDialog(
            script = script,
            onConfirm = {
                scope.launch {
                    database.userScriptDao().insertScript(com.omniweb.app.data.UserScript(name = "Imported", script = script.substringAfter("javascript:"), type = "bookmarklet"))
                    Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show()
                }
                showAddBookmarkletDialog = null
            },
            onDismiss = { showAddBookmarkletDialog = null }
        )
    }

    if (showContextMenu && contextMenuResult != null) {
        ContextMenuSheet(
            result = contextMenuResult!!,
            onNewTab = { viewModel.createTab(it) },
            onDownload = { url, name -> downloadManager.startDownload(url, name) },
            onAddBookmarklet = { extra ->
                 scope.launch {
                    database.userScriptDao().insertScript(com.omniweb.app.data.UserScript(name = "Saved", script = extra.substringAfter("javascript:"), type = "bookmarklet"))
                    Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showContextMenu = false }
        )
    }

    if (isReaderMode) {
        ReaderModeView(title = activeTab.title, content = readerContent, onClose = { isReaderMode = false })
    }

    if (showSource) ViewSourceView(source = pageSource) { showSource = false }
    if (showConsole) ConsoleView(logs = consoleLogs, onClear = { consoleLogs.clear() }) { showConsole = false }
    if (showMediaGrabber) {
        MediaGrabberView(mediaItems = activeTab.detectedMedia, onDownload = { item ->
            downloadManager.startDownload(item.src, item.title)
        }) { showMediaGrabber = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserToolsSheet(
    onAction: (String) -> Unit,
    isDesktop: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
            Text("Page Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { ToolButton(Icons.Default.Add, "New Tab", Color(0xFF10B981)) { onAction("new_tab") } }
                item { ToolButton(Icons.Default.VisibilityOff, "Incognito", Color(0xFF6366F1)) { onAction("new_incognito") } }
                item { ToolButton(Icons.Default.Share, "Share", Color(0xFF3B82F6)) { onAction("share") } }
                item { ToolButton(Icons.Default.ContentCopy, "Copy", Color(0xFF8B5CF6)) { onAction("copy_url") } }
                item { ToolButton(Icons.Default.Search, "Find", Color(0xFF3B82F6)) { onAction("find") } }
                item { ToolButton(if (isDesktop) Icons.Default.Computer else Icons.Default.Smartphone, "Desktop", Color(0xFF6366F1)) { onAction("desktop") } }
                item { ToolButton(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, "Dark", Color(0xFF1E293B)) { onAction("force_dark") } }
                item { ToolButton(Icons.AutoMirrored.Filled.MenuBook, "Reader", Color(0xFFEA580C)) { onAction("reader") } }
                item { ToolButton(Icons.Default.Code, "Source", Color(0xFFEA580C)) { onAction("view_source") } }
                item { ToolButton(Icons.Default.Terminal, "Console", Color(0xFF10B981)) { onAction("console") } }
                item { ToolButton(Icons.Default.Star, "Bookmarks", Color(0xFFFFB000)) { onAction("bookmarks") } }
                item { ToolButton(Icons.Default.History, "History", Color(0xFF607D8B)) { onAction("history") } }
                item { ToolButton(Icons.Default.Settings, "Settings", Color(0xFF4B5563)) { onAction("settings") } }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
