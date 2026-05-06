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
import org.json.JSONArray

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val tabs = mutableStateListOf<TabInfo>()
    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()
    private val webViewCache = mutableMapOf<String, WebView>()
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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

    fun updateTabScroll(tabId: String, x: Int, y: Int) {
        tabs.find { it.id == tabId }?.let { tab ->
            tab.scrollX = x
            tab.scrollY = y
            updateTabInDb(tab)
        }
    }

    fun getOrCreateWebView(tabId: String, context: android.content.Context): WebView {
        return webViewCache.getOrPut(tabId) {
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewStateCache[tabId]?.let { state ->
                    restoreState(state)
                    webViewStateCache.remove(tabId)
                }
            }
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

            viewModelScope.launch {
                database.tabDao().deleteTab(TabEntry(removedTab.id, removedTab.url, removedTab.title, index))
            }
            webViewCache.remove(id)?.let { webView ->
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
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
        tabLastActive[id] = System.currentTimeMillis()
        hibernateTabsIfNeeded()
    }

    fun restoreLastClosedTab() {
        if (recentlyClosedTabs.isNotEmpty()) {
            val tab = recentlyClosedTabs.removeAt(0)
            tabs.add(tab)
            _activeTabId.value = tab.id
            saveTabToDb(tab)
        }
    }

    private fun hibernateTabsIfNeeded() {
        val now = System.currentTimeMillis()
        val timeout = 2 * 60 * 1000 // 2 minutes
        tabs.forEach { tab ->
            if (tab.id != _activeTabId.value) {
                val lastActive = tabLastActive[tab.id] ?: 0L
                if (now - lastActive > timeout && webViewCache.containsKey(tab.id)) {
                    webViewCache.remove(tab.id)?.let { webView ->
                        val state = android.os.Bundle()
                        webView.saveState(state)
                        webViewStateCache[tab.id] = state
                        webView.stopLoading()
                        webView.webChromeClient = null
                        webView.webViewClient = WebViewClient()
                        webView.clearHistory()
                        webView.removeAllViews()
                        webView.destroy()
                    }
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
        viewModelScope.launch {
            val settings = database.perSiteSettingsDao().getSettingsForHostSync(host)
            if (settings != null) {
                perSiteSettingsCache[host] = settings
            }
        }
    }

    fun updatePerSiteSettings(settings: PerSiteSettings) {
        perSiteSettingsCache[settings.host] = settings
        viewModelScope.launch {
            database.perSiteSettingsDao().insertSettings(settings)
        }
    }

    private suspend fun fetchSuggestionsInternal(query: String) {
        val history = (database.historyDao().getAllHistory().firstOrNull() ?: emptyList()).filter {
            it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }.take(5).map { Suggestion(it.title, it.url, isHistory = true) }

        val bookmarks = (database.bookmarkDao().getAllBookmarks().firstOrNull() ?: emptyList()).filter {
            it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }.take(5).map { Suggestion(it.title, it.url, isHistory = false) }

        val liveSuggestions = fetchLiveSuggestions(query)

        _searchSuggestions.value = (bookmarks + history + liveSuggestions).distinctBy { it.url }
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
