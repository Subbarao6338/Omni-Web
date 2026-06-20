package com.nature.browser.db

import androidx.room.*

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
    ]
)
data class NamedSessionTab(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionName: String,
    val url: String,
    val title: String
)

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
