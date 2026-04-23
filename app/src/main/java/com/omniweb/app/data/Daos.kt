package com.omniweb.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)
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
}

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
