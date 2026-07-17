package com.omniweb.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.net.ConnectivityManager
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.data.AnnotationEntity
import com.omniweb.app.data.MediaItem
import com.omniweb.app.util.AdBlockManager
import com.omniweb.app.util.UrlUtils
import com.omniweb.app.util.WebAppInterface
import com.omniweb.app.util.ScriptProvider
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: TabInfo,
    viewModel: BrowserViewModel,
    settings: Settings,
    onLoginDetected: (String, String, String) -> Unit,
    onBookmarkletDetected: (String) -> Unit,
    onTextExtracted: (String) -> Unit,
    onScrollChanged: (Int, Int) -> Unit,
    onContextMenu: (WebView.HitTestResult) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onTitleReceived: (String) -> Unit,
    onIconReceived: (Bitmap?) -> Unit,
    onConsoleLog: (String, String) -> Unit,
    onDownload: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentWebView = remember(tab.id) { viewModel.getOrCreateWebView(tab.id, context) }
    val scriptProvider = remember { ScriptProvider(context) }

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

    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            currentWebView.reload()
            delay(500)
            while (tab.isLoading) { delay(100) }
            pullToRefreshState.endRefresh()
        }
    }

    // Dynamic Tor settings observer using LaunchedEffect to avoid redundant/frequent updates in the update block of AndroidView
    LaunchedEffect(settings.torEnabled, settings.torProxyHost, settings.torProxyPort) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (settings.torEnabled) {
                val proxyConfig = ProxyConfig.Builder()
                    .addProxyRule("${settings.torProxyHost}:${settings.torProxyPort}")
                    .addDirect().build()
                ProxyController.getInstance().setProxyOverride(proxyConfig, { run {} }, { })
            } else {
                ProxyController.getInstance().clearProxyOverride({ run {} }, { })
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        AndroidView(
            factory = { _ ->
                currentWebView.apply {
                    (parent as? ViewGroup)?.removeView(this)

                    val initialHost = Uri.parse(tab.url).host ?: ""
                    val initialPerSite = viewModel.getPerSiteSettings(initialHost)

                    this.settings.apply {
                        javaScriptEnabled = initialPerSite?.javaScriptEnabled ?: settings.javaScriptEnabled
                        domStorageEnabled = true
                        setGeolocationEnabled(true)
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        allowContentAccess = true
                        allowFileAccess = true

                        val ua = if (initialPerSite?.customUserAgent != null) {
                            initialPerSite.customUserAgent
                        } else if (initialPerSite?.desktopMode == true) {
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else if (settings.strictPrivacyMode) {
                            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        } else {
                            settings.customUserAgent ?: userAgentString
                        }
                        userAgentString = ua

                        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                            WebSettingsCompat.setSafeBrowsingEnabled(this, true)
                        }
                    }

                    AdBlockManager.init(context.applicationContext)

                    if (tab.isIncognito) {
                        CookieManager.getInstance().setAcceptCookie(false)
                        this.settings.domStorageEnabled = false
                        this.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    } else {
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, !settings.blockThirdPartyCookies)
                    }

                    setDownloadListener { url, _, contentDisposition, mimetype, _ ->
                        val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                        onDownload(url, fileName)
                    }

                    addJavascriptInterface(WebAppInterface(
                        onMediaDetected = {
                            tab.detectedMedia.clear()
                            tab.detectedMedia.addAll(it)
                        },
                        onTextExtracted = { onTextExtracted(it) },
                        onLoginFormDetected = { user, pass ->
                            val site = Uri.parse(url).host ?: ""
                            if (site.isNotEmpty()) {
                                onLoginDetected(site, user, pass)
                            }
                            },
                            onGetAnnotations = {
                                val list = kotlinx.coroutines.runBlocking { viewModel.getAnnotationsForUrl(tab.url).firstOrNull() } ?: emptyList<AnnotationEntity>()
                                val array = org.json.JSONArray()
                                list.forEach { a: AnnotationEntity ->
                                    val obj = org.json.JSONObject()
                                    obj.put("text", a.text)
                                    obj.put("color", "#" + Integer.toHexString(a.color).takeLast(6))
                                    array.put(obj)
                                }
                                array.toString()
                        },
                        onPageReadable = { tab.isPageReadable = it }
                    ), "Android")

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            val progress = newProgress / 100f
                            onProgressChanged(progress)
                            if (newProgress == 100) tab.isLoading = false
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            if (title != null && !title.startsWith("http")) {
                                onTitleReceived(title)
                            }
                        }

                        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                            super.onReceivedIcon(view, icon)
                            onIconReceived(icon)
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                onConsoleLog(it.message(), it.messageLevel().name)
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    setOnScrollChangeListener { v, scrollX, scrollY, _, _ ->
                        onScrollChanged(scrollX, scrollY)
                        val webView = v as WebView
                        @Suppress("DEPRECATION")
                        val contentHeight = webView.contentHeight * webView.scale
                        val totalScrollable = contentHeight - webView.height
                        if (totalScrollable > 0) {
                            tab.scrollProgress = scrollY.toFloat() / totalScrollable
                        }
                    }

                    setOnLongClickListener {
                        val result = hitTestResult
                        if (result.type != WebView.HitTestResult.UNKNOWN_TYPE) {
                            onContextMenu(result)
                            true
                        } else {
                            false
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (UrlUtils.isBookmarklet(url)) {
                                onBookmarkletDetected(url)
                                return true
                            }

                            val redirect = viewModel.getRedirect(url)
                            if (redirect != null) {
                                view?.loadUrl(redirect)
                                return true
                            }

                            if (viewModel.isUrlBlocked(url)) {
                                android.widget.Toast.makeText(context, "This site is blocked by parental controls", android.widget.Toast.LENGTH_SHORT).show()
                                return true
                            }

                            if (settings.httpsOnlyMode && url.startsWith("http://")) {
                                val httpsUrl = url.replace("http://", "https://")
                                view?.loadUrl(httpsUrl)
                                return true
                            }

                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            tab.isLoading = true
                            url?.let {
                                if (viewModel.isUrlBlocked(it)) {
                                    view?.stopLoading()
                                    android.widget.Toast.makeText(context, "This site is blocked by parental controls", android.widget.Toast.LENGTH_SHORT).show()
                                    return
                                }

                                tab.url = it
                                val uri = Uri.parse(it)
                                val host = uri.host ?: ""
                                viewModel.preloadPerSiteSettings(host)

                                // DNS Pre-fetch for common resources
                                view?.evaluateJavascript("""
                                    (function() {
                                        if (window.omniDnsPrefetched) return;
                                        window.omniDnsPrefetched = true;
                                        const link = document.createElement('link');
                                        link.rel = 'dns-prefetch';
                                        link.href = '${uri.scheme}://${host}';
                                        document.head.appendChild(link);
                                    })();
                                """.trimIndent(), null)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            tab.isLoading = false
                            view?.let { tab.thumbnail = com.omniweb.app.util.PageUtils.captureTabThumbnail(it) }

                            val host = Uri.parse(url ?: "").host ?: ""
                            val perSite = viewModel.getPerSiteSettings(host)
                            val adBlockEnabled = perSite?.adBlockEnabled ?: settings.adBlockEnabled

                            val coreScripts = scriptProvider.getAllInjectedScripts(
                                blockAMP = settings.ampBlockingEnabled,
                                cookieBlock = true,
                                textReflow = settings.textReflowEnabled,
                                invertPage = settings.invertPageEnabled,
                                deepDarkMode = settings.deepDarkMode,
                                forceLightTheme = settings.forceLightTheme,
                                forceBlackTheme = settings.forceBlackTheme,
                                adBlockEnabled = adBlockEnabled
                            )

                            val finalBundle = StringBuilder()
                            finalBundle.append("(function() {\n")
                            finalBundle.append("if (window.omniScriptsInjected) return;\n")
                            finalBundle.append("window.omniScriptsInjected = true;\n")
                            finalBundle.append(coreScripts).append("\n")

                            if (settings.forceZoom) {
                                finalBundle.append("(function() { const meta = document.querySelector('meta[name=\"viewport\"]'); if (meta) meta.setAttribute(\"content\",\"width=device-width\"); else { const n = document.createElement('meta'); n.name='viewport'; n.content='width=device-width'; document.head.appendChild(n); } })();").append("\n")
                            }

                            // Combined Injections
                            finalBundle.append("""
                                (function() {
                                    // Password Management
                                    function findForms() {
                                        document.querySelectorAll('form').forEach(form => {
                                            form.addEventListener('submit', function() {
                                                const userField = form.querySelector('input[type="text"], input[type="email"], input:not([type])');
                                                const passField = form.querySelector('input[type="password"]');
                                                if (userField && passField && userField.value && passField.value) {
                                                    Android.onLoginDetected(userField.value, passField.value);
                                                }
                                            });
                                        });
                                    }
                                    setTimeout(findForms, 1000);

                                    // Annotations
                                    Android.getAnnotations().then(json => {
                                        try {
                                            const annotations = JSON.parse(json);
                                            annotations.forEach(a => {
                                                const text = a.text;
                                                const color = a.color;
                                                function highlight(node) {
                                                    if (node.nodeType === 3) {
                                                        const index = node.data.indexOf(text);
                                                        if (index >= 0) {
                                                            const range = document.createRange();
                                                            range.setStart(node, index);
                                                            range.setEnd(node, index + text.length);
                                                            const mark = document.createElement('mark');
                                                            mark.style.backgroundColor = color;
                                                            range.surroundContents(mark);
                                                        }
                                                    } else if (node.nodeType === 1 && node.childNodes && !/(script|style|mark)/i.test(node.tagName)) {
                                                        for (let i = 0; i < node.childNodes.length; i++) {
                                                            highlight(node.childNodes[i]);
                                                        }
                                                    }
                                                }
                                                highlight(document.body);
                                            });
                                        } catch(e) {}
                                    });

                                    // Metadata & Media
                                    Android.postText(document.body.innerText);

                                    // Reader Mode Detection (Simple Heuristic)
                                    const paragraphs = document.querySelectorAll('p');
                                    let totalText = 0;
                                    paragraphs.forEach(p => totalText += p.innerText.length);
                                    if (totalText > 1500 || paragraphs.length > 5) {
                                        Android.onPageReadable(true);
                                    } else {
                                        Android.onPageReadable(false);
                                    }
                                })();
                            """.trimIndent()).append("\n")

                            finalBundle.append(mediaSnifferScript()).append("\n")
                            finalBundle.append("(function() { document.querySelectorAll('video').forEach(v => v.playbackRate = ${tab.playbackSpeed}); })();\n")

                            if (settings.strictPrivacyMode) {
                                finalBundle.append(antiFingerprintScript()).append("\n")
                            }
                            finalBundle.append("})();")

                            view?.evaluateJavascript(finalBundle.toString(), null)
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val reqHost = request?.url?.host ?: ""
                            if (reqHost.isBlank()) return null

                            val pageHost = Uri.parse(tab.url).host ?: ""
                            val perSite = viewModel.getPerSiteSettings(pageHost)
                            val adBlockEnabled = perSite?.adBlockEnabled ?: settings.adBlockEnabled

                            if (adBlockEnabled) {
                                val category = AdBlockManager.getCategory(reqHost)
                                if (category != null) {
                                    val blockedSet = viewModel.blockedTrackersByTab.getOrPut(tab.id) { java.util.concurrent.ConcurrentHashMap.newKeySet<String>() }
                                    blockedSet.add("$category $reqHost")
                                    return WebResourceResponse("text/plain", "UTF-8", null)
                                }
                            }

                            val reqUrl = request?.url?.toString() ?: ""
                            if (reqUrl.isNotBlank()) {
                                sniffRequest(reqUrl, tab)
                            }

                            // Don't add headers to third party requests to avoid issues
                            if (reqHost == pageHost) {
                                request?.requestHeaders?.put("DNT", "1")
                            }
                            return null
                        }
                    }
                }
            },
            update = { view ->
                val currentUrl = view.url
                val originalUrl = view.originalUrl
                val targetUrl = tab.url

                if (targetUrl != currentUrl && targetUrl != originalUrl && !targetUrl.startsWith("about:")) {
                    if (targetUrl.startsWith("/")) {
                        view.loadUrl("file://" + tab.url)
                    } else {
                        view.loadUrl(tab.url)
                    }
                }

                val cm = context.getSystemService(ConnectivityManager::class.java)
                val network = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(network)
                val isSlowConnection = capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.linkDownstreamBandwidthKbps < 2000
                )

                val isConnected = capabilities != null && (
                    capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )

                view.settings.cacheMode = if (isConnected) {
                    if (isSlowConnection) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
                } else {
                    WebSettingsCompat.setSafeBrowsingEnabled(view.settings, false)
                    WebSettings.LOAD_CACHE_ONLY
                }

                val host = Uri.parse(view.url ?: tab.url).host ?: ""
                val perSite = viewModel.getPerSiteSettings(host)

                val targetJsEnabled = perSite?.javaScriptEnabled ?: settings.javaScriptEnabled
                if (view.settings.javaScriptEnabled != targetJsEnabled) {
                    view.settings.javaScriptEnabled = targetJsEnabled
                }

                val targetUa = if (perSite?.customUserAgent != null) {
                    perSite.customUserAgent
                } else if (perSite?.desktopMode == true) {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                } else if (settings.strictPrivacyMode) {
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                } else {
                    settings.customUserAgent ?: "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                if (view.settings.userAgentString != targetUa) {
                    view.settings.userAgentString = targetUa
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    val shouldDarken = (settings.darkMode || settings.forceBlackTheme) && !settings.forceLightTheme
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, shouldDarken)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun sniffRequest(url: String, tab: TabInfo) {
    val lowerUrl = url.lowercase()

    val isVideo = listOf(".mp4", ".m3u8", ".mpd", ".webm", ".mov", ".m4v", ".3gp", ".ts", ".avi", ".mkv").any { lowerUrl.contains(it) }
    val isAudio = listOf(".mp3", ".m4a", ".wav", ".ogg", ".aac", ".flac", ".wma").any { lowerUrl.contains(it) }
    val isImage = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".ico").any { lowerUrl.contains(it) }

    if (isVideo || isAudio || isImage) {
        // Ignore analytics, advertising, telemetry, trackers
        if (lowerUrl.contains("google-analytics") || lowerUrl.contains("doubleclick") || lowerUrl.contains("telemetry") || lowerUrl.contains("/ad/") || lowerUrl.contains("tracker")) {
            return
        }

        val type = if (isVideo) "video" else if (isAudio) "audio" else "image"

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            // Check if already contains
            if (tab.detectedMedia.none { it.src == url }) {
                val title = url.substringBefore("?").substringAfterLast("/").ifBlank { "Media File" }
                val id = "sniffed-" + Math.abs(url.hashCode()).toString(36)
                tab.detectedMedia.add(MediaItem(id = id, type = type, src = url, title = title))
            }
        }
    }
}

