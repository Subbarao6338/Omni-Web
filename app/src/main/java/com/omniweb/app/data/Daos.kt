package com.omniweb.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<Bookmark>)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchBookmarks(query: String, limit: Int): List<Bookmark>
}

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY position ASC")
    fun getAllTabs(): Flow<List<TabEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntry)

    @Update
    suspend fun updateTab(tab: TabEntry)

    @Delete
    suspend fun deleteTab(tab: TabEntry)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Delete
    suspend fun deleteHistoryEntry(entry: HistoryEntry)

    @Query("SELECT title, url, COUNT(url) as visitCount FROM history GROUP BY url ORDER BY visitCount DESC LIMIT :limit")
    fun getMostVisited(limit: Int = 8): Flow<List<MostVisitedEntry>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchHistory(query: String, limit: Int): List<HistoryEntry>
}

data class MostVisitedEntry(
    val title: String,
    val url: String,
    val visitCount: Int
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun getSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: Settings)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadTask>>

    @Delete
    suspend fun deleteDownload(task: DownloadTask)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadByIdSync(id: Long): DownloadTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(task: DownloadTask)

    @Update
    suspend fun updateDownload(task: DownloadTask)

    @Query("DELETE FROM downloads WHERE status = 8")
    suspend fun deleteFinishedDownloads()
}

@Dao
interface UserScriptDao {
    @Query("SELECT * FROM userscripts")
    fun getAllScripts(): Flow<List<UserScript>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: UserScript)

    @Delete
    suspend fun deleteScript(script: UserScript)
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY timestamp ASC")
    fun getAllShortcuts(): Flow<List<Shortcut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: Shortcut)

    @Delete
    suspend fun deleteShortcut(shortcut: Shortcut)
}

@Dao
interface PerSiteSettingsDao {
    @Query("SELECT * FROM per_site_settings WHERE host = :host")
    fun getSettingsForHost(host: String): Flow<PerSiteSettings?>

    @Query("SELECT * FROM per_site_settings WHERE host = :host")
    suspend fun getSettingsForHostSync(host: String): PerSiteSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: PerSiteSettings)

    @Query("DELETE FROM per_site_settings WHERE host = :host")
    suspend fun deleteSettingsForHost(host: String)
}

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY timestamp DESC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM passwords WHERE site = :site")
    suspend fun getPasswordsForSite(site: String): List<PasswordEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: PasswordEntry)

    @Delete
    suspend fun deletePassword(entry: PasswordEntry)

    @Query("DELETE FROM passwords")
    suspend fun clearAllPasswords()
}

@Dao
interface NamedSessionDao {
    @Query("SELECT * FROM named_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<NamedSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: NamedSession)

    @Insert
    suspend fun insertTabs(tabs: List<NamedSessionTab>)

    @Query("SELECT * FROM named_session_tabs WHERE sessionName = :name")
    suspend fun getTabsForSession(name: String): List<NamedSessionTab>

    @Transaction
    suspend fun saveSession(name: String, tabs: List<NamedSessionTab>) {
        insertSession(NamedSession(name))
        insertTabs(tabs)
    }

    @Delete
    suspend fun deleteSession(session: NamedSession)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE url = :url ORDER BY timestamp DESC")
    fun getAnnotationsForUrl(url: String): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)
}

@Dao
interface CustomRedirectDao {
    @Query("SELECT * FROM custom_redirects")
    fun getAllRedirects(): Flow<List<CustomRedirectEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedirect(redirect: CustomRedirectEntry)

    @Delete
    suspend fun deleteRedirect(redirect: CustomRedirectEntry)
}

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<ReadingListEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(readingListEntry: ReadingListEntry)

    @Delete
    suspend fun deleteEntry(readingListEntry: ReadingListEntry)

    @Query("DELETE FROM reading_list")
    suspend fun clearAll()
}

@Dao
interface RssItemDao {
    @Query("SELECT * FROM rss_items ORDER BY timestamp DESC")
    fun getAllRssItems(): Flow<List<RssItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRssItems(items: List<RssItemEntity>)

    @Query("DELETE FROM rss_items")
    suspend fun clearAllRssItems()
}
