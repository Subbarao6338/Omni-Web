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
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import org.json.JSONArray
import com.omniweb.app.BuildConfig
import com.omniweb.app.util.adblock.BloomFilterAdBlocker
import com.omniweb.app.util.AccessibilityTools

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val tabs = mutableStateListOf<TabInfo>()
    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()
    private val webViewCache = object : java.util.LinkedHashMap<String, WebView>(6, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WebView>?): Boolean {
            val limit = settings.value.maxWebViewCacheSize.coerceAtLeast(1)
            if (size > limit) {
                eldest?.let { entry ->
                    // Do not remove the active or split tab from cache
                    if (entry.key == _activeTabId.value || entry.key == _splitTabId.value) {
                        // If eldest is active, LinkedHashMap might not remove it easily,
                        // but since we access it (moveToFront), it shouldn't be eldest.
                        return false
                    }

                    val webView = entry.value
                    val state = android.os.Bundle()
                    webView.saveState(state)
                    webViewStateCache[entry.key] = state
                    destroyWebView(webView)
                    return true
                }
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

    var pendingNavigation = mutableStateOf<String?>(null)
        private set

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
                    TabInfo(entry.id, entry.url, entry.title, entry.isIncognito, entry.scrollX, entry.scrollY, entry.parentTabId)
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
                scrollY = tab.scrollY,
                parentTabId = tab.parentTabId
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
                scrollY = tab.scrollY,
                parentTabId = tab.parentTabId
            )
            database.tabDao().updateTab(entry)
        }
    }

    private val lastScrollUpdates = mutableMapOf<String, Long>()
    fun updateTabScroll(tabId: String, x: Int, y: Int) {
        tabs.find { it.id == tabId }?.let { tab ->
            tab.scrollX = x
            tab.scrollY = y

            val now = System.currentTimeMillis()
            val lastUpdate = lastScrollUpdates[tabId] ?: 0L
            if (now - lastUpdate > 5000) { // Throttled to every 5 seconds
                lastScrollUpdates[tabId] = now
                viewModelScope.launch(Dispatchers.IO) {
                    val entry = TabEntry(
                        id = tab.id,
                        url = tab.url,
                        title = tab.title,
                        position = tabs.indexOf(tab),
                        isIncognito = tab.isIncognito,
                        scrollX = tab.scrollX,
                        scrollY = tab.scrollY,
                        parentTabId = tab.parentTabId
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
            } ?: run {
                // If no saved state bundle, check if we have scroll info in TabInfo
                tabs.find { it.id == tabId }?.let { tab ->
                    if (tab.scrollX != 0 || tab.scrollY != 0) {
                        scrollTo(tab.scrollX, tab.scrollY)
                    }
                }
            }
        }
        webViewCache[tabId] = webView

        // Prepare next prewarmed WebView using applicationContext if memory is not low
        viewModelScope.launch(Dispatchers.Main) {
            val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            if (prewarmedWebView == null && !memoryInfo.lowMemory) {
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

    fun createTab(url: String = "about:home", title: String = "Home", isIncognito: Boolean = false, parentTabId: String? = null) {
        val finalIncognito = isIncognito || settings.value.alwaysIncognito
        val newTab = TabInfo(UUID.randomUUID().toString(), url, title, finalIncognito, parentTabId = parentTabId)
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
                destroyWebView(webView)
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
                destroyWebView(webView)
            } catch (e: Exception) {
                com.omniweb.app.util.LogUtils.e("Error destroying WebView in onCleared", e)
            }
        }
        webViewCache.clear()

        prewarmedWebView?.let { webView ->
            try {
                destroyWebView(webView)
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

        // Ensure the selected tab is moved to the front of the cache
        webViewCache[id]?.let {
            webViewCache.remove(id)
            webViewCache[id] = it
        }

        hibernateTabsIfNeeded()
    }

    fun toggleZenMode() {
        _isZenMode.value = !_isZenMode.value
    }

    fun handleIntent(intent: android.content.Intent?) {
        when (intent?.action) {
            android.content.Intent.ACTION_VIEW -> {
                val url = intent.dataString
                if (url != null) {
                    createTab(url)
                    pendingNavigation.value = "browser"
                }
            }
            android.content.Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        createTab(sharedText)
                        pendingNavigation.value = "browser"
                    }
                }
            }
        }
    }

    fun onNavigationHandled() {
        pendingNavigation.value = null
    }

    fun toggleSplitScreen() {
        if (!_isSplitScreen.value) {
            val currentActiveId = _activeTabId.value
            val otherTab = tabs.find { it.id != currentActiveId }
            if (otherTab != null) {
                _splitTabId.value = otherTab.id
                _isSplitScreen.value = true

                // Ensure split tab is also moved to front of cache
                webViewCache[otherTab.id]?.let {
                    webViewCache.remove(otherTab.id)
                    webViewCache[otherTab.id] = it
                }
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

    fun hibernateTabsIfNeeded(force: Boolean = false, isCritical: Boolean = false) {
        val now = System.currentTimeMillis()
        val activeId = _activeTabId.value
        val splitId = _splitTabId.value

        val activityManager = getApplication<Application>().getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val availablePercent = if (memoryInfo.totalMem > 0) memoryInfo.availMem.toFloat() / memoryInfo.totalMem else 1f
        val isCriticalMemory = memoryInfo.lowMemory || availablePercent < 0.15f || isCritical

        // More aggressive timeout if memory is low
        val timeout = when {
            isCriticalMemory -> 0L
            force -> 60 * 1000L // 1 minute if forced but not critical
            else -> 5 * 60 * 1000L // 5 minutes
        }

        val maxCacheSize = when {
            isCriticalMemory -> 1
            availablePercent < 0.25f -> 3
            else -> 5
        }

        // Use a list to avoid ConcurrentModificationException
        val tabsToHibernate = webViewCache.keys.filter { it != activeId && it != splitId }.toMutableList()

        val filteredToHibernate = tabsToHibernate.filter { tabId ->
            val lastActive = tabLastActive[tabId] ?: 0L
            force || (now - lastActive > timeout) || memoryInfo.lowMemory || (webViewCache.size > maxCacheSize)
        }

        filteredToHibernate.forEach { tabId ->
            webViewCache.remove(tabId)?.let { webView ->
                val state = android.os.Bundle()
                webView.saveState(state)
                webViewStateCache[tabId] = state
                destroyWebView(webView)
            }
        }

        if (memoryInfo.lowMemory || force) {
            prewarmedWebView?.let {
                destroyWebView(it)
                prewarmedWebView = null
            }
            suggestionCache.clear()
            webViewCache.clear()
        }
    }

    private fun destroyWebView(webView: WebView) {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        webView.clearHistory()
        webView.clearCache(true)
        webView.removeAllViews()
        webView.destroy()
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
        return com.omniweb.app.util.AdBlockManager.shouldBlock(host)
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
        return com.omniweb.app.util.PageUtils.chatWithPage(url, content, message, apiKey)
    }

    fun translatePage(tabId: String, targetLanguage: String) {
        val webView = webViewCache[tabId] ?: return
        val url = webView.url ?: return
        val translateUrl = "https://translate.google.com/translate?sl=auto&tl=$targetLanguage&u=${android.net.Uri.encode(url)}"
        webView.loadUrl(translateUrl)
    }

    private suspend fun fetchSuggestionsInternal(query: String) = withContext(Dispatchers.IO) {
        val historyDeferred = async {
            database.historyDao().searchHistory(query, 5).map { Suggestion(it.title, it.url, isHistory = true) }
        }

        val bookmarksDeferred = async {
            database.bookmarkDao().searchBookmarks(query, 5).map { Suggestion(it.title, it.url, isHistory = false) }
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
                engine.contains("brave.com") -> "https://search.brave.com/api/suggest?q="
                else -> "https://duckduckgo.com/ac/?q="
            }

            val url = "$baseUrl${android.net.Uri.encode(query)}"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute().use { it.body?.string() } ?: return@withContext emptyList()
            val suggestions = mutableListOf<Suggestion>()

            try {
                if (response.trim().startsWith("[")) {
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() >= 2 && jsonArray.get(1) is JSONArray) {
                        val items = jsonArray.getJSONArray(1)
                        for (i in 0 until items.length()) {
                            val phrase = items.optString(i)
                            if (phrase.isNotEmpty()) {
                                suggestions.add(Suggestion(phrase, phrase, isHistory = false))
                            }
                        }
                    } else {
                        // Fallback for flat arrays
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.opt(i)
                            if (item is String) {
                                suggestions.add(Suggestion(item, item, isHistory = false))
                            } else if (item is org.json.JSONObject) {
                                val phrase = item.optString("phrase", "")
                                if (phrase.isNotEmpty()) {
                                    suggestions.add(Suggestion(phrase, phrase, isHistory = false))
                                }
                            }
                        }
                    }
                } else if (response.trim().startsWith("{")) {
                    val jsonObject = org.json.JSONObject(response)
                    val suggestionsArray = jsonObject.optJSONArray("suggestions")
                    if (suggestionsArray != null) {
                        for (i in 0 until suggestionsArray.length()) {
                            val phrase = suggestionsArray.optString(i)
                            if (phrase.isNotEmpty()) {
                                suggestions.add(Suggestion(phrase, phrase, isHistory = false))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                com.omniweb.app.util.LogUtils.e("Suggestion parsing failed for engine $engine", e)
            }
            suggestionCache[query] = suggestions
            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class Suggestion(val title: String, val url: String, val isHistory: Boolean)
