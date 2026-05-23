package com.omniweb.app.ui

import android.app.Application
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

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val tabs = mutableStateListOf<TabInfo>()
    private val tabAccessOrder = LinkedHashMap<String, Long>(16, 0.75f, true)
    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()
    private val webViewCache = object : java.util.LinkedHashMap<String, WebView>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WebView>?): Boolean {
            if (size > 5) {
                eldest?.let { entry ->
                    val webView = entry.value
                    val state = android.os.Bundle()
                    webView.saveState(state)
                    webViewStateCache[entry.key] = state
                    webView.stopLoading()
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
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

    val blockedTrackersByTab = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()
    private val tabLastActive = mutableMapOf<String, Long>()
    private val perSiteSettingsCache = mutableMapOf<String, PerSiteSettings>()

    init {
        // Initialize with a default tab to avoid empty state; it will be replaced if saved tabs are restored.
        val initialId = UUID.randomUUID().toString()
        tabs.add(TabInfo(initialId, "about:home", "Home"))
        _activeTabId.value = initialId

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
        if (existing != null) return existing

        val webView = prewarmedWebView ?: createWebView(context)
        prewarmedWebView = null

        webView.apply {
            webViewStateCache[tabId]?.let { state ->
                restoreState(state)
                webViewStateCache.remove(tabId)
            }
        }
        webViewCache[tabId] = webView

        // Prepare next prewarmed WebView
        viewModelScope.launch(Dispatchers.Main) {
            if (prewarmedWebView == null) {
                prewarmedWebView = createWebView(context)
            }
        }

        return webView
    }

    private fun createWebView(context: android.content.Context): WebView {
        return WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val settings = database.settingsDao().getSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val _searchSuggestions = mutableStateOf<List<Suggestion>>(emptyList())
    val searchSuggestions get() = _searchSuggestions

    fun createTab(url: String = "about:home", title: String = "Home", isIncognito: Boolean = false) {
        val newTab = TabInfo(UUID.randomUUID().toString(), url, title, isIncognito)
        tabs.add(newTab)
        _activeTabId.value = newTab.id
        saveTabToDb(newTab)
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            val removedTab = tabs.removeAt(index)
            recentlyClosedTabs.add(0, removedTab)
            if (recentlyClosedTabs.size > 10) recentlyClosedTabs.removeLast()

            viewModelScope.launch(Dispatchers.IO) {
                database.tabDao().deleteTab(TabEntry(removedTab.id, removedTab.url, removedTab.title, index))
            }
            webViewCache.remove(id)?.let { webView ->
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.clearCache(true)
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
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
        webViewCache.clear()
        webViewStateCache.clear()
        blockedTrackersByTab.clear()
    }

    fun selectTab(id: String) {
        _activeTabId.value = id
        val now = System.currentTimeMillis()
        tabLastActive[id] = now
        tabAccessOrder[id] = now
        hibernateTabsIfNeeded()
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
        val timeout = if (force) 0 else 60 * 1000 // 60 seconds

        // Strategy: Hibernate if inactive for too long OR if we have too many active WebViews
        val activeWebViews = webViewCache.size
        val maxActive = 3

        val sortedTabs = tabs.filter { it.id != _activeTabId.value }
            .sortedBy { tabAccessOrder[it.id] ?: 0L }

        sortedTabs.forEachIndexed { index, tab ->
            val lastActive = tabLastActive[tab.id] ?: 0L
            val isTooOld = now - lastActive > timeout
            val isBeyondThreshold = (sortedTabs.size - index) > maxActive

            if ((force || isTooOld || isBeyondThreshold) && webViewCache.containsKey(tab.id)) {
                webViewCache.remove(tab.id)?.let { webView ->
                    val state = android.os.Bundle()
                    webView.saveState(state)
                    webViewStateCache[tab.id] = state
                    webView.stopLoading()
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                }
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
            val url = URL("https://duckduckgo.com/ac/?q=${android.net.Uri.encode(query)}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val response = connection.inputStream.bufferedReader().readText()
            val jsonArray = JSONArray(response)
            val suggestions = mutableListOf<Suggestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val phrase = obj.getString("phrase")
                suggestions.add(Suggestion(phrase, phrase, isHistory = false))
            }
            suggestionCache[query] = suggestions
            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class Suggestion(val title: String, val url: String, val isHistory: Boolean)
