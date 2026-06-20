package com.nature.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationStack {
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var isNavigatingInternally = false

    fun push(url: String) {
        if (isNavigatingInternally) return
        if (url.isEmpty() || url == "about:blank" || url.startsWith("javascript:")) return

        val currentHistory = _history.value.toMutableList()
        val nextIndex = _currentIndex.value + 1

        // Truncate forward history
        if (nextIndex < currentHistory.size) {
            _history.value = currentHistory.subList(0, nextIndex)
        }

        // Avoid redundant consecutive entries
        if (_history.value.lastOrNull() == url) {
            _currentIndex.value = _history.value.size - 1
            return
        }

        _history.value = _history.value + url
        _currentIndex.value = _history.value.size - 1
    }

    fun sync(historyList: org.mozilla.geckoview.GeckoSession.HistoryDelegate.HistoryList) {
        val newHistory = (0 until historyList.size).map { historyList[it].uri }
        if (_history.value != newHistory || _currentIndex.value != historyList.currentIndex) {
            _history.value = newHistory
            _currentIndex.value = historyList.currentIndex
        }
    }

    fun canGoBack(): Boolean = _currentIndex.value > 0

    fun canGoForward(): Boolean = _currentIndex.value < _history.value.size - 1

    fun goBack(): String? {
        if (canGoBack()) {
            _currentIndex.value -= 1
            return _history.value[_currentIndex.value]
        }
        return null
    }

    fun goForward(): String? {
        if (canGoForward()) {
            _currentIndex.value += 1
            return _history.value[_currentIndex.value]
        }
        return null
    }

    fun clear() {
        _history.value = emptyList()
        _currentIndex.value = -1
    }
}
