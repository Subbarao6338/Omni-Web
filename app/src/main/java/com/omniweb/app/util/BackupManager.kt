package com.omniweb.app.util

import com.omniweb.app.data.*
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    fun exportData(
        bookmarks: List<Bookmark>,
        shortcuts: List<Shortcut>,
        history: List<HistoryEntry>,
        scripts: List<UserScript>,
        perSiteSettings: List<PerSiteSettings>,
        settings: Settings
    ): String {
        val root = JSONObject()

        val bookmarksArray = JSONArray()
        bookmarks.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            bookmarksArray.put(obj)
        }
        root.put("bookmarks", bookmarksArray)

        val shortcutsArray = JSONArray()
        shortcuts.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            shortcutsArray.put(obj)
        }
        root.put("shortcuts", shortcutsArray)

        val historyArray = JSONArray()
        history.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("timestamp", it.timestamp)
            historyArray.put(obj)
        }
        root.put("history", historyArray)

        val scriptsArray = JSONArray()
        scripts.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("script", it.script)
            obj.put("type", it.type)
            obj.put("enabled", it.enabled)
            obj.put("matchPattern", it.matchPattern)
            scriptsArray.put(obj)
        }
        root.put("scripts", scriptsArray)

        val pssArray = JSONArray()
        perSiteSettings.forEach {
            val obj = JSONObject()
            obj.put("host", it.host)
            obj.put("desktopMode", it.desktopMode)
            obj.put("adBlockEnabled", it.adBlockEnabled)
            obj.put("javaScriptEnabled", it.javaScriptEnabled)
            obj.put("zoomLevel", it.zoomLevel.toDouble())
            pssArray.put(obj)
        }
        root.put("perSiteSettings", pssArray)

        val settingsObj = JSONObject()
        settingsObj.put("searchEngine", settings.searchEngine)
        settingsObj.put("adBlockEnabled", settings.adBlockEnabled)
        settingsObj.put("themeMode", settings.themeMode)
        settingsObj.put("accentColor", settings.accentColor)
        settingsObj.put("torEnabled", settings.torEnabled)
        settingsObj.put("httpsOnlyMode", settings.httpsOnlyMode)
        settingsObj.put("strictPrivacyMode", settings.strictPrivacyMode)
        settingsObj.put("customSearchEngines", settings.customSearchEngines)
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

    fun importShortcuts(json: String): List<Shortcut> {
        val list = mutableListOf<Shortcut>()
        val root = JSONObject(json)
        if (root.has("shortcuts")) {
            val array = root.getJSONArray("shortcuts")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(Shortcut(title = obj.getString("title"), url = obj.getString("url")))
            }
        }
        return list
    }

    fun importHistory(json: String): List<HistoryEntry> {
        val list = mutableListOf<HistoryEntry>()
        val root = JSONObject(json)
        if (root.has("history")) {
            val array = root.getJSONArray("history")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(HistoryEntry(title = obj.getString("title"), url = obj.getString("url"), timestamp = obj.optLong("timestamp", System.currentTimeMillis())))
            }
        }
        return list
    }

    fun importScripts(json: String): List<UserScript> {
        val list = mutableListOf<UserScript>()
        val root = JSONObject(json)
        if (root.has("scripts")) {
            val array = root.getJSONArray("scripts")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(UserScript(
                    name = obj.getString("name"),
                    script = obj.getString("script"),
                    type = obj.optString("type", "userscript"),
                    enabled = obj.optBoolean("enabled", true),
                    matchPattern = obj.optString("matchPattern", "*")
                ))
            }
        }
        return list
    }

    fun importPerSiteSettings(json: String): List<PerSiteSettings> {
        val list = mutableListOf<PerSiteSettings>()
        val root = JSONObject(json)
        if (root.has("perSiteSettings")) {
            val array = root.getJSONArray("perSiteSettings")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(PerSiteSettings(
                    host = obj.getString("host"),
                    desktopMode = obj.optBoolean("desktopMode", false),
                    adBlockEnabled = obj.optBoolean("adBlockEnabled", true),
                    javaScriptEnabled = obj.optBoolean("javaScriptEnabled", true),
                    zoomLevel = obj.optDouble("zoomLevel", 1.0).toFloat()
                ))
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
                accentColor = obj.optString("accentColor", currentSettings.accentColor),
                torEnabled = obj.optBoolean("torEnabled", currentSettings.torEnabled),
                httpsOnlyMode = obj.optBoolean("httpsOnlyMode", currentSettings.httpsOnlyMode),
                strictPrivacyMode = obj.optBoolean("strictPrivacyMode", currentSettings.strictPrivacyMode),
                customSearchEngines = if (obj.has("customSearchEngines")) obj.getString("customSearchEngines") else currentSettings.customSearchEngines
            )
        }
        return currentSettings
    }
}
