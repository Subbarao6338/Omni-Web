package com.nature.browser

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import android.util.Base64
import com.nature.browser.ui.theme.AppTheme
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession

data class Bookmark(
    val title: String,
    val url: String,
    val isFolder: Boolean = false,
    val parentId: String? = null,
    val id: String = java.util.UUID.randomUUID().toString(),
    val tags: List<String> = emptyList()
)
data class HistoryItem(val title: String, val url: String, val timestamp: Long = System.currentTimeMillis())
data class ReadingListItem(val title: String, val url: String, val timestamp: Long = System.currentTimeMillis(), val savedPath: String? = null)

class BrowserStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

    // In-memory index for fast suggestions
    private var historyIndex: List<HistoryItem> = emptyList()
    private var bookmarksIndex: List<Bookmark> = emptyList()

    init {
        // Initial load into memory
        historyIndex = getHistory()
        bookmarksIndex = getBookmarks()
    }

    // Settings
    var homepage: String
        get() = prefs.getString("settings_homepage", "https://www.google.com") ?: "https://www.google.com"
        set(value) = prefs.edit().putString("settings_homepage", value).apply()

    var searchEngine: String
        get() = prefs.getString("settings_search_engine", "https://www.google.com/search?q=") ?: "https://www.google.com/search?q="
        set(value) = prefs.edit().putString("settings_search_engine", value).apply()

    fun getSearchEngines(): List<Pair<String, String>> {
        val json = prefs.getString("settings_search_engines_list", null) ?: return listOf(
            "Google" to "https://www.google.com/search?q=",
            "DuckDuckGo" to "https://duckduckgo.com/?q=",
            "Bing" to "https://www.bing.com/search?q="
        )
        val list = mutableListOf<Pair<String, String>>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(obj.getString("name") to obj.getString("url"))
        }
        return list
    }

    fun addSearchEngine(name: String, url: String) {
        val engines = getSearchEngines().toMutableList()
        engines.add(name to url)
        saveSearchEngines(engines)
    }

    private fun saveSearchEngines(list: List<Pair<String, String>>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("name", it.first)
            obj.put("url", it.second)
            array.put(obj)
        }
        prefs.edit().putString("settings_search_engines_list", array.toString()).apply()
    }

    var isAdBlockEnabled: Boolean
        get() = prefs.getBoolean("settings_adblock", true)
        set(value) = prefs.edit().putBoolean("settings_adblock", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("settings_darkmode", false)
        set(value) = prefs.edit().putBoolean("settings_darkmode", value).apply()

    var isDesktopMode: Boolean
        get() = prefs.getBoolean("settings_desktopmode", false)
        set(value) = prefs.edit().putBoolean("settings_desktopmode", value).apply()

    var appTheme: AppTheme
        get() = try {
            AppTheme.valueOf(prefs.getString("settings_theme", AppTheme.Default.name) ?: AppTheme.Default.name)
        } catch (e: Exception) {
            AppTheme.Default
        }
        set(value) = prefs.edit().putString("settings_theme", value.name).apply()

    // Bookmarks
    fun getBookmarks(): List<Bookmark> {
        val json = prefs.getString("bookmarks_v3", null) ?: return emptyList()
        val list = mutableListOf<Bookmark>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val tags = mutableListOf<String>()
            val tagsArray = obj.optJSONArray("tags")
            if (tagsArray != null) {
                for (j in 0 until tagsArray.length()) tags.add(tagsArray.getString(j))
            }
            list.add(Bookmark(
                obj.getString("title"),
                obj.getString("url"),
                obj.getBoolean("isFolder"),
                if (obj.has("parentId") && !obj.isNull("parentId")) obj.getString("parentId") else null,
                obj.getString("id"),
                tags
            ))
        }
        return list
    }

    fun saveBookmark(bookmark: Bookmark) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.add(bookmark)
        saveBookmarksList(bookmarks)
        bookmarksIndex = bookmarks
    }

    private fun saveBookmarksList(list: List<Bookmark>) {
        val array = JSONArray()
        list.forEach { b ->
            val obj = JSONObject()
            obj.put("title", b.title)
            obj.put("url", b.url)
            obj.put("isFolder", b.isFolder)
            obj.put("parentId", b.parentId)
            obj.put("id", b.id)
            val tagsArray = JSONArray()
            b.tags.forEach { tagsArray.put(it) }
            obj.put("tags", tagsArray)
            array.put(obj)
        }
        prefs.edit().putString("bookmarks_v3", array.toString()).apply()
    }

    // History with Search
    fun addHistory(title: String, url: String) {
        val history = getHistory().toMutableList()
        history.removeAll { it.url == url }
        history.add(0, HistoryItem(if (title.isEmpty()) url else title, url))
        val list = if (history.size > 1000) history.take(1000) else history
        saveHistoryList(list)
        historyIndex = list
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history_v2", null) ?: return emptyList()
        val list = mutableListOf<HistoryItem>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(HistoryItem(obj.getString("title"), obj.getString("url"), obj.getLong("timestamp")))
        }
        return list
    }

    fun searchHistory(query: String): List<HistoryItem> {
        // Fast search using in-memory index
        return historyIndex.filter { it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
    }

    private fun saveHistoryList(list: List<HistoryItem>) {
        val array = JSONArray()
        list.forEach { h ->
            val obj = JSONObject()
            obj.put("title", h.title)
            obj.put("url", h.url)
            obj.put("timestamp", h.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("history_v2", array.toString()).apply()
    }

    // Reading List
    fun getReadingList(): List<ReadingListItem> {
        val json = prefs.getString("reading_list", null) ?: return emptyList()
        val list = mutableListOf<ReadingListItem>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(ReadingListItem(obj.getString("title"), obj.getString("url"), obj.getLong("timestamp"), obj.optString("savedPath")))
        }
        return list
    }

    fun addToReadingList(item: ReadingListItem) {
        val list = getReadingList().toMutableList()
        list.add(0, item)
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("timestamp", it.timestamp)
            obj.put("savedPath", it.savedPath)
            array.put(obj)
        }
        prefs.edit().putString("reading_list", array.toString()).apply()
    }

    // Tab Persistence
    fun saveTabs(tabsJson: String) {
        prefs.edit().putString("saved_tabs", tabsJson).apply()
    }

    fun getSavedTabs(): String? {
        return prefs.getString("saved_tabs", null)
    }

    fun saveActiveTabId(id: String) {
        prefs.edit().putString("active_tab_id", id).apply()
    }

    fun getActiveTabId(): String? {
        return prefs.getString("active_tab_id", null)
    }

    fun serializeState(state: GeckoSession.SessionState): String {
        val parcel = Parcel.obtain()
        state.writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.recycle()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun deserializeState(encoded: String): GeckoSession.SessionState? {
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val parcel = Parcel.obtain()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val state = GeckoSession.SessionState.CREATOR.createFromParcel(parcel)
            parcel.recycle()
            state
        } catch (e: Exception) {
            null
        }
    }
}
