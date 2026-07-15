package com.omniweb.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_list")
data class ReadingListEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val filePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String, // Hostname
    val username: String,
    val encryptedPassword: String,
    val iv: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "per_site_settings")
data class PerSiteSettings(
    @PrimaryKey val host: String,
    val desktopMode: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val zoomLevel: Float = 1.0f,
    val customUserAgent: String? = null
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
    val askDownloadLocation: Boolean = false,
    val restoreTabsOnStart: Boolean = true,
    val clearDataOnExit: Boolean = false,
    val javaScriptEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val httpsOnlyMode: Boolean = false,
    val deepDarkMode: Boolean = false,
    val strictPrivacyMode: Boolean = false,
    val geminiApiKey: String? = null,
    val customUserAgent: String? = null,
    val customSearchEngines: String? = null, // Stored as JSON: List<Pair<String, String>>
    val torEnabled: Boolean = false,
    val torProxyHost: String = "127.0.0.1",
    val torProxyPort: Int = 9050,
    val parentalPassword: String? = null,
    val blockedSites: String? = null, // JSON: List<String>
    val alwaysIncognito: Boolean = false,
    val textReflowEnabled: Boolean = false,
    val ampBlockingEnabled: Boolean = false,
    val invertPageEnabled: Boolean = false,
    val forceZoom: Boolean = false,
    val forceLightTheme: Boolean = false,
    val forceBlackTheme: Boolean = false,
    val readerFontSize: Float = 18f,
    val readerTheme: String = "system",
    val readerFontFamily: String = "serif",
    val toolbarLocation: String = "bottom",
    val hibernationTimeoutMillis: Long = 300000L,
    val maxWebViewCacheSize: Int = 5,
    val useGeckoView: Boolean = false,
    val firefoxUserId: String? = null,
    val firefoxCollectionName: String? = null
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
    val scrollY: Int = 0,
    val parentTabId: String? = null
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

@Entity(tableName = "named_sessions")
data class NamedSession(
    @PrimaryKey val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "named_session_tabs",
    foreignKeys = [
        ForeignKey(
            entity = NamedSession::class,
            parentColumns = ["name"],
            childColumns = ["sessionName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionName"])]
)
data class NamedSessionTab(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionName: String,
    val url: String,
    val title: String
)

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val text: String,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val color: Int = 0xFF57CC99.toInt() // Nature green default
)

@Entity(tableName = "custom_redirects")
data class CustomRedirectEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val target: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "rss_items")
data class RssItemEntity(
    @PrimaryKey val link: String,
    val title: String?,
    val pubDate: String?,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)
