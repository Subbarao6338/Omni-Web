package com.omniweb.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.HistoryEntry
import com.omniweb.app.data.MediaItem
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.data.ReadingListEntry
import com.omniweb.app.util.AdBlockManager
import com.omniweb.app.util.OmniDownloadManager
import com.omniweb.app.util.PageAnalyzer
import com.omniweb.app.util.AnalysisResult
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

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
    onOpenDownloads: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by database.bookmarkDao().getAllBookmarks().collectAsStateWithLifecycle(initialValue = emptyList())
    val isBookmarked = bookmarks.any { it.url == activeTab.url }
    val userScripts by database.userScriptDao().getAllScripts().collectAsStateWithLifecycle(initialValue = emptyList())
    val downloadManager = remember { OmniDownloadManager(context) }

    val tabs = viewModel.tabs
    val isSplitScreen by viewModel.isSplitScreen.collectAsStateWithLifecycle()
    val splitTabId by viewModel.splitTabId.collectAsStateWithLifecycle()
    val isZenMode by viewModel.isZenMode.collectAsStateWithLifecycle()

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

    LaunchedEffect(activeTab.url) {
        if (urlInput != activeTab.url) {
            urlInput = activeTab.url
        }
    }

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
    var isReaderMode by remember { mutableStateOf(false) }
    var readerContent by remember { mutableStateOf("") }
    var summaryContent by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }

    var isInspectMode by remember { mutableStateOf(false) }

    var showPrivacyReport by remember { mutableStateOf(false) }
    var showSiteSettings by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }

    var passwordToSave by remember { mutableStateOf<Triple<String, String, String>?>(null) } // site, user, pass
    var showVideoSpeed by remember { mutableStateOf(false) }

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

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrBlank()) {
                urlInput = spokenText
                val target = UrlUtils.resolveUrl(spokenText, settings.searchEngine)
                if (target == "about:home") onBackToHome() else viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
            }
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
        snackbarHost = {
            Column {
                if (isZenMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        modifier = Modifier.padding(16.dp).align(Alignment.End).clickable { viewModel.toggleZenMode() },
                        shape = CircleShape,
                        shadowElevation = 4.dp
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Zen Mode", modifier = Modifier.padding(8.dp), tint = Color.White)
                    }
                }
                if (isInspectMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inspect Mode Active: Tap an element", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                isInspectMode = false
                                viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("""
                                    (function() {
                                        if (window.omniInspector) window.omniInspector.stop();
                                    })();
                                """.trimIndent(), null)
                            }) { Text("Exit") }
                        }
                    }
                }
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        topBar = {
            if (!isZenMode && settings.toolbarLocation == "top") {
                BrowserAddressBar(
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 50) {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            } else if (dragAmount < -50) {
                                scope.launch {
                                    if (pagerState.currentPage < tabs.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                        }
                    }.statusBarsPadding(),
                    urlInput = urlInput,
                    onUrlChange = {
                        urlInput = it
                        viewModel.updateSuggestions(it)
                    },
                    onGo = {
                        val input = urlInput.trim()
                        if (input.isNotEmpty()) {
                            val target = UrlUtils.resolveUrl(input, settings.searchEngine)
                            if (target == "about:home") onBackToHome() else {
                                activeTab.url = target
                                viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                            }
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
                                database.bookmarkDao().insertBookmark(Bookmark(title = activeTab.title, url = urlInput))
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
                    onVoiceClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice search not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onScanClick = onOpenScanner,
                    suggestions = if (urlInput != activeTab.url) viewModel.searchSuggestions.value else emptyList(),
                    onSuggestionClick = { suggestion ->
                        val target = UrlUtils.resolveUrl(suggestion.url, settings.searchEngine)
                        if (target == "about:home") onBackToHome() else {
                            urlInput = target
                            activeTab.url = target
                            viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                        }
                        viewModel.updateSuggestions("")
                    },
                    blockedCount = synchronized(viewModel.blockedTrackersByTab) { viewModel.blockedTrackersByTab[activeTab.id]?.size ?: 0 },
                    tabCount = viewModel.tabs.size,
                    mediaCount = activeTab.detectedMedia.size,
                    onShowTabs = { showTabs = true },
                    onShowMenu = { showTools = true }
                )
            }
        },
        bottomBar = {
            if (!isZenMode && settings.toolbarLocation == "bottom") {
                BrowserAddressBar(
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 50) {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            } else if (dragAmount < -50) {
                                scope.launch {
                                    if (pagerState.currentPage < tabs.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                        }
                    }.navigationBarsPadding(),
                    urlInput = urlInput,
                    onUrlChange = {
                        urlInput = it
                        viewModel.updateSuggestions(it)
                    },
                    onGo = {
                        val input = urlInput.trim()
                        if (input.isNotEmpty()) {
                            val target = UrlUtils.resolveUrl(input, settings.searchEngine)
                            if (target == "about:home") onBackToHome() else {
                                activeTab.url = target
                                viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                            }
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
                                database.bookmarkDao().insertBookmark(Bookmark(title = activeTab.title, url = urlInput))
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
                    onVoiceClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice search not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onScanClick = onOpenScanner,
                    suggestions = if (urlInput != activeTab.url) viewModel.searchSuggestions.value else emptyList(),
                    onSuggestionClick = { suggestion ->
                        val target = UrlUtils.resolveUrl(suggestion.url, settings.searchEngine)
                        if (target == "about:home") onBackToHome() else {
                            urlInput = target
                            activeTab.url = target
                            viewModel.getOrCreateWebView(activeTab.id, context).loadUrl(target)
                        }
                        viewModel.updateSuggestions("")
                    },
                    blockedCount = synchronized(viewModel.blockedTrackersByTab) { viewModel.blockedTrackersByTab[activeTab.id]?.size ?: 0 },
                    tabCount = viewModel.tabs.size,
                    mediaCount = activeTab.detectedMedia.size,
                    onShowTabs = { showTabs = true },
                    onShowMenu = { showTools = true }
                )
            }
        }
    ) { padding ->
    if (isSplitScreen && splitTabId != null) {
        val splitTab = viewModel.tabs.find { it.id == splitTabId }
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                WebViewContainer(
                    tab = activeTab,
                    viewModel = viewModel,
                    settings = settings,
                    onLoginDetected = { site, user, pass -> passwordToSave = Triple(site, user, pass) },
                    onBookmarkletDetected = { showAddBookmarkletDialog = it },
                    onTextExtracted = { if (activeTab.id == viewModel.activeTabId.value) pageText = it },
                    onScrollChanged = { x, y -> viewModel.updateTabScroll(activeTab.id, x, y) },
                    onContextMenu = { contextMenuResult = it; showContextMenu = true },
                    onProgressChanged = { activeTab.progress = it },
                    onTitleReceived = { activeTab.title = it; viewModel.updateTabInDb(activeTab) },
                    onIconReceived = { activeTab.faviconBitmap = it },
                    onConsoleLog = { msg, level -> consoleLogs.add(ConsoleLog(msg, level)) }
                )
            }
            Box(modifier = Modifier.height(4.dp).fillMaxWidth().background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.weight(1f)) {
                if (splitTab != null) {
                    WebViewContainer(
                        tab = splitTab,
                        viewModel = viewModel,
                        settings = settings,
                        onLoginDetected = { site, user, pass -> passwordToSave = Triple(site, user, pass) },
                        onBookmarkletDetected = { showAddBookmarkletDialog = it },
                        onTextExtracted = { },
                        onScrollChanged = { x, y -> viewModel.updateTabScroll(splitTab.id, x, y) },
                        onContextMenu = { contextMenuResult = it; showContextMenu = true },
                        onProgressChanged = { splitTab.progress = it },
                        onTitleReceived = { splitTab.title = it; viewModel.updateTabInDb(splitTab) },
                        onIconReceived = { splitTab.faviconBitmap = it },
                        onConsoleLog = { msg, level -> consoleLogs.add(ConsoleLog(msg, level)) }
                    )
                }
            }
        }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            userScrollEnabled = false
        ) { pageIndex ->
            val tab = tabs[pageIndex]
            val currentWebView = viewModel.getOrCreateWebView(tab.id, context)
            DisposableEffect(isFindMode) {
                if (isFindMode && tab.id == activeTab.id) {
                    currentWebView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                        if (isDoneCounting) {
                            findMatchStatus = if (numberOfMatches > 0) "${activeMatchOrdinal + 1}/$numberOfMatches" else "0/0"
                        }
                    }
                } else {
                    currentWebView.setFindListener(null)
                }
                onDispose {
                    currentWebView.setFindListener(null)
                }
            }

            WebViewContainer(
                tab = tab,
                viewModel = viewModel,
                settings = settings,
                onLoginDetected = { site, user, pass -> passwordToSave = Triple(site, user, pass) },
                onBookmarkletDetected = { showAddBookmarkletDialog = it },
                onTextExtracted = { text ->
                    if (tab.id == activeTab.id) {
                        if (text.startsWith("INSPECT:")) {
                            scope.launch {
                                snackbarHostState.showSnackbar(text.removePrefix("INSPECT:"))
                            }
                        } else {
                            pageText = text
                        }
                    }
                },
                onScrollChanged = { x, y -> viewModel.updateTabScroll(tab.id, x, y) },
                onContextMenu = { contextMenuResult = it; showContextMenu = true },
                onProgressChanged = { tab.progress = it },
                onTitleReceived = { title ->
                    tab.title = title
                    viewModel.updateTabInDb(tab)
                    if (!tab.isIncognito) {
                        scope.launch {
                            database.historyDao().insertHistory(HistoryEntry(title = title, url = tab.url))
                        }
                    }
                },
                onIconReceived = { tab.faviconBitmap = it },
                onConsoleLog = { msg, level -> consoleLogs.add(ConsoleLog(msg, level)) }
            )
        }
    }

    if (showTabs) {
        val activeId by viewModel.activeTabId.collectAsStateWithLifecycle()
        TabSwitcherSheet(
            tabs = viewModel.tabs,
            recentlyClosedTabs = viewModel.recentlyClosedTabs,
            activeTabId = activeId,
            onTabSelect = { viewModel.selectTab(it) },
            onTabRestore = { tab ->
                viewModel.restoreTab(tab)
            },
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

    analysisResult?.let {
        PageAnalysisView(result = it, onBack = { analysisResult = null })
    }

    if (showTools) {
        ModalBottomSheet(onDismissRequest = { showTools = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())) {
                Text("Page Tools", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(24.dp))

                val toolsWebView = viewModel.getOrCreateWebView(activeTab.id, context)

                ToolCategory("Navigation") {
                    item { ToolButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", Color(0xFF3B82F6)) {
                        if (toolsWebView.canGoBack()) toolsWebView.goBack() else onBackToHome()
                        showTools = false
                    }}
                    item { ToolButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Forward", Color(0xFF3B82F6)) {
                        if (toolsWebView.canGoForward()) toolsWebView.goForward()
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Add, "New Tab", Color(0xFF10B981)) {
                        viewModel.createTab()
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Home, "Home", Color(0xFF6B7280)) {
                        onBackToHome()
                        showTools = false
                    }}
                    item {
                        val isBookmarkedInternal = bookmarks.any { it.url == activeTab.url }
                        ToolButton(if (isBookmarkedInternal) Icons.Default.Star else Icons.Default.StarBorder, if (isBookmarkedInternal) "Bookmarked" else "Bookmark", Color(0xFFFFB000)) {
                        scope.launch {
                            if (isBookmarkedInternal) {
                                bookmarks.find { it.url == activeTab.url }?.let { database.bookmarkDao().deleteBookmark(it) }
                            } else {
                                database.bookmarkDao().insertBookmark(Bookmark(title = activeTab.title, url = urlInput))
                            }
                        }
                        showTools = false
                    }}
                }

                Spacer(modifier = Modifier.height(24.dp))

                ToolCategory("Page Actions") {
                    item { ToolButton(if (isSplitScreen) Icons.Default.Fullscreen else Icons.Default.VerticalSplit, if (isSplitScreen) "Single Screen" else "Split Screen", Color(0xFFF59E0B)) {
                        viewModel.toggleSplitScreen()
                        showTools = false
                    }}
                    item { ToolButton(Icons.AutoMirrored.Filled.OpenInNew, "Open in App", Color(0xFF3B82F6)) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app can handle this link", Toast.LENGTH_SHORT).show()
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Share, "Share", Color(0xFF3B82F6)) {
                        toolsWebView.url?.let {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, it)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Link"))
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.AutoMirrored.Filled.Chat, "AI Chat", Color(0xFF8B5CF6)) {
                        showAiChat = true
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.ContentCopy, "Copy Link", Color(0xFF8B5CF6)) {
                        toolsWebView.url?.let {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", it))
                            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Search, "Find", Color(0xFF3B82F6)) {
                        isFindMode = true
                        showTools = false
                    }}
                    item { ToolButton(Icons.AutoMirrored.Filled.MenuBook, "Reader", Color(0xFFEA580C)) {
                        toolsWebView.evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source: String? ->
                            val cleanSource = if (source != null && source.startsWith("\"") && source.endsWith("\"")) {
                                source.substring(1, source.length - 1).replace("\\\"", "\"").replace("\\n", "\n").replace("\\t", "\t")
                            } else source ?: ""
                            readerContent = PageUtils.extractArticleContent(cleanSource)
                            isReaderMode = true
                        }
                        showTools = false
                    }}
                    item { ToolButton(if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone, if (isDesktopMode) "Mobile" else "Desktop", Color(0xFF6366F1)) {
                        isDesktopMode = !isDesktopMode
                        toolsWebView.settings.userAgentString = if (isDesktopMode) "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" else null
                        toolsWebView.reload()
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.AutoAwesome, "Summarize", Color(0xFF8B5CF6)) {
                        toolsWebView.evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
                            scope.launch {
                                summaryContent = PageUtils.generateSummary(source ?: "", settings.geminiApiKey)
                            }
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.QrCode, "QR Code", Color(0xFF10B981)) {
                        qrBitmap = PageUtils.generateQRCode(activeTab.url)
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Insights, "Insights", Color(0xFF10B981)) {
                        PageAnalyzer.analyze(toolsWebView) {
                            analysisResult = it
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.SlowMotionVideo, "Video Speed", Color(0xFFEA580C)) {
                        showVideoSpeed = true
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.SelfImprovement, "Zen Mode", Color(0xFF10B981)) {
                        viewModel.toggleZenMode()
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.VideoLibrary, "Media Grabber", Color(0xFFEC4899)) {
                        showMediaGrabber = true
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Download, "Downloads", Color(0xFF3B82F6)) {
                        onOpenDownloads()
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.AddHome, "Add Home", Color(0xFF10B981)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                            if (shortcutManager!!.isRequestPinShortcutSupported) {
                                val pinShortcutInfo = ShortcutInfo.Builder(context, urlInput)
                                    .setShortLabel(toolsWebView.title ?: "Web Page")
                                    .setIcon(if (activeTab.faviconBitmap != null) Icon.createWithBitmap(activeTab.faviconBitmap) else Icon.createWithResource(context, com.omniweb.app.R.mipmap.ic_launcher))
                                    .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(urlInput)))
                                    .build()
                                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                            }
                        }
                        showTools = false
                    }}
                }

                Spacer(modifier = Modifier.height(24.dp))
                ToolCategory("Save & Print") {
                    item { ToolButton(Icons.Default.CameraAlt, "Full Shot", Color(0xFF06B6D4)) {
                        PageUtils.takeFullPageScreenshot(context, toolsWebView, activeTab.title)
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.PictureAsPdf, "Save PDF", Color(0xFFEF4444)) {
                        PageUtils.saveAsPdf(context, toolsWebView, activeTab.title)
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Archive, "Save MHTML", Color(0xFF8B5CF6)) {
                        scope.launch {
                            val path = PageUtils.saveAsMhtml(context, toolsWebView, activeTab.title)
                            database.readingListDao().insertEntry(com.omniweb.app.data.ReadingListEntry(title = activeTab.title, url = activeTab.url, filePath = path))
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Description, "Save MD", Color(0xFF10B981)) {
                        toolsWebView.evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
                            val clean = if (source != null && source.startsWith("\"") && source.endsWith("\"")) source.substring(1, source.length - 1).replace("\\\"", "\"").replace("\\n", "\n") else source ?: ""
                            PageUtils.saveAsMarkdown(context, clean, activeTab.title)
                        }
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Print, "Print", Color(0xFF4B5563)) {
                        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        printManager.print("Omni Document", toolsWebView.createPrintDocumentAdapter("Document"), null)
                        showTools = false
                    }}
                }

                Spacer(modifier = Modifier.height(24.dp))
                ToolCategory("Developer Tools") {
                    item { ToolButton(Icons.Default.RecordVoiceOver, "Speak", Color(0xFF10B981)) {
                        viewModel.speak(pageText)
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.VoiceOverOff, "Stop Voice", Color(0xFFEF4444)) {
                        viewModel.stopSpeaking()
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Code, "Source", Color(0xFFEA580C)) {
                        toolsWebView.evaluateJavascript("document.documentElement.outerHTML") { source ->
                            pageSource = source ?: ""
                            showSource = true
                            showTools = false
                        }
                    }}
                    item { ToolButton(Icons.Default.Terminal, "Console", Color(0xFF10B981)) {
                        showConsole = true
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.Javascript, "Bookmarklets", Color(0xFFFACC15)) {
                        showBookmarklets = true
                        showTools = false
                    }}
                    item { ToolButton(Icons.Default.BugReport, "Inspect", Color(0xFFEF4444)) {
                        isInspectMode = true
                        showTools = false
                        toolsWebView.evaluateJavascript("""
                            (function() {
                                if (window.omniInspector) {
                                    window.omniInspector.start();
                                    return;
                                }
                                window.omniInspector = {
                                    style: null,
                                    lastEl: null,
                                    start: function() {
                                        if (!this.style) {
                                            this.style = document.createElement('style');
                                            this.style.innerHTML = '.omni-inspect-highlight { outline: 2px solid #ef4444 !important; outline-offset: -2px !important; }';
                                            document.head.appendChild(this.style);
                                        }
                                        document.addEventListener('click', this.handler, true);
                                    },
                                    stop: function() {
                                        document.removeEventListener('click', this.handler, true);
                                        if (this.lastEl) this.lastEl.classList.remove('omni-inspect-highlight');
                                    },
                                    handler: function(e) {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        if (window.omniInspector.lastEl) window.omniInspector.lastEl.classList.remove('omni-inspect-highlight');
                                        window.omniInspector.lastEl = e.target;
                                        e.target.classList.add('omni-inspect-highlight');
                                        const info = {
                                            tag: e.target.tagName.toLowerCase(),
                                            id: e.target.id,
                                            class: e.target.className,
                                            text: e.target.innerText.substring(0, 50)
                                        };
                                        Android.postText("INSPECT:" + JSON.stringify(info));
                                    }
                                };
                                window.omniInspector.start();
                            })();
                        """.trimIndent(), null)
                    }}
                }

                Spacer(modifier = Modifier.height(24.dp))
                ToolCategory("Browser") {
                    item { ToolButton(Icons.Default.Settings, "Settings", Color(0xFF4B5563)) { onOpenSettings(); showTools = false }}
                    item { ToolButton(Icons.Default.History, "History", Color(0xFF607D8B)) { onOpenHistory(); showTools = false }}
                    item { ToolButton(Icons.Default.Star, "Bookmarks", Color(0xFFFFB000)) { onOpenBookmarks(); showTools = false }}
                    item { ToolButton(Icons.Default.Download, "Downloads", Color(0xFF3B82F6)) { onOpenDownloads(); showTools = false }}
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
        MediaGrabberView(mediaItems = activeTab.detectedMedia, onDownload = { items ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            items.forEach { item ->
                downloadManager.startDownload(item.src, item.title)
            }
            Toast.makeText(context, "Started ${items.size} downloads", Toast.LENGTH_SHORT).show()
        }) { showMediaGrabber = false }
    }

    if (showBookmarklets) {
        val bookmarklets = userScripts.filter { it.type == "bookmarklet" && it.enabled }
        ModalBottomSheet(onDismissRequest = { showBookmarklets = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
                val bookmarkletsWebView = viewModel.getOrCreateWebView(activeTab.id, context)
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
                                    bookmarkletsWebView.evaluateJavascript("(function() { ${bookmarklet.script} })();", null)
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
            title = activeTab.title,
            content = readerContent,
            settings = settings,
            onUpdateSettings = { newSettings ->
                scope.launch {
                    database.settingsDao().updateSettings(newSettings)
                }
            },
            onExportMarkdown = {
                viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
                    val clean = if (source != null && source.startsWith("\"") && source.endsWith("\"")) source.substring(1, source.length - 1).replace("\\\"", "\"").replace("\\n", "\n") else source ?: ""
                    PageUtils.saveAsMarkdown(context, clean, activeTab.title)
                }
            },
            onClose = { isReaderMode = false }
        )
    }

    if (showSiteSettings) {
        val host = Uri.parse(activeTab.url).host ?: "Local"
        val perSiteSettings by database.perSiteSettingsDao().getSettingsForHost(host).collectAsStateWithLifecycle(initialValue = null)
        SiteSettingsDialog(
            host = host,
            settings = perSiteSettings,
            onUpdate = { viewModel.updatePerSiteSettings(it); viewModel.getOrCreateWebView(activeTab.id, context).reload() },
            onViewPrivacyReport = { showPrivacyReport = true; showSiteSettings = false },
            onClearData = {
                viewModel.clearSiteData(host)
                Toast.makeText(context, "Data cleared for $host", Toast.LENGTH_SHORT).show()
                viewModel.getOrCreateWebView(activeTab.id, context).reload()
                showSiteSettings = false
            },
            onDismiss = { showSiteSettings = false }
        )
    }

    if (showPrivacyReport) {
        val blockedTrackers = synchronized(viewModel.blockedTrackersByTab) {
            viewModel.blockedTrackersByTab[activeTab.id]?.toList() ?: emptyList()
        }
        PrivacyReportView(
            blockedTrackers = blockedTrackers,
            onBack = { showPrivacyReport = false }
        )
    }

    if (showAiChat) {
        var chatInput by remember { mutableStateOf("") }
        val chatMessages = remember { mutableStateListOf<Pair<String, String>>() }
        ModalBottomSheet(onDismissRequest = { showAiChat = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
                Text("Chat with Page", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                    items(chatMessages) { msg ->
                        Text("${msg.first}: ${msg.second}", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = chatInput, onValueChange = { chatInput = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask anything...") })
                    IconButton(onClick = {
                        val msg = chatInput
                        chatMessages.add("You" to msg)
                        chatInput = ""
                        scope.launch {
                            val response = viewModel.chatWithPage(activeTab.url, pageText, msg, settings.geminiApiKey)
                            chatMessages.add("AI" to response)
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, null) }
                }
            }
        }
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
                viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
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

    if (summaryContent != null) {
        AlertDialog(
            onDismissRequest = { summaryContent = null },
            title = { Text("AI Page Summary") },
            text = { Text(summaryContent!!) },
            confirmButton = { TextButton(onClick = { summaryContent = null }) { Text("Close") } }
        )
    }

    if (showVideoSpeed) {
        VideoSpeedController(
            currentSpeed = activeTab.playbackSpeed,
            onSpeedChange = { speed ->
                activeTab.playbackSpeed = speed
                viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("""
                    (function() {
                        document.querySelectorAll('video').forEach(v => v.playbackRate = $speed);
                    })();
                """.trimIndent(), null)
            },
            onDismiss = { showVideoSpeed = false }
        )
    }

    if (qrBitmap != null) {
        AlertDialog(
            onDismissRequest = { qrBitmap = null },
            title = { Text("Share via QR") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { qrBitmap = null }) { Text("Close") } }
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
            onHighlight = {
                viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("(function() { return window.getSelection().toString(); })();") { selection ->
                    val text = selection?.trim()?.removeSurrounding("\"") ?: ""
                    if (text.isNotEmpty()) {
                        viewModel.saveAnnotation(activeTab.url, text)
                        // Trigger JS to highlight immediately using safe range-based method
                        viewModel.getOrCreateWebView(activeTab.id, context).evaluateJavascript("""
                            (function() {
                                const text = `${text.replace("`", "\\`")}`;
                                function highlight(node) {
                                    if (node.nodeType === 3) {
                                        const index = node.data.indexOf(text);
                                        if (index >= 0) {
                                            const range = document.createRange();
                                            range.setStart(node, index);
                                            range.setEnd(node, index + text.length);
                                            const mark = document.createElement('mark');
                                            mark.style.backgroundColor = 'yellow';
                                            range.surroundContents(mark);
                                        }
                                    } else if (node.nodeType === 1 && node.childNodes && !/(script|style|mark)/i.test(node.tagName)) {
                                        for (let i = 0; i < node.childNodes.length; i++) {
                                            highlight(node.childNodes[i]);
                                        }
                                    }
                                }
                                highlight(document.body);
                            })();
                        """.trimIndent(), null)
                    }
                }
            },
            onDismiss = { showContextMenu = false }
        )
    }
}
}
