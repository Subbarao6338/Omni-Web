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
    val darkMode: Boolean = false,
    val lastTabUrl: String = "about:home",
    val geminiApiKey: String = ""
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
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "userscripts")
data class UserScript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val script: String,
    val matchPattern: String = "*", // Glob pattern for URLs
    val enabled: Boolean = true
)

@Entity(tableName = "shortcuts")
data class Shortcut(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)
