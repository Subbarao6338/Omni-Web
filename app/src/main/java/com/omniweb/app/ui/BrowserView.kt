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
import com.omniweb.app.util.OmniDownloadManager
import com.omniweb.app.util.PageUtils
import com.omniweb.app.util.UrlUtils
import com.omniweb.app.util.WebAppInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.compose.ui.draw.clip

private val ADS_DOMAINS = setOf(
    "doubleclick.net", "googleadservices.com", "adnxs.com", "googlesyndication.com",
    "zedo.com", "amazon-adsystem.com", "adservice.google.com", "ad.doubleclick.net",
    "pagead2.googlesyndication.com", "pubads.g.doubleclick.net", "ads.google.com",
    "moatads.com", "openx.net", "adroll.com", "outbrain.com", "taboola.com",
    "advertising.com", "adtech.de", "adtechus.com", "yieldmanager.com", "pubmatic.com",
    "rubiconproject.com", "smartadserver.com", "criteo.com", "casalemedia.com",
    "atdmt.com", "ad-delivery.net", "adnxs-simple.com", "adform.net", "adgrx.com",
    "adhigh.net", "adinall.com", "adition.com", "admanmedia.com", "admicro.vn",
    "admixer.net", "adotmob.com", "adperium.com", "adriver.ru", "adrtx.com",
    "ads-pixie.com", "ads-union.com", "ads-zero.com", "adsafeprotected.com",
    "adsrvr.org", "adswizz.com", "adsymptotic.com", "bidswitch.net", "bluekai.com",
    "gumgum.com", "indexww.com", "lijit.com", "media.net", "mopub.com", "popads.net",
    "revcontent.com", "rubiconproject.com", "sharethrough.com", "sovrn.com",
    "adcolony.com", "applovin.com", "chartboost.com", "fyber.com", "ironsrc.com",
    "unityads.unity3d.com", "vungle.com", "flurry.com", "inmobi.com", "tapjoy.com"
)

private val ANALYTICS_DOMAINS = setOf(
    "google-analytics.com", "analytics.google.com", "googletagmanager.com",
    "googletagservices.com", "hotjar.com", "mouseflow.com", "crazyegg.com",
    "optimizely.com", "mixpanel.com", "segment.com", "clarity.ms", "quantserve.com",
    "scorecardresearch.com", "chartbeat.com", "clicky.com", "newrelic.com",
    "amplitude.com", "statcounter.com", "inspectlet.com", "fullstory.com",
    "bugsnag.com", "sentry.io", "crashlytics.com", "app-measurement.com"
)

