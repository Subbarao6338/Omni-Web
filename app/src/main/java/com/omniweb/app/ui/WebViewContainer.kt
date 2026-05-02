package com.omniweb.app.ui

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.util.AdBlockManager
import com.omniweb.app.util.WebAppInterface
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

@Composable
fun WebViewContainer(
    tab: TabInfo,
    settings: Settings,
    webView: WebView,
    isForceDark: Boolean,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onFaviconChanged: (Bitmap?) -> Unit,
    onMediaDetected: (List<com.omniweb.app.data.MediaItem>) -> Unit,
    onLoginFormDetected: (String, String, String) -> Unit,
    onConsoleMessage: (String, String) -> Unit,
    onScrollChanged: (Int, Int) -> Unit,
    onLongClick: (WebView.HitTestResult) -> Unit,
    onBookmarkletDetected: (String) -> Unit,
    onTextExtracted: (String) -> Unit,
    onBlockedTracker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { _ ->
            webView.apply {
                this.settings.apply {
                    javaScriptEnabled = settings.javaScriptEnabled
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
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    allowContentAccess = true
                    allowFileAccess = true
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                    userAgentString = settings.customUserAgent ?: userAgentString
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
                    onMediaDetected = onMediaDetected,
                    onTextExtracted = onTextExtracted,
                    onLoginFormDetected = { user, pass ->
                        val site = Uri.parse(url).host ?: ""
                        onLoginFormDetected(site, user, pass)
                    }
                ), "Android")

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        if (title != null && !title.startsWith("http")) {
                            onTitleChanged(title)
                        }
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        super.onReceivedIcon(view, icon)
                        onFaviconChanged(icon)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            onConsoleMessage(it.message(), it.messageLevel().name)
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
                        onLongClick(result)
                        true
                    } else {
                        false
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (com.omniweb.app.util.UrlUtils.isBookmarklet(url)) {
                            onBookmarkletDetected(url)
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onUrlChanged(url ?: "")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onUrlChanged(url ?: "")
                        if (settings.adBlockEnabled) {
                            view?.evaluateJavascript(AdBlockManager.getAdBlockScript(), null)
                        }

                        // Password Injection
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

                        // Media Sniffer
                        view?.evaluateJavascript("""
                            (function() {
                                function sniff() {
                                    const media = [];
                                    const seen = new Set();

                                    // Resource timing API for HLS/M3U8 detection
                                    performance.getEntriesByType('resource').forEach(resource => {
                                        const name = resource.name;
                                        if ((name.includes('.m3u8') || name.includes('.mpd') || name.includes('.m4s')) && !seen.has(name)) {
                                            seen.add(name);
                                            media.push({
                                                id: 'res-' + Math.random().toString(36).substr(2, 5),
                                                src: name,
                                                type: 'video',
                                                title: (document.title || 'Stream') + ' (Sniffed)'
                                            });
                                        }
                                    });

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

                                    // Social media aggressive detection
                                    const host = location.host;
                                    if (host.includes('instagram.com') || host.includes('facebook.com') || host.includes('tiktok.com') || host.includes('x.com')) {
                                        if (!seen.has(location.href)) {
                                            seen.add(location.href);
                                            media.push({
                                                id: 'social-' + Date.now(),
                                                src: location.href,
                                                type: 'video',
                                                title: document.title || 'Social Video'
                                            });
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
                                }
                            })();
                        """.trimIndent(), null)
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val host = request?.url?.host ?: ""
                        if (settings.adBlockEnabled) {
                            val category = AdBlockManager.getCategory(host)
                            if (category != null) {
                                onBlockedTracker("$category $host")
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
                view.loadUrl(tab.url)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, isForceDark)
            }
        },
        modifier = modifier
    )
}