private fun antiFingerprintScript() = """
    (function() {
        const hideProperty = (obj, prop, value) => {
            Object.defineProperty(obj, prop, {
                get: () => value,
                enumerable: true,
                configurable: false
            });
        };
        hideProperty(navigator, 'platform', 'Linux armv8l');
        hideProperty(navigator, 'webdriver', false);
        hideProperty(navigator, 'plugins', []);
        hideProperty(navigator, 'languages', ['en-US', 'en']);

        // Minimize canvas fingerprinting by adding slight noise
        const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
        CanvasRenderingContext2D.prototype.getImageData = function() {
            const imageData = originalGetImageData.apply(this, arguments);
            const pixels = imageData.data;
            for (let i = 0; i < pixels.length; i += 4) {
                // Add extremely subtle noise to the least significant bits of RGB
                pixels[i] = pixels[i] ^ (Math.random() < 0.1 ? 1 : 0);
                pixels[i+1] = pixels[i+1] ^ (Math.random() < 0.1 ? 1 : 0);
                pixels[i+2] = pixels[i+2] ^ (Math.random() < 0.1 ? 1 : 0);
            }
            return imageData;
        };
    })();
""".trimIndent()

private fun mediaSnifferScript() = """
    (function() {
        let lastReportedJson = '';
        let sniffTimeout = null;

        function getHash(str) {
            let hash = 0;
            for (let i = 0; i < str.length; i++) {
                const char = str.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash |= 0;
            }
            return Math.abs(hash).toString(36);
        }

        function sniff() {
            const media = [];
            const seen = new Set();
            const selectors = 'video, audio, source, img, a[href*=".mp4"], a[href*=".m3u8"], a[href*=".mpd"], a[href*=".mp3"], a[href*=".m4a"], a[href*=".wav"], a[href*=".jpg"], a[href*=".png"], a[href*=".webp"], a[href*=".gif"]';

            document.querySelectorAll(selectors).forEach(el => {
                let src = el.src || el.getAttribute('src') || el.currentSrc || el.href;
                if (src && src.startsWith('//')) src = 'https:' + src;
                if (src && src.startsWith('http') && !seen.has(src)) {
                    try {
                        const urlObj = new URL(src);
                        const ext = urlObj.pathname.split('.').pop().toLowerCase();
                        const isVideo = ['mp4', 'm3u8', 'mpd', 'webm', 'mov', 'm4v', '3gp', 'ts', 'avi', 'mkv'].includes(ext) || el.tagName.toLowerCase() === 'video';
                        const isAudio = ['mp3', 'm4a', 'wav', 'ogg', 'aac', 'flac', 'wma'].includes(ext) || el.tagName.toLowerCase() === 'audio';
                        const isImage = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg', 'ico'].includes(ext) || el.tagName.toLowerCase() === 'img';
                        if (isVideo || isAudio || isImage) {
                            seen.add(src);
                            media.push({
                                id: getHash(src),
                                src: src,
                                type: isVideo ? 'video' : (isAudio ? 'audio' : 'image'),
                                title: document.title || 'Media File'
                            });
                        }
                    } catch(e) {}
                }
            });

            if (window.performance && window.performance.getEntriesByType) {
                performance.getEntriesByType('resource').forEach(resource => {
                    const rUrl = resource.name;
                    if (rUrl && rUrl.startsWith('http') && !seen.has(rUrl)) {
                        if (rUrl.includes('google-analytics') || rUrl.includes('doubleclick') || rUrl.includes('telemetry') || rUrl.includes('/ad/') || rUrl.includes('tracker')) return;

                        try {
                            const urlObj = new URL(rUrl);
                            const ext = urlObj.pathname.split('.').pop().toLowerCase();
                            const isVideo = ['mp4', 'm3u8', 'mpd', 'webm', 'mov', 'm4v', '3gp', 'ts', 'avi', 'mkv'].includes(ext);
                            const isAudio = ['mp3', 'm4a', 'wav', 'ogg', 'aac', 'flac', 'wma'].includes(ext);
                            const isImage = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg', 'ico'].includes(ext);

                            if (isVideo || isAudio || isImage) {
                                seen.add(rUrl);
                                media.push({
                                    id: 'resource-' + getHash(rUrl),
                                    src: rUrl,
                                    type: isVideo ? 'video' : (isAudio ? 'audio' : 'image'),
                                    title: 'Network resource: ' + (rUrl.split('/').pop().split('?')[0] || 'Media')
                                });
                            }
                        } catch(e) {}
                    }
                });
            }

            attachMediaListeners();

            if (media.length > 0) {
                const currentJson = JSON.stringify(media);
                if (currentJson !== lastReportedJson) {
                    lastReportedJson = currentJson;
                    Android.postMedia(currentJson);
                }
            }
        }

        function attachMediaListeners() {
            document.querySelectorAll('video, audio').forEach(el => {
                if (!el.omniSniffed) {
                    el.omniSniffed = true;
                    ['play', 'load', 'loadedmetadata'].forEach(evt => {
                        el.addEventListener(evt, sniff);
                    });
                }
            });
        }

        function debouncedSniff() {
            if (sniffTimeout) clearTimeout(sniffTimeout);
            sniffTimeout = setTimeout(sniff, 1000);
        }

        if (!window.omniSnifferStarted) {
            window.omniSnifferStarted = true;
            const observer = new MutationObserver((mutations) => {
                let shouldSniff = false;
                for (const mutation of mutations) {
                    if (mutation.addedNodes.length > 0) {
                        shouldSniff = true;
                        break;
                    }
                }
                if (shouldSniff) debouncedSniff();
            });
            observer.observe(document.documentElement, { childList: true, subtree: true });
            setInterval(sniff, 15000);
            sniff();
        }
    })();
""".trimIndent()
