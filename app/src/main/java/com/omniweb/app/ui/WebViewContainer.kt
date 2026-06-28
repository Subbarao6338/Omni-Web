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
    onConsoleLog: (String, String) -> Unit
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

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        AndroidView(
            factory = { _ ->
                currentWebView.apply {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                        if (settings.torEnabled) {
                            val proxyConfig = ProxyConfig.Builder()
                                .addProxyRule("${settings.torProxyHost}:${settings.torProxyPort}")
                                .addDirect().build()
                            ProxyController.getInstance().setProxyOverride(proxyConfig, { }, { })
                        } else {
                            ProxyController.getInstance().clearProxyOverride({ }, { })
                        }
                    }

                    val initialHost = Uri.parse(tab.url).host ?: ""
                    val initialPerSite = viewModel.getPerSiteSettings(initialHost)

                    this.settings.apply {
                        javaScriptEnabled = initialPerSite?.javaScriptEnabled ?: settings.javaScriptEnabled
                        domStorageEnabled = true
                        setGeolocationEnabled(true)
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        allowContentAccess = true
                        allowFileAccess = true

                        val ua = if (initialPerSite?.desktopMode == true) {
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

                    setDownloadListener { url, _, _, _, _ ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        intent.data = android.net.Uri.parse(url)
                        context.startActivity(intent)
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
                        }
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

                    setOnScrollChangeListener { _, scrollX, scrollY, _, _ ->
                        onScrollChanged(scrollX, scrollY)
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
                            finalBundle.append(coreScripts).append("\n")

                            if (settings.forceZoom) {
                                finalBundle.append("(function() { const meta = document.querySelector('meta[name=\"viewport\"]'); if (meta) meta.setAttribute(\"content\",\"width=device-width\"); else { const n = document.createElement('meta'); n.name='viewport'; n.content='width=device-width'; document.head.appendChild(n); } })();").append("\n")
                            }

                            // Password Management
                            finalBundle.append("""
                                (function() {
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
                                })();
                            """.trimIndent()).append("\n")

                            finalBundle.append("Android.postText(document.body.innerText);\n")

                            // Media Sniffer
                            finalBundle.append(mediaSnifferScript()).append("\n")

                            // Playback Speed
                            finalBundle.append("(function() { document.querySelectorAll('video').forEach(v => v.playbackRate = ${tab.playbackSpeed}); })();\n")

                            // Anti-fingerprinting
                            if (settings.strictPrivacyMode) {
                                finalBundle.append(antiFingerprintScript()).append("\n")
                            }

                            // Annotations
                            finalBundle.append("""
                                (function() {
                                    Android.getAnnotations().then(json => {
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
                                    });
                                })();
                            """.trimIndent())

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

                view.settings.javaScriptEnabled = perSite?.javaScriptEnabled ?: settings.javaScriptEnabled

                val targetUa = if (perSite?.desktopMode == true) {
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

        // Minimize canvas fingerprinting by adding slight noise (placeholder logic)
        const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
        CanvasRenderingContext2D.prototype.getImageData = function() {
            const imageData = originalGetImageData.apply(this, arguments);
            // Could add subtle noise here
            return imageData;
        };
    })();
""".trimIndent()

private fun mediaSnifferScript() = """
    (function() {
        function sniff() {
            const media = [];
            const seen = new Set();
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
            performance.getEntriesByType('resource').forEach(resource => {
                const isHls = resource.name.includes('.m3u8') || resource.name.includes('.mpd') || resource.name.includes('.ts');
                if (isHls && !seen.has(resource.name)) {
                    // Ignore common noise
                    if (resource.name.includes('google-analytics') || resource.name.includes('doubleclick')) return;

                    seen.add(resource.name);
                    media.push({
                        id: 'stream-' + Math.random().toString(36).substr(2, 5),
                        src: resource.name,
                        type: 'video',
                        title: 'Stream: ' + (document.title || 'Video')
                    });
                }
            });
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
        }
    })();
""".trimIndent()
