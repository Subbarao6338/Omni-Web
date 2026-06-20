package com.nature.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nature.browser.db.AppDatabase
import com.nature.browser.db.SiteSettings
import com.nature.browser.db.ReadingListEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension
import java.io.File

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = BrowserStorage(application)
    val engine = BrowserEngine(application)
    private val repository = TabRepository(storage, engine)
    private val db = AppDatabase.getDatabase(application)

    val tabs = repository.tabs
    val activeTabId = repository.activeTabId

    private val _suggestions = MutableStateFlow<List<HistoryItem>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _isSplitScreen = MutableStateFlow(false)
    val isSplitScreen = _isSplitScreen.asStateFlow()

    private val _splitTabId = MutableStateFlow<String?>(null)
    val splitTabId = _splitTabId.asStateFlow()

    val readingList = db.readingListDao().getAllItems()

    private val extensionPorts = mutableMapOf<String, WebExtension.Port>()
    private val annotationJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    init {
        repository.onPageStartListener = { tabId, url ->
            applySettingsForTab(tabId, url)
        }
        repository.restoreTabs()
        setupMemoryPressureHandler()
    }

    private fun setupMemoryPressureHandler() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000)
                val currentTabs = tabs.value
                val now = System.currentTimeMillis()
                val hibernateThreshold = 5 * 60 * 1000

                currentTabs.forEach { tab ->
                    if (tab.id != activeTabId.value && tab.id != _splitTabId.value) {
                        if (now - tab.lastActive > hibernateThreshold && tab.session.isOpen) {
                            hibernateTab(tab)
                        }
                    }
                }
            }
        }
    }

    private fun hibernateTab(tab: TabModel) {
        if (tab.session.isOpen) {
            // Save state before closing for hibernation
            repository.saveTabsState()
            tab.session.close()
        }
    }

    fun addTab(url: String = storage.homepage, isIncognito: Boolean = false) {
        repository.addTab(url, isIncognito)
    }

    fun closeTab(tabId: String) {
        annotationJobs[tabId]?.cancel()
        annotationJobs.remove(tabId)
        extensionPorts.remove(tabId)
        repository.closeTab(tabId)
    }

    fun switchTab(tabId: String) {
        val oldTabId = activeTabId.value
        val oldTab = tabs.value.find { it.id == oldTabId }

        oldTab?.let {
            if (it.session.isOpen) {
                repository.saveTabsState()
            }
        }

        val tab = tabs.value.find { it.id == tabId }
        if (tab != null && !tab.session.isOpen) {
            engine.setupSession(
                session = tab.session,
                tab = tab,
                onPageStart = { startedUrl ->
                    applySettingsForTab(tab.id, startedUrl)
                },
                onStateChange = {
                    if (!tab.isIncognito) repository.saveTabsState()
                }
            )
            tab.savedState?.let {
                tab.session.restoreState(it)
            } ?: run {
                tab.session.loadUri(tab.url.value)
            }
        }
        repository.switchTab(tabId)
    }

    fun goBack(tabId: String) {
        val tab = tabs.value.find { it.id == tabId } ?: return
        tab.session.goBack()
    }

    fun goForward(tabId: String) {
        val tab = tabs.value.find { it.id == tabId } ?: return
        tab.session.goForward()
    }

    fun applySettingsForTab(tabId: String, url: String) {
        val domain = try { Uri.parse(url).host ?: "" } catch (e: Exception) { "" }
        if (domain.isEmpty()) return

        viewModelScope.launch {
            val settings = db.siteSettingsDao().getSettingsForDomain(domain)
            val tab = tabs.value.find { it.id == tabId } ?: return@launch

            tab.session.settings.allowJavascript = true

            engine.injectionExtension?.let { ext ->
                tab.session.webExtensionController.setMessageDelegate(ext, object : WebExtension.MessageDelegate {
                    override fun onConnect(port: WebExtension.Port) {
                        extensionPorts[tabId] = port
                        annotationJobs[tabId]?.cancel()
                        annotationJobs[tabId] = viewModelScope.launch {
                            db.annotationDao().getAnnotationsForUrl(url).collect { annotations ->
                                annotations.forEach { a ->
                                    val msg = org.json.JSONObject().apply {
                                        put("type", "show_annotation")
                                        put("text", a.text)
                                        put("color", String.format("#%06X", (0xFFFFFF and a.color)))
                                    }
                                    port.postMessage(msg)
                                }
                            }
                        }
                        settings?.let { s ->
                            s.customCss?.let { css ->
                                val msg = org.json.JSONObject().apply {
                                    put("type", "inject_css")
                                    put("css", css)
                                }
                                port.postMessage(msg)
                            }
                            s.customJs?.let { js ->
                                val msg = org.json.JSONObject().apply {
                                    put("type", "inject_js")
                                    put("js", js)
                                }
                                port.postMessage(msg)
                            }
                        }
                    }

                    override fun onMessage(nativeMessage: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
                        if (message is org.json.JSONObject) {
                            val type = message.optString("type")
                            if (type == "prefetch") {
                                val prefetchUrl = message.optString("url")
                                if (prefetchUrl.isNotEmpty()) {
                                    engine.prefetch(prefetchUrl)
                                }
                            } else if (type == "selection_change") {
                                tab.selectedText.value = message.optString("text")
                            }
                        }
                        return null
                    }
                }, "nature")
            }

            settings?.let { s ->
                tab.session.settings.userAgentMode = if (s.isDesktopMode) org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            }
        }
    }

    fun saveSiteSettings(settings: SiteSettings) {
        viewModelScope.launch {
            db.siteSettingsDao().saveSettings(settings)
        }
    }

    fun addToReadingList(tab: TabModel) {
        viewModelScope.launch {
            val url = tab.url.value
            val title = tab.title.value
            val fileName = "offline_${System.currentTimeMillis()}.html"
            val file = File(getApplication<Application>().filesDir, fileName)

            db.readingListDao().insertItem(ReadingListEntity(url, title, localFilePath = file.absolutePath))

            // In v122, we attempt to capture the DOM if possible, otherwise we save a nature-themed summary
            tab.session.webExtensionController.setMessageDelegate(engine.injectionExtension!!, object : org.mozilla.geckoview.WebExtension.MessageDelegate {
                override fun onMessage(nativeMessage: String, message: Any, sender: org.mozilla.geckoview.WebExtension.MessageSender): org.mozilla.geckoview.GeckoResult<Any>? {
                    if (message is org.json.JSONObject && message.optString("type") == "dom_content") {
                        val dom = message.optString("html")
                        val content = wrapWithNatureTheme(title, url, dom)
                        file.writeText(content)
                    }
                    return null
                }
            }, "reading_list")

            val port = extensionPorts[tab.id]
            if (port != null) {
                port.postMessage(org.json.JSONObject().apply { put("type", "get_dom") })
            } else {
                // Fallback
                file.writeText(wrapWithNatureTheme(title, url, "The essence of this page has been gathered. In the deep forest, this stream remains still."))
            }
        }
    }

    private fun wrapWithNatureTheme(title: String, url: String, content: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>$title</title>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body {
                        font-family: 'Lora', serif;
                        padding: 5% 10%;
                        background: #F9F6EF;
                        color: #264653;
                        line-height: 1.8;
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    .nature-border {
                        border-left: 4px solid #2A9D8F;
                        padding-left: 24px;
                        margin-bottom: 40px;
                        position: relative;
                    }
                    .metadata {
                        color: #57CC99;
                        font-size: 0.9em;
                        margin-bottom: 8px;
                        font-style: italic;
                    }
                    h1 { color: #2A9D8F; margin-top: 0; }
                    a { color: #2A9D8F; text-decoration: none; border-bottom: 1px solid rgba(42, 157, 143, 0.3); }
                    hr { border: 0; border-top: 1px solid #E0E0E0; margin: 32px 0; }
                    #offline-banner { margin-top: 40px; padding: 20px; background: rgba(87, 204, 153, 0.1); border-radius: 8px; border: 1px solid #57CC99; }
                </style>
            </head>
            <body>
                <div class="nature-border">
                    <div class="metadata">Preserved from the stream on ${java.util.Date()}</div>
                    <h1>$title</h1>
                    <p>Source: <a href="$url">$url</a></p>
                </div>
                <hr>
                <div id="content">$content</div>
                <div id="offline-banner">
                    <strong>Offline</strong> — This content was gathered on ${java.util.Date()}. Some interactive elements may be still as a frozen pond.
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun toggleSplitScreen() {
        if (!_isSplitScreen.value) {
            val splitTabUrl = storage.homepage
            addTab(splitTabUrl)
            _splitTabId.value = tabs.value.last().id
            _isSplitScreen.value = true
        } else {
            _isSplitScreen.value = false
            _splitTabId.value = null
        }
    }

    fun updateSuggestions(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                _suggestions.value = emptyList()
            } else {
                _suggestions.value = storage.searchHistory(query)
            }
        }
    }

    fun toggleReaderMode(tabId: String) {
        val tab = tabs.value.find { it.id == tabId } ?: return
        if (tab.readerContent.value != null) {
            tab.readerContent.value = null
        } else {
            tab.readerContent.value = "Nature's stream of content flows here..."
        }
    }

    fun setVideoSpeed(tabId: String, speed: Float) {
        viewModelScope.launch {
            val port = extensionPorts[tabId]
            if (port != null) {
                val msg = org.json.JSONObject().apply {
                    put("type", "set_video_speed")
                    put("speed", speed.toDouble())
                }
                port.postMessage(msg)
            }
        }
    }

    fun getAnnotationsForUrl(url: String) = db.annotationDao().getAnnotationsForUrl(url)

    fun addAnnotation(annotation: com.nature.browser.db.AnnotationEntity) {
        viewModelScope.launch {
            db.annotationDao().insertAnnotation(annotation)
            // Push to extension to show highlights immediately
            val tab = tabs.value.find { it.url.value == annotation.url }
            tab?.let { t ->
                val port = extensionPorts[t.id]
                if (port != null) {
                    val msg = org.json.JSONObject().apply {
                        put("type", "show_annotation")
                        put("text", annotation.text)
                        put("color", String.format("#%06X", (0xFFFFFF and annotation.color)))
                    }
                    port.postMessage(msg)
                }
            }
        }
    }

    fun saveNamedSession(name: String) {
        viewModelScope.launch {
            val sessionTabs = tabs.value.filter { !it.isIncognito }.map {
                com.nature.browser.db.NamedSessionTab(
                    sessionName = name,
                    url = it.url.value,
                    title = it.title.value
                )
            }
            db.namedSessionDao().saveSession(name, sessionTabs)
        }
    }

    fun restoreNamedSession(name: String) {
        viewModelScope.launch {
            val sessionTabs = db.namedSessionDao().getTabsForSession(name)
            if (sessionTabs.isNotEmpty()) {
                sessionTabs.forEach {
                    addTab(it.url)
                }
            }
        }
    }
}
