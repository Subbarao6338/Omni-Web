package com.omniweb.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,
    val username: String,
    val password: String, // Encrypted
    val iv: String, // Initialization Vector
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0,
    val searchEngine: String = "https://www.google.com/search?q=",
    val adBlockEnabled: Boolean = true,
    val themeMode: String = "system",
    val lastTabUrl: String = "about:home",
    val accentColor: String = "#3B82F6",
    val darkMode: Boolean = false,
    val downloadPath: String? = null,
    val restoreTabsOnStart: Boolean = true,
    val clearDataOnExit: Boolean = false,
    val javaScriptEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val customUserAgent: String? = null,
    val customSearchEngines: String? = null
)

@Entity(tableName = "tabs")
data class TabEntry(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val position: Int,
    val isIncognito: Boolean = false,
    val lastActive: Long = System.currentTimeMillis(),
    val scrollX: Int = 0,
    val scrollY: Int = 0
)

@Entity(tableName = "downloads")
data class DownloadTask(
    @PrimaryKey val id: Long,
    val title: String,
    val url: String,
    val filePath: String?,
    val status: Int,
    val totalSize: Long,
    val downloadedSize: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeed: Long = 0,
    val estimatedTimeRemaining: Long = 0
)

@Entity(tableName = "userscripts")
data class UserScript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val script: String,
    val matchPattern: String = "*",
    val enabled: Boolean = true,
    val type: String = "userscript",
    val runAt: String = "end"
)

@Entity(tableName = "shortcuts")
data class Shortcut(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "per_site_settings")
data class PerSiteSettings(
    @PrimaryKey val host: String,
    val adBlockEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val javaScriptEnabled: Boolean = true
)
