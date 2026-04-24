package com.omniweb.app.ui

import android.app.Application
import android.webkit.WebView
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
    val activeTabId = mutableStateOf("")
    private val webViewCache = mutableMapOf<String, WebView>()

    init {
        // Start with a placeholder if empty to prevent crash in UI before init completes
        if (tabs.isEmpty()) {
            tabs.add(TabInfo("loading", "about:blank", "Loading..."))
            activeTabId.value = "loading"
        }

        viewModelScope.launch {
            val currentSettings = database.settingsDao().getSettings().firstOrNull() ?: Settings()
            val savedTabs = database.tabDao().getAllTabs().firstOrNull() ?: emptyList()

            tabs.clear()

            if (currentSettings.restoreTabsOnStart && savedTabs.isNotEmpty()) {
                savedTabs.forEach { entry ->
                    tabs.add(TabInfo(entry.id, entry.url, entry.title, entry.isIncognito))
                }
                activeTabId.value = tabs.first().id
            } else {
                if (!currentSettings.restoreTabsOnStart) {
                    database.tabDao().clearAllTabs()
                }
                createDefaultTab()
            }
        }
    }

    private fun createDefaultTab() {
        val id = UUID.randomUUID().toString()
        val newTab = TabInfo(id, "about:home", "Home")
        tabs.add(newTab)
        activeTabId.value = id
        saveTabToDb(newTab)
    }

    private fun saveTabToDb(tab: TabInfo) {
        viewModelScope.launch {
            database.tabDao().insertTab(TabEntry(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                position = tabs.indexOf(tab),
                isIncognito = tab.isIncognito
            ))
        }
    }

    fun updateTabInDb(tab: TabInfo) {
        viewModelScope.launch {
            database.tabDao().updateTab(TabEntry(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                position = tabs.indexOf(tab),
                isIncognito = tab.isIncognito
            ))
        }
    }

    fun getOrCreateWebView(tabId: String, context: android.content.Context): WebView {
        return webViewCache.getOrPut(tabId) {
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
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
        activeTabId.value = newTab.id
        saveTabToDb(newTab)
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            val removedTab = tabs.removeAt(index)
            viewModelScope.launch {
                database.tabDao().deleteTab(TabEntry(removedTab.id, removedTab.url, removedTab.title, index))
            }
            webViewCache.remove(id)?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            if (tabs.isEmpty()) {
                createTab()
            } else if (activeTabId.value == id) {
                activeTabId.value = tabs[maxOf(0, index - 1)].id
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webViewCache.values.forEach {
            it.stopLoading()
            it.loadUrl("about:blank")
            it.clearHistory()
            it.removeAllViews()
            it.destroy()
        }
        webViewCache.clear()
    }

    fun selectTab(id: String) {
        activeTabId.value = id
    }

    fun updateSuggestions(query: String) {
        if (query.isBlank()) {
            _searchSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            val history = (database.historyDao().getAllHistory().firstOrNull() ?: emptyList()).filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(5).map { Suggestion(it.title, it.url, isHistory = true) }

            val bookmarks = (database.bookmarkDao().getAllBookmarks().firstOrNull() ?: emptyList()).filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(5).map { Suggestion(it.title, it.url, isHistory = false) }

            val liveSuggestions = fetchLiveSuggestions(query)

            _searchSuggestions.value = (bookmarks + history + liveSuggestions).distinctBy { it.url }
        }
    }

    private suspend fun fetchLiveSuggestions(query: String): List<Suggestion> = withContext(Dispatchers.IO) {
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
            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class Suggestion(val title: String, val url: String, val isHistory: Boolean)