private val SOCIAL_DOMAINS = setOf(
    "fbcdn.net", "facebook.com", "ads.linkedin.com", "static.ads-twitter.com",
    "ads-twitter.com", "analytics.twitter.com", "analytics.facebook.com",
    "ads-api.twitter.com", "pixel.facebook.com", "connect.facebook.net",
    "snapads.com", "pinterest.com", "tiktok.com", "twimg.com", "t.co",
    "instagram.com", "lnkd.in", "redditstatic.com", "redditmedia.com"
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
    var isDesktopMode by remember { mutableStateOf(false) }
    var isForceDark by remember { mutableStateOf(false) }
    var isReaderMode by remember { mutableStateOf(false) }
    var readerContent by remember { mutableStateOf("") }

    var showPrivacyReport by remember { mutableStateOf(false) }


    var showAddBookmarkletDialog by remember { mutableStateOf<String?>(null) }

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuResult by remember { mutableStateOf<WebView.HitTestResult?>(null) }

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
        val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
        if (currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else {
            onBackToHome()
        }
    }

    Scaffold(
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
                    onPrivacyClick = { showPrivacyReport = true },
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
                    onCloseFind = {
                        isFindMode = false
                        findQuery = ""
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
                    searchEngine = settings.searchEngine
                )
            }
        },
        bottomBar = {
            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
            BrowserBottomBar(
                tabCount = viewModel.tabs.size,
                mediaCount = activeTab.detectedMedia.size,
                onShowTabs = { showTabs = true },
                onNewTab = { viewModel.createTab() },
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
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

            AndroidView(
                factory = { _ ->
                currentWebView.apply {
                        this.settings.apply {
                            javaScriptEnabled = settings.javaScriptEnabled
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
                            setRenderPriority(WebSettings.RenderPriority.HIGH)
                            enableSmoothTransition()
                            userAgentString = settings.customUserAgent ?: userAgentString
                        }

                    if (tab.isIncognito) {
                            CookieManager.getInstance().setAcceptCookie(false)
                            this.settings.databaseEnabled = false
                            this.settings.domStorageEnabled = false
                            this.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        } else {
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !settings.blockThirdPartyCookies)
                        }

                        addJavascriptInterface(WebAppInterface(
                            onMediaDetected = {
                                tab.detectedMedia.clear()
                                tab.detectedMedia.addAll(it)
                            },
                            onTextExtracted = { if (tab.id == activeTab.id) pageText = it }
                        ), "Android")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                tab.progress = newProgress / 100f
                                if (newProgress == 100) tab.isLoading = false
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
                                tab.faviconBitmap = icon
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    consoleLogs.add(ConsoleLog(it.message(), it.messageLevel().name))
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        setOnLongClickListener {
                            val result = hitTestResult
                            if (result.type != WebView.HitTestResult.UNKNOWN_TYPE) {
                                contextMenuResult = result
                                showContextMenu = true
                                true
                            } else {
                                false
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (UrlUtils.isBookmarklet(url)) {
                                    showAddBookmarkletDialog = url
                                    return true
                                }
                                return false
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                if (tab.id == activeTab.id) {
                                    Toast.makeText(context, "WebView crashed, reloading...", Toast.LENGTH_SHORT).show()
                                    view?.reload()
                                }
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                tab.isLoading = true
                                if (tab.id == activeTab.id) {
                                    url?.let { urlInput = it }
                                }
                            url?.let { currentUrl ->
                                userScripts.filter { it.enabled && it.type == "userscript" && it.runAt == "start" }.forEach { script ->
                                    try {
                                        val patterns = script.matchPattern.split(",").map { it.trim() }
                                        val isMatch = patterns.any { pattern ->
                                            val regex = pattern.replace(".", "\\.")
                                                .replace("?", ".")
                                                .replace("*", ".*")
                                                .let { "^$it$" }
                                            currentUrl.matches(Regex(regex))
                                        }
                                        if (isMatch) {
                                            view?.evaluateJavascript("(function() { ${script.script} })();", null)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                tab.isLoading = false
                            if (settings.adBlockEnabled) {
                                view?.evaluateJavascript("""
                                    (function() {
                                        const selectors = [
                                            "div[class*='ad-']", "div[id*='ad-']", "div[class*='Ads']",
                                            "div[class*='banner-ad']", "ins.adsbygoogle", "iframe[id*='google_ads']",
                                            "div[id*='taboola']", "div[id*='outbrain']", "div[class*='sponsored-content']",
                                            "[id^='ad-']", "[class^='ad-']", "[class*='sponsored']", ".trc_rbox_container",
                                            "div[id^='google_ads_iframe']", "aside[class*='ad']", "section[class*='ad']"
                                        ];
                                        const style = document.createElement('style');
                                        style.innerHTML = selectors.join(', ') + ' { display: none !important; }';
                                        document.head.appendChild(style);
                                    })();
                                """.trimIndent(), null)
                            }
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

                                    userScripts.filter { it.enabled && it.type == "userscript" && it.runAt == "end" }.forEach { script ->
                                        try {
                                            val patterns = script.matchPattern.split(",").map { it.trim() }
                                            val isMatch = patterns.any { pattern ->
                                                val regex = pattern.replace(".", "\\.")
                                                    .replace("?", ".")
                                                    .replace("*", ".*")
                                                    .let { "^$it$" }
                                                it.matches(Regex(regex))
                                            }
                                            if (isMatch) {
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
                                            const selectors = 'video, audio, source, img, a[href*=".mp4"], a[href*=".m3u8"], a[href*=".mp3"], a[href*=".m4a"], a[href*=".wav"], a[href*=".jpg"], a[href*=".png"], a[href*=".webp"], a[href*=".gif"]';
                                            document.querySelectorAll(selectors).forEach(el => {
                                                let src = el.src || el.getAttribute('src') || el.currentSrc || el.href;
                                                if (src && src.startsWith('//')) src = 'https:' + src;
                                                if (src && src.startsWith('http') && !seen.has(src)) {
                                                    const urlObj = new URL(src);
                                                    const ext = urlObj.pathname.split('.').pop().toLowerCase();
                                                    const isVideo = ['mp4', 'm3u8', 'webm', 'mov', 'm4v'].includes(ext) || el.tagName.toLowerCase() === 'video';
                                                    const isAudio = ['mp3', 'm4a', 'wav', 'ogg', 'aac'].includes(ext) || el.tagName.toLowerCase() === 'audio';
                                                    const isImage = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'].includes(ext) || el.tagName.toLowerCase() === 'img';

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
                                            const socialDomains = ['instagram.com', 'x.com', 'twitter.com', 'facebook.com', 'tiktok.com', 'threads.net', 'vimeo.com', 'dailymotion.com', 'pinterest.com'];
                                            if (socialDomains.some(d => host.includes(d))) {
                                                // Aggressive detection for social media
                                                if (!seen.has(location.href)) {
                                                    seen.add(location.href);
                                                    media.push({
                                                        id: 'page-' + Date.now(),
                                                        src: location.href,
                                                        type: 'video',
                                                        title: (document.title || (host.split('.')[0] + ' Video'))
                                                    });
                                                }
                                            }

                                            // Check for HLS/M3U8 streams and large blobs
                                            performance.getEntriesByType('resource').forEach(resource => {
                                                const isHls = resource.name.includes('.m3u8') || resource.name.includes('.mpd');
                                                if (isHls && !seen.has(resource.name)) {
                                                    seen.add(resource.name);
                                                    media.push({
                                                        id: 'stream-' + Math.random().toString(36).substr(2, 5),
                                                        src: resource.name,
                                                        type: 'video',
                                                        title: 'Stream: ' + (document.title || 'Video')
                                                    });
                                                }
                                            });

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
                                val host = request?.url?.host ?: ""
                                if (settings.adBlockEnabled) {
                                    val isAd = ADS_DOMAINS.any { host.contains(it) }
                                    val isAnalytics = ANALYTICS_DOMAINS.any { host.contains(it) }
                                    val isSocial = SOCIAL_DOMAINS.any { host.contains(it) }

                                    if (isAd || isAnalytics || isSocial) {
                                        synchronized(viewModel.blockedTrackersByTab) {
                                            val category = when {
                                                isAd -> "[Ad]"
                                                isAnalytics -> "[Analytics]"
                                                isSocial -> "[Social]"
                                                else -> "[Other]"
                                            }
                                            val blockedSet = viewModel.blockedTrackersByTab.getOrPut(tab.id) { mutableSetOf() }
                                            blockedSet.add("$category $host")
                                        }
                                        return WebResourceResponse("text/plain", "UTF-8", null)
                                    }
                                }

                                // Privacy: Do Not Track
                                request?.requestHeaders?.put("DNT", "1")

                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                    if (url == null || url == "about:blank") {
                        loadUrl(tab.url)
                    }
                    }
                },
                update = { view ->
                    if (view.url != tab.url && !tab.url.startsWith("about:")) {
                        view.loadUrl(tab.url)
                    }

                    // Performance: Adjust cache based on connectivity
                    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    view.settings.cacheMode = if (activeNetwork?.isConnected == true) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK

                    view.settings.javaScriptEnabled = settings.javaScriptEnabled
                    view.settings.userAgentString = settings.customUserAgent ?: view.settings.userAgentString

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, isForceDark)
                    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        WebSettingsCompat.setForceDark(view.settings, if (isForceDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

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
                            PageUtils.saveAsMhtml(context, currentWebView, currentWebView.title ?: "Page")
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
                        ToolButton(Icons.Default.CameraAlt, "Screenshot", Color(0xFF06B6D4)) {
                            val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
                            PageUtils.takeScreenshot(context, currentWebView, currentWebView.title ?: "Page")
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
        val currentWebView = viewModel.getOrCreateWebView(activeTab.id, context)
        ReaderModeView(
            title = currentWebView.title ?: "Reader Mode",
            content = readerContent,
            onClose = { isReaderMode = false }
        )
    }

    if (showPrivacyReport) {
        val blockedTrackers = synchronized(viewModel.blockedTrackersByTab) {
            viewModel.blockedTrackersByTab[activeTab.id]?.toList() ?: emptyList()
        }

        val ads = blockedTrackers.filter { it.startsWith("[Ad]") }
        val analytics = blockedTrackers.filter { it.startsWith("[Analytics]") }
        val social = blockedTrackers.filter { it.startsWith("[Social]") }
        val others = blockedTrackers.filter { !it.startsWith("[Ad]") && !it.startsWith("[Analytics]") && !it.startsWith("[Social]") }

        AlertDialog(
            onDismissRequest = { showPrivacyReport = false },
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
                TextButton(onClick = { showPrivacyReport = false }) { Text("Close") }
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
        val result = contextMenuResult!!
        ModalBottomSheet(onDismissRequest = { showContextMenu = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
                val extra = result.extra
                when (result.type) {
                    WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        Text("Link Options", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                        ListItem(
                            headlineContent = { Text("Open in New Tab") },
                            leadingContent = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                            modifier = Modifier.clickable {
                                extra?.let { viewModel.createTab(it) }
                                showContextMenu = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Copy Link Address") },
                            leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            modifier = Modifier.clickable {
                                extra?.let {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", it))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                showContextMenu = false
                            }
                        )
                        if (extra != null && UrlUtils.isBookmarklet(extra)) {
                            ListItem(
                                headlineContent = { Text("Add to Bookmarklets") },
                                leadingContent = { Icon(Icons.Default.Javascript, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        database.userScriptDao().insertScript(
                                            com.omniweb.app.data.UserScript(
                                                name = "Saved Bookmarklet",
                                                script = extra.substringAfter("javascript:"),
                                                type = "bookmarklet",
                                                enabled = true
                                            )
                                        )
                                        Toast.makeText(context, "Added to bookmarklets", Toast.LENGTH_SHORT).show()
                                    }
                                    showContextMenu = false
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
                                extra?.let { downloadManager.startDownload(it, "Image") }
                                showContextMenu = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Open Image in New Tab") },
                            leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                            modifier = Modifier.clickable {
                                extra?.let { viewModel.createTab(it) }
                                showContextMenu = false
                            }
                        )
                    }
                    WebView.HitTestResult.PHONE_TYPE -> {
                         ListItem(
                            headlineContent = { Text("Call ${extra}") },
                            leadingContent = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$extra"))
                                context.startActivity(intent)
                                showContextMenu = false
                            }
                        )
                    }
                    WebView.HitTestResult.EMAIL_TYPE -> {
                         ListItem(
                            headlineContent = { Text("Email ${extra}") },
                            leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$extra"))
                                context.startActivity(intent)
                                showContextMenu = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeView(title: String, content: String, onClose: () -> Unit) {
    var fontSize by remember { mutableFloatStateOf(18f) }
    var theme by remember { mutableStateOf("light") } // "light", "dark"

    val isDark = when (theme) {
        "dark" -> true
        else -> false
    }

    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)

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
                    IconButton(onClick = { theme = if (theme == "light") "dark" else "light" }) {
                        Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle Theme")
                    }
                    IconButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(32f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "Increase Font")
                    }
                    IconButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(12f) }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "Decrease Font")
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
                color = textColor
            )
            Spacer(modifier = Modifier.height(24.dp))
            val cleanContent = content.replace(Regex("<[^>]*>"), "")
            Text(
                text = cleanContent,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6).sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}
