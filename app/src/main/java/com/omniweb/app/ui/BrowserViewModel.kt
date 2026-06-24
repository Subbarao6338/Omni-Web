package com.omniweb.app.ui

import android.app.Application
import android.content.MutableContextWrapper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omniweb.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import org.json.JSONArray
import com.omniweb.app.BuildConfig
import com.omniweb.app.util.adblock.BloomFilterAdBlocker
import com.omniweb.app.util.AccessibilityTools

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val tabs = mutableStateListOf<TabInfo>()
    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()
    private val webViewCache = object : java.util.LinkedHashMap<String, WebView>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WebView>?): Boolean {
            if (size > 15) {
                eldest?.let { entry ->
                    val webView = entry.value
                    val state = android.os.Bundle()
                    webView.saveState(state)
                    webViewStateCache[entry.key] = state

                    webView.stopLoading()
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.clearHistory()
                    webView.clearCache(false)

                    // Only destroy if it's not the active or split tab
                    if (entry.key != _activeTabId.value && entry.key != _splitTabId.value) {
                        webView.removeAllViews()
                        webView.destroy()
                    }
                }
                return true
            }
            return false
        }
    }
    private var prewarmedWebView: WebView? = null
    private val webViewStateCache = mutableMapOf<String, android.os.Bundle>()
    private val _searchQuery = MutableStateFlow("")
    private val suggestionCache = mutableMapOf<String, List<Suggestion>>()
    val recentlyClosedTabs = mutableStateListOf<TabInfo>()

    private val _isSplitScreen = MutableStateFlow(false)
    val isSplitScreen = _isSplitScreen.asStateFlow()

    private val _splitTabId = MutableStateFlow<String?>(null)
    val splitTabId = _splitTabId.asStateFlow()

    private val _isZenMode = MutableStateFlow(false)
    val isZenMode = _isZenMode.asStateFlow()

    val blockedTrackersByTab = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()
    private val bloomFilterAdBlocker = BloomFilterAdBlocker(application)
    private var redirectManager: com.omniweb.app.util.RedirectManager? = null
    private val accessibilityTools = AccessibilityTools(application)
    private val tabLastActive = mutableMapOf<String, Long>()
    private val perSiteSettingsCache = mutableMapOf<String, PerSiteSettings>()

    init {
        // Initialize with a default tab to avoid empty state; it will be replaced if saved tabs are restored.
        val initialId = UUID.randomUUID().toString()
        tabs.add(TabInfo(initialId, "about:home", "Home"))
        _activeTabId.value = initialId

        viewModelScope.launch {
            database.customRedirectDao().getAllRedirects().collect {
                redirectManager = com.omniweb.app.util.RedirectManager(it)
            }
        }

        viewModelScope.launch {
            val currentSettings = database.settingsDao().getSettings().firstOrNull() ?: Settings()
            val savedTabs = database.tabDao().getAllTabs().firstOrNull() ?: emptyList()

            if (currentSettings.restoreTabsOnStart && savedTabs.isNotEmpty()) {
                val restoredTabs = savedTabs.map { entry ->
                    TabInfo(entry.id, entry.url, entry.title, entry.isIncognito, entry.scrollX, entry.scrollY)
                }
                tabs.clear()
                tabs.addAll(restoredTabs)
                _activeTabId.value = restoredTabs.first().id
            } else {
                if (!currentSettings.restoreTabsOnStart) {
                    database.tabDao().clearAllTabs()
                }
                // If not restoring, the default tab already created suffices.
                // We should save it to DB to ensure consistency.
                saveTabToDb(tabs.first())
            }
        }

        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        fetchSuggestionsInternal(query)
                    } else {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }
    }

    private fun createDefaultTab() {
        val id = UUID.randomUUID().toString()
        val newTab = TabInfo(id, "about:home", "Home")
        tabs.add(newTab)
        _activeTabId.value = id
        saveTabToDb(newTab)
    }

    private fun saveTabToDb(tab: TabInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            database.tabDao().insertTab(TabEntry(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                position = tabs.indexOf(tab),
                isIncognito = tab.isIncognito,
                scrollX = tab.scrollX,
                scrollY = tab.scrollY
            ))
        }
    }

    fun updateTabInDb(tab: TabInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = TabEntry(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                position = tabs.indexOf(tab),
                isIncognito = tab.isIncognito,
                scrollX = tab.scrollX,
                scrollY = tab.scrollY
            )
            database.tabDao().updateTab(entry)
        }
    }

    private var lastScrollUpdate = 0L
    fun updateTabScroll(tabId: String, x: Int, y: Int) {
        tabs.find { it.id == tabId }?.let { tab ->
            tab.scrollX = x
            tab.scrollY = y

            val now = System.currentTimeMillis()
            if (now - lastScrollUpdate > 5000) { // Throttled to every 5 seconds
                lastScrollUpdate = now
                viewModelScope.launch(Dispatchers.IO) {
                    val entry = TabEntry(
                        id = tab.id,
                        url = tab.url,
                        title = tab.title,
                        position = tabs.indexOf(tab),
                        isIncognito = tab.isIncognito,
                        scrollX = tab.scrollX,
                        scrollY = tab.scrollY
                    )
                    database.tabDao().updateTab(entry)
                }
            }
        }
    }

    fun getOrCreateWebView(tabId: String, context: android.content.Context): WebView {
        val existing = webViewCache[tabId]
        if (existing != null) {
            (existing.context as? MutableContextWrapper)?.baseContext = context
            return existing
        }

        // Use applicationContext for pre-warmed WebView to avoid leaking Activities
        val webView = prewarmedWebView ?: createWebView(context.applicationContext)
        prewarmedWebView = null

        (webView.context as? MutableContextWrapper)?.baseContext = context

        webView.apply {
            webViewStateCache[tabId]?.let { state ->
                restoreState(state)
                webViewStateCache.remove(tabId)
            }
        }
        webViewCache[tabId] = webView

        // Prepare next prewarmed WebView using applicationContext
        viewModelScope.launch(Dispatchers.Main) {
            if (prewarmedWebView == null) {
                prewarmedWebView = createWebView(context.applicationContext)
            }
        }

        return webView
    }

    private fun createWebView(context: android.content.Context): WebView {
        return WebView(MutableContextWrapper(context)).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val settings = database.settingsDao().getSettings().map {
        it?.copy(geminiApiKey = it.geminiApiKey ?: BuildConfig.GEMINI_API_KEY) ?: Settings(geminiApiKey = BuildConfig.GEMINI_API_KEY)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings(geminiApiKey = BuildConfig.GEMINI_API_KEY)
    )

    private val _searchSuggestions = mutableStateOf<List<Suggestion>>(emptyList())
    val searchSuggestions get() = _searchSuggestions

    fun createTab(url: String = "about:home", title: String = "Home", isIncognito: Boolean = false) {
        val finalIncognito = isIncognito || settings.value.alwaysIncognito
        val newTab = TabInfo(UUID.randomUUID().toString(), url, title, finalIncognito)
        tabs.add(newTab)
        _activeTabId.value = newTab.id
        saveTabToDb(newTab)
    }

    fun isUrlBlocked(url: String): Boolean {
        val blockedJson = settings.value.blockedSites ?: return false
        try {
            val arr = JSONArray(blockedJson)
            val uri = android.net.Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false

            for (i in 0 until arr.length()) {
                val blocked = arr.getString(i).lowercase()
                if (host == blocked || host.endsWith(".$blocked")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Log error
        }
        return false
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            val removedTab = tabs.removeAt(index)
            recentlyClosedTabs.add(0, removedTab)
            if (recentlyClosedTabs.size > 10) recentlyClosedTabs.removeAt(recentlyClosedTabs.lastIndex)

            viewModelScope.launch(Dispatchers.IO) {
                database.tabDao().deleteTab(TabEntry(removedTab.id, removedTab.url, removedTab.title, index))
            }
            webViewCache.remove(id)?.let { webView ->
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.clearCache(false)
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            }
            webViewStateCache.remove(id)
            blockedTrackersByTab.remove(id)
            tabLastActive.remove(id)

            if (tabs.isEmpty()) {
                createTab()
            } else if (_activeTabId.value == id) {
                _activeTabId.value = tabs[maxOf(0, index - 1)].id
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webViewCache.values.forEach { webView ->
            try {
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            } catch (e: Exception) {
                com.omniweb.app.util.LogUtils.e("Error destroying WebView in onCleared", e)
            }
        }
        webViewCache.clear()

        prewarmedWebView?.let { webView ->
            try {
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.loadUrl("about:blank")
                webView.removeAllViews()
                webView.destroy()
            } catch (e: Exception) {
                com.omniweb.app.util.LogUtils.e("Error destroying prewarmed WebView", e)
            }
            prewarmedWebView = null
        }

        webViewStateCache.clear()
        blockedTrackersByTab.clear()
        perSiteSettingsCache.clear()
        suggestionCache.clear()
    }

    fun selectTab(id: String) {
        _activeTabId.value = id
        tabLastActive[id] = System.currentTimeMillis()
        hibernateTabsIfNeeded()
    }

    fun toggleZenMode() {
        _isZenMode.value = !_isZenMode.value
    }

    fun toggleSplitScreen() {
        if (!_isSplitScreen.value) {
            val currentActiveId = _activeTabId.value
            val otherTab = tabs.find { it.id != currentActiveId }
            if (otherTab != null) {
                _splitTabId.value = otherTab.id
                _isSplitScreen.value = true
            } else {
                createTab()
                val newTabId = tabs.last().id
                _splitTabId.value = newTabId
                _isSplitScreen.value = true
            }
        } else {
            _isSplitScreen.value = false
            _splitTabId.value = null
        }
    }

    fun restoreLastClosedTab() {
        if (recentlyClosedTabs.isNotEmpty()) {
            val tab = recentlyClosedTabs.removeAt(0)
            restoreTab(tab)
        }
    }

    fun restoreTab(tab: TabInfo) {
        if (!tabs.any { it.id == tab.id }) {
            tabs.add(tab)
            recentlyClosedTabs.remove(tab)
            _activeTabId.value = tab.id
            saveTabToDb(tab)
        }
    }

    fun hibernateTabsIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val timeout = if (force) 0 else 30 * 1000 // 30 seconds
        val activeId = _activeTabId.value

        // Use a list to avoid ConcurrentModificationException if we were modifying the cache during iteration
        val tabsToHibernate = webViewCache.keys.filter { it != activeId }.filter { tabId ->
            val lastActive = tabLastActive[tabId] ?: 0L
            force || (now - lastActive > timeout)
        }

        tabsToHibernate.forEach { tabId ->
            webViewCache.remove(tabId)?.let { webView ->
                val state = android.os.Bundle()
                webView.saveState(state)
                webViewStateCache[tabId] = state

                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.clearCache(false) // Don't clear disk cache during hibernation
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            }
        }
    }

    fun updateSuggestions(query: String) {
        _searchQuery.value = query
    }

    fun getPerSiteSettings(host: String): PerSiteSettings? {
        return perSiteSettingsCache[host]
    }

    fun preloadPerSiteSettings(host: String) {
        if (host.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val settings = database.perSiteSettingsDao().getSettingsForHostSync(host)
            if (settings != null) {
                withContext(Dispatchers.Main) {
                    perSiteSettingsCache[host] = settings
                }
            }
        }
    }

    fun updatePerSiteSettings(settings: PerSiteSettings) {
        perSiteSettingsCache[settings.host] = settings
        viewModelScope.launch(Dispatchers.IO) {
            database.perSiteSettingsDao().insertSettings(settings)
        }
    }

    fun isAd(url: String): Boolean {
        if (!settings.value.adBlockEnabled) return false
        val uri = android.net.Uri.parse(url)
        val host = uri.host ?: return false
        return com.omniweb.app.util.AdBlockManager.shouldBlock(host) || bloomFilterAdBlocker.isAd(url)
    }

    fun getRedirect(url: String): String? {
        return redirectManager?.getRedirect(url)
    }

    fun getAnnotationsForUrl(url: String): Flow<List<AnnotationEntity>> {
        return database.annotationDao().getAnnotationsForUrl(url)
    }

    fun saveAnnotation(url: String, text: String, color: Int = 0xFFFFFF00.toInt()) {
        viewModelScope.launch(Dispatchers.IO) {
            database.annotationDao().insertAnnotation(AnnotationEntity(url = url, text = text, color = color))
        }
    }

    fun deleteAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.annotationDao().deleteAnnotation(annotation)
        }
    }

    suspend fun getAllSessions(): List<NamedSession> {
        return database.namedSessionDao().getAllSessions()
    }

    fun saveCurrentSession(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionTabs = tabs.map { tab ->
                NamedSessionTab(sessionName = name, url = tab.url, title = tab.title)
            }
            database.namedSessionDao().saveSession(name, sessionTabs)
        }
    }

    fun restoreSession(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionTabs = database.namedSessionDao().getTabsForSession(name)
            if (sessionTabs.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    sessionTabs.forEach { tab ->
                        createTab(url = tab.url, title = tab.title)
                    }
                }
            }
        }
    }

    fun deleteSession(session: NamedSession) {
        viewModelScope.launch(Dispatchers.IO) {
            database.namedSessionDao().deleteSession(session)
        }
    }

    fun speak(text: String) {
        accessibilityTools.speak(text)
    }

    fun stopSpeaking() {
        accessibilityTools.stop()
    }

    fun clearSiteData(host: String) {
        val cookieManager = android.webkit.CookieManager.getInstance()
        val webStorage = android.webkit.WebStorage.getInstance()

        val protocols = listOf("https://", "http://")

        protocols.forEach { protocol ->
            val url = protocol + host
            val cookies = cookieManager.getCookie(url)
            if (cookies != null) {
                val cookieArray = cookies.split(";")
                for (cookie in cookieArray) {
                    val parts = cookie.split("=")
                    if (parts.isNotEmpty()) {
                        cookieManager.setCookie(url, parts[0].trim() + "=; Max-Age=0")
                    }
                }
            }
            webStorage.deleteOrigin(url)
        }

        cookieManager.flush()
    }

    suspend fun chatWithPage(url: String, content: String, message: String, apiKey: String?): String {
        if (apiKey.isNullOrBlank()) return "Please set Gemini API key in Settings."
        return try {
            com.omniweb.app.util.PageUtils.generateSummary("Context: Website $url\nContent: $content\nQuestion: $message", apiKey)
        } catch (e: Exception) {
            "AI Error: ${e.message}"
        }
    }

    private suspend fun fetchSuggestionsInternal(query: String) = withContext(Dispatchers.IO) {
        val historyDeferred = async {
            (database.historyDao().getAllHistory().firstOrNull() ?: emptyList()).filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(5).map { Suggestion(it.title, it.url, isHistory = true) }
        }

        val bookmarksDeferred = async {
            (database.bookmarkDao().getAllBookmarks().firstOrNull() ?: emptyList()).filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(5).map { Suggestion(it.title, it.url, isHistory = false) }
        }

        val liveDeferred = async { fetchLiveSuggestions(query) }

        val history = historyDeferred.await()
        val bookmarks = bookmarksDeferred.await()
        val liveSuggestions = liveDeferred.await()

        withContext(Dispatchers.Main) {
            _searchSuggestions.value = (bookmarks + history + liveSuggestions).distinctBy { it.url }
        }
    }

    private suspend fun fetchLiveSuggestions(query: String): List<Suggestion> = withContext(Dispatchers.IO) {
        suggestionCache[query]?.let { return@withContext it }
        try {
            val engine = settings.value.searchEngine
            val baseUrl = when {
                engine.contains("google.com") -> "https://suggestqueries.google.com/complete/search?client=firefox&q="
                engine.contains("baidu.com") -> "https://suggestion.baidu.com/s?action=opensearch&wd="
                engine.contains("bing.com") -> "https://www.bing.com/osjson.aspx?query="
                engine.contains("ecosia.org") -> "https://ac.ecosia.org/autocomplete?q="
                else -> "https://duckduckgo.com/ac/?q="
            }
            val url = URL("$baseUrl${android.net.Uri.encode(query)}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val response = connection.inputStream.bufferedReader().readText()
            val suggestions = mutableListOf<Suggestion>()

            try {
                if (engine.contains("google.com") || engine.contains("baidu.com") || engine.contains("bing.com")) {
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() >= 2) {
                        val items = jsonArray.getJSONArray(1)
                        for (i in 0 until items.length()) {
                            val phrase = items.getString(i)
                            suggestions.add(Suggestion(phrase, phrase, isHistory = false))
                        }
                    }
                } else if (engine.contains("ecosia.org")) {
                    val jsonObject = org.json.JSONObject(response)
                    val suggestionsArray = jsonObject.getJSONArray("suggestions")
                    for (i in 0 until suggestionsArray.length()) {
                        val phrase = suggestionsArray.getString(i)
                        suggestions.add(Suggestion(phrase, phrase, isHistory = false))
                    }
                } else {
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val phrase = obj.optString("phrase", "")
                        if (phrase.isNotEmpty()) {
                            suggestions.add(Suggestion(phrase, phrase, isHistory = false))
                        }
                    }
                }
            } catch (e: Exception) {
                com.omniweb.app.util.LogUtils.e("Suggestion parsing failed", e)
            }
            suggestionCache[query] = suggestions
            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class Suggestion(val title: String, val url: String, val isHistory: Boolean)
