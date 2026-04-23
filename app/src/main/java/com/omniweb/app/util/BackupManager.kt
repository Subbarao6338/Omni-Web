package com.omniweb.app.util

import com.omniweb.app.data.Bookmark
import com.omniweb.app.data.Settings
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    fun exportData(bookmarks: List<Bookmark>, settings: Settings): String {
        val root = JSONObject()

        val bookmarksArray = JSONArray()
        bookmarks.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            bookmarksArray.put(obj)
        }
        root.put("bookmarks", bookmarksArray)

        val settingsObj = JSONObject()
        settingsObj.put("searchEngine", settings.searchEngine)
        settingsObj.put("adBlockEnabled", settings.adBlockEnabled)
        settingsObj.put("themeMode", settings.themeMode)
        settingsObj.put("accentColor", settings.accentColor)
        root.put("settings", settingsObj)

        return root.toString(4)
    }

    fun importBookmarks(json: String): List<Bookmark> {
        val list = mutableListOf<Bookmark>()
        val root = JSONObject(json)
        if (root.has("bookmarks")) {
            val array = root.getJSONArray("bookmarks")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(Bookmark(title = obj.getString("title"), url = obj.getString("url")))
            }
        }
        return list
    }

    fun importSettings(json: String, currentSettings: Settings): Settings {
        val root = JSONObject(json)
        if (root.has("settings")) {
            val obj = root.getJSONObject("settings")
            return currentSettings.copy(
                searchEngine = obj.optString("searchEngine", currentSettings.searchEngine),
                adBlockEnabled = obj.optBoolean("adBlockEnabled", currentSettings.adBlockEnabled),
                themeMode = obj.optString("themeMode", currentSettings.themeMode),
                accentColor = obj.optString("accentColor", currentSettings.accentColor)
            )
        }
        return currentSettings
    }
}
