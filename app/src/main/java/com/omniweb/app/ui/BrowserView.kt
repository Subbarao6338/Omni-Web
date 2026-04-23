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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.HistoryEntry
import com.omniweb.app.data.MediaItem
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.util.OmniDownloadManager
import com.omniweb.app.util.PageUtils
import com.omniweb.app.util.WebAppInterface
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.clip

private val AD_DOMAINS = setOf(
    "doubleclick.net", "googleadservices.com", "adnxs.com",
    "googlesyndication.com", "quantserve.com", "scorecardresearch.com",
    "zedo.com", "amazon-adsystem.com", "adservice.google.com",
    "google-analytics.com", "analytics.google.com", "ads.linkedin.com",
    "static.ads-twitter.com", "ads-twitter.com", "fbcdn.net", "facebook.com",
    "ad.doubleclick.net", "pagead2.googlesyndication.com", "pubads.g.doubleclick.net"
)

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

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < tabs.size) {
            viewModel.selectTab(tabs[pagerState.currentPage].id)
        }
    }

    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var urlInput by remember { mutableStateOf(activeTab.url) }

    var showTools by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }

    var showSource by remember { mutableStateOf(false) }
    var showConsole by remember { mutableStateOf(false) }
    var showMediaGrabber by remember { mutableStateOf(false) }

    var detectedMedia by remember { mutableStateOf(listOf<MediaItem>()) }
    var pageText by remember { mutableStateOf("") }
    var pageSource by remember { mutableStateOf("") }
    val consoleLogs = remember { mutableStateListOf<ConsoleLog>() }

    var isFindMode by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var isDesktopMode by remember { mutableStateOf(false) }

    var pageFavicon by remember { mutableStateOf<Bitmap?>(null) }

    val scope = rememberCoroutineScope()

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
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBackToHome()
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.statusBarsPadding()) {
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
                            IconButton(onClick = onBackToHome) { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Box(modifier = Modifier.weight(1f)) {
                                TextField(
                                    value = urlInput,
                                    onValueChange = {
                                        urlInput = it
                                        viewModel.updateSuggestions(it)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
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
                                        if (!target.startsWith("http") && !target.startsWith("about:")) {
                                            if (!target.contains(".") || target.contains(" ")) {
                                                target = "${settings.searchEngine}${android.net.Uri.encode(target)}"
                                            } else {
                                                target = "https://$target"
                                            }
                                        }
                                        webView?.loadUrl(target)
                                        viewModel.updateSuggestions("")
                                    }),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                                )

                                val suggestions by viewModel.searchSuggestions
                                if (suggestions.isNotEmpty() && urlInput != activeTab.url) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Column {
                                            suggestions.forEach { suggestion ->
                                                ListItem(
                                                    headlineContent = { Text(suggestion.title, maxLines = 1) },
                                                    supportingContent = { Text(suggestion.url, maxLines = 1, fontSize = 12.sp) },
                                                    modifier = Modifier.clickable {
                                                        urlInput = suggestion.url
                                                        webView?.loadUrl(suggestion.url)
                                                        viewModel.updateSuggestions("")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    if (isBookmarked) {
                                        bookmarks.find { it.url == activeTab.url }?.let {
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
                    NavButton(Icons.Default.Layers, "Tabs", badge = viewModel.tabs.size) { showTabs = true }
                    NavButton(Icons.Default.VideoLibrary, "Media", badge = detectedMedia.size) { showMediaGrabber = true }
                    NavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back") {
                         if (webView?.canGoBack() == true) webView?.goBack() else onBackToHome()
                    }
                    NavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Forward") { webView?.goForward() }
                    NavButton(Icons.Default.Download, "Files") { onOpenDownloads() }
                    NavButton(Icons.Default.MoreVert, "Menu") { showTools = true }
                }
            }
        }
    ) { padding ->
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.padding(padding).fillMaxSize(),
        userScrollEnabled = false
    ) { pageIndex ->
        val tab = tabs[pageIndex]
            AndroidView(
                factory = { context ->
                viewModel.getOrCreateWebView(tab.id, context).apply {
                        this.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                            allowContentAccess = true
                            allowFileAccess = true
                        }

                    if (tab.isIncognito) {
                            CookieManager.getInstance().setAcceptCookie(false)
                            this.settings.databaseEnabled = false
                            this.settings.domStorageEnabled = false
                            this.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        } else {
                            CookieManager.getInstance().setAcceptCookie(true)
                        }

                        addJavascriptInterface(WebAppInterface(
                            onMediaDetected = { detectedMedia = it },
                            onTextExtracted = { pageText = it }
                        ), "Android")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (tab.id == activeTab.id) {
                                progress = newProgress / 100f
                                if (newProgress == 100) isLoading = false
                            }
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (title != null && !title.startsWith("http")) {
                                tab.title = title
                                    viewModel.updateTabInDb(tab)
                                }
                            }

                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                super.onReceivedIcon(view, icon)
                            if (tab.id == activeTab.id) pageFavicon = icon
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
                            if (tab.id == activeTab.id) {
                                isLoading = true
                                url?.let { urlInput = it }
                            }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                            if (tab.id == activeTab.id) isLoading = false
                                url?.let {
                                tab.url = it
                                    val title = view?.title
                                    if (title != null && title.isNotEmpty()) {
                                     tab.title = title
                                    }
                                    viewModel.updateTabInDb(tab)

                                if (!tab.isIncognito) {
                                        scope.launch {
                                            database.historyDao().insertHistory(HistoryEntry(title = view?.title ?: it, url = it))
                                        }
                                    }

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

                                view?.evaluateJavascript("Android.postText(document.body.innerText)", null)

                                view?.evaluateJavascript("""
                                    (function() {
                                        function sniff() {
                                            const media = [];
                                            const seen = new Set();

                                            // Generic media elements and common extensions
                                            const selectors = 'video, audio, source, img, a[href*=".mp4"], a[href*=".m3u8"], a[href*=".mp3"], a[href*=".m4a"], a[href*=".wav"], a[href*=".jpg"], a[href*=".png"], a[href*=".webp"]';
                                            document.querySelectorAll(selectors).forEach(el => {
                                                const src = el.src || el.getAttribute('src') || el.currentSrc || el.href;
                                                if (src && src.startsWith('http') && !seen.has(src)) {
                                                    const ext = src.split('.').pop().split('?')[0].toLowerCase();
                                                    const isVideo = ['mp4', 'm3u8', 'webm', 'mov'].includes(ext) || el.tagName.toLowerCase() === 'video';
                                                    const isAudio = ['mp3', 'm4a', 'wav', 'ogg'].includes(ext) || el.tagName.toLowerCase() === 'audio';
                                                    const isImage = ['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(ext) || el.tagName.toLowerCase() === 'img';

                                                    if (isVideo || isAudio || isImage) {
                                                        seen.add(src);
                                                        media.push({
                                                            id: Math.random().toString(36).substr(2, 9),
                                                            src: src,
                                                            type: isVideo ? 'video' : (isAudio ? 'audio' : 'image'),
                                                            title: document.title || 'Media File'
                                                        });
                                                    }
                                                }
                                            });

                                            // Special handling for social platforms
                                            const host = location.host;
                                            if (host.includes('instagram.com') || host.includes('x.com') || host.includes('facebook.com') || host.includes('tiktok.com') || host.includes('threads.net')) {
                                                // yt-dlp will handle the page URL better than individual sniffed parts
                                                if (!seen.has(location.href)) {
                                                    seen.add(location.href);
                                                    media.push({
                                                        id: 'page-' + Date.now(),
                                                        src: location.href,
                                                        type: 'video',
                                                        title: 'Social Video: ' + (document.title || 'Post')
                                                    });
                                                }
                                            }

                                            if (host.includes('youtube.com')) {
                                                const videoId = new URLSearchParams(window.location.search).get('v');
                                                if (videoId) {
                                                    const ytUrl = 'https://www.youtube.com/watch?v=' + videoId;
                                                    if (!seen.has(ytUrl)) {
                                                         seen.add(ytUrl);
                                                         media.push({
                                                            id: 'yt-' + videoId,
                                                            src: ytUrl,
                                                            type: 'video',
                                                            title: document.title
                                                         });
                                                    }
                                                }
                                            }

                                            if (media.length > 0) {
                                                Android.postMedia(JSON.stringify(media));
                                            }
                                        }
                                        if (!window.omniSnifferStarted) {
                                            window.omniSnifferStarted = true;
                                            const observer = new MutationObserver(sniff);
                                            observer.observe(document.body, { childList: true, subtree: true });
                                            setInterval(sniff, 5000);
                                            sniff();
                                            window.startOmniScroll = function() {
                                                let distance = 100;
                                                let timer = setInterval(() => {
                                                    window.scrollBy(0, distance);
                                                    if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight) {
                                                        clearInterval(timer);
                                                    }
                                                }, 500);
                                            }
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

                    if (url == null || url == "about:blank") {
                        loadUrl(tab.url)
                    }
                    }
                },
                update = { view ->
                if (tab.id == activeTab.id) {
                    webView = view
                }
                if (view.url != tab.url && !tab.url.startsWith("about:")) {
                    view.loadUrl(tab.url)
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
                    Row {
                         IconButton(onClick = {
                            viewModel.createTab(isIncognito = true)
                            showTabs = false
                        }) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "New Incognito Tab")
                        }
                        IconButton(onClick = {
                            viewModel.createTab()
                            showTabs = false
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.tabs) { tab ->
                        val isSelected = tab.id == viewModel.activeTabId.value
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable {
                                    viewModel.selectTab(tab.id)
                                    showTabs = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Language,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(
                                        onClick = { viewModel.closeTab(tab.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(tab.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
                        ToolButton(Icons.Default.Translate, "Translate", Color(0xFF3B82F6)) {
                            webView?.let {
                                val targetUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(it.url)}"
                                it.loadUrl(targetUrl)
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Archive, "Save MHTML", Color(0xFF8B5CF6)) {
                            webView?.let { PageUtils.saveAsMhtml(context, it, it.title ?: "Page") }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Description, "Save MD", Color(0xFF10B981)) {
                            webView?.evaluateJavascript("document.documentElement.outerHTML") { source ->
                                val cleanSource = if (source != null && source.startsWith("\"") && source.endsWith("\"")) {
                                    source.substring(1, source.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                } else {
                                    source ?: ""
                                }
                                PageUtils.saveAsMarkdown(context, cleanSource, webView?.title ?: "Page")
                            }
                            showTools = false
                        }
                    }
                    item {
                        ToolButton(Icons.Default.Code, "View Source", Color(0xFFEA580C)) {
                            webView?.evaluateJavascript("document.documentElement.outerHTML") { source ->
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
                                        .setIcon(if (pageFavicon != null) Icon.createWithBitmap(pageFavicon) else Icon.createWithResource(context, com.omniweb.app.R.mipmap.ic_launcher))
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
                        ToolButton(Icons.Default.VerticalAlignBottom, "Auto Scroll", Color(0xFF0EA5E9)) {
                            webView?.evaluateJavascript("window.startOmniScroll()", null)
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
        MediaGrabberView(mediaItems = detectedMedia, onDownload = { item ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            downloadManager.startDownload(item.src, item.title)
        }) { showMediaGrabber = false }
    }

}
