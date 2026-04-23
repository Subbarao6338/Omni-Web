package com.omniweb.app.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omniweb.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val tabs = mutableStateListOf(TabInfo(UUID.randomUUID().toString(), "about:home", "Home"))
    val activeTabId = mutableStateOf(tabs.first().id)

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
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            tabs.removeAt(index)
            if (tabs.isEmpty()) {
                createTab()
            } else if (activeTabId.value == id) {
                activeTabId.value = tabs[maxOf(0, index - 1)].id
            }
        }
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
            val history = database.historyDao().getAllHistory().first().filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(5).map { Suggestion(it.title, it.url, isHistory = true) }

            val bookmarks = database.bookmarkDao().getAllBookmarks().first().filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(5).map { Suggestion(it.title, it.url, isHistory = false) }

            _searchSuggestions.value = (bookmarks + history).distinctBy { it.url }
        }
    }
}

data class Suggestion(val title: String, val url: String, val isHistory: Boolean)
