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

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0, // Only one row
    val searchEngine: String = "https://www.google.com/search?q=",
    val adBlockEnabled: Boolean = true,
    val themeMode: String = "system", // "light", "dark", "system"
    val lastTabUrl: String = "about:home",
    val accentColor: String = "#3B82F6",
    val darkMode: Boolean = false, // Deprecated but kept for migration if needed
    val downloadPath: String? = null,
    val restoreTabsOnStart: Boolean = true
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
    @PrimaryKey val id: Long, // Use the ID from Android DownloadManager
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
    val matchPattern: String = "*", // Glob pattern for URLs
    val enabled: Boolean = true,
    val type: String = "userscript", // "userscript" or "bookmarklet"
    val runAt: String = "end" // "start" or "end"
)

@Entity(tableName = "shortcuts")
data class Shortcut(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)
