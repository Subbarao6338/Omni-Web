package com.omniweb.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.util.AdBlockManager
import com.omniweb.app.util.UrlUtils
import com.omniweb.app.util.WebAppInterface
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay

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
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
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
                    val host = Uri.parse(tab.url).host ?: ""
                    val perSite = viewModel.getPerSiteSettings(host)

                    this.settings.apply {
                        javaScriptEnabled = perSite?.javaScriptEnabled ?: settings.javaScriptEnabled
                        domStorageEnabled = true
                        databaseEnabled = true
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
                        setRenderPriority(WebSettings.RenderPriority.HIGH)

                        val ua = if (perSite?.desktopMode == true) {
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else {
                            settings.customUserAgent ?: userAgentString
                        }
                        userAgentString = ua

                        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                            WebSettingsCompat.setSafeBrowsingEnabled(this, true)
                        }
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
                        onTextExtracted = { onTextExtracted(it) },
                        onLoginFormDetected = { user, pass ->
                            val site = Uri.parse(url).host ?: ""
                            if (site.isNotEmpty()) {
                                onLoginDetected(site, user, pass)
                            }
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
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            tab.isLoading = true
                            url?.let {
                                val host = Uri.parse(it).host ?: ""
                                viewModel.preloadPerSiteSettings(host)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            tab.isLoading = false
                            if (settings.adBlockEnabled) {
                                view?.evaluateJavascript(AdBlockManager.getAdBlockScript(), null)
                            }

                            // Password Management: Injection
                            view?.evaluateJavascript("""
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
                            """.trimIndent(), null)

                            view?.evaluateJavascript("Android.postText(document.body.innerText)", null)

                            // Sniffer injection
                            view?.evaluateJavascript(mediaSnifferScript(), null)
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val reqHost = request?.url?.host ?: ""
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
                            request?.requestHeaders?.put("DNT", "1")
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                }
            },
            update = { view ->
                if (view.url != tab.url && !tab.url.startsWith("about:")) {
                    if (tab.url.startsWith("/")) {
                        view.loadUrl("file://" + tab.url)
                    } else {
                        view.loadUrl(tab.url)
                    }
                }

                val cm = context.getSystemService(ConnectivityManager::class.java)
                val activeNetwork = cm.activeNetworkInfo
                view.settings.cacheMode = if (activeNetwork?.isConnected == true) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK

                view.settings.javaScriptEnabled = settings.javaScriptEnabled

                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, settings.darkMode)
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
