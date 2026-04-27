package com.omniweb.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Bookmark::class, HistoryEntry::class, Settings::class, DownloadTask::class, UserScript::class, Shortcut::class, TabEntry::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun downloadDao(): DownloadDao
    abstract fun userScriptDao(): UserScriptDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun tabDao(): TabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE userscripts ADD COLUMN type TEXT NOT NULL DEFAULT 'userscript'")
                database.execSQL("ALTER TABLE userscripts ADD COLUMN runAt TEXT NOT NULL DEFAULT 'end'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tabs ADD COLUMN scrollX INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tabs ADD COLUMN scrollY INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN clearDataOnExit INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omni_browser_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT OR IGNORE INTO settings (id, searchEngine, adBlockEnabled, themeMode, lastTabUrl, accentColor, darkMode, downloadPath, restoreTabsOnStart, clearDataOnExit) " +
                                "VALUES (0, 'https://www.google.com/search?q=', 1, 'system', 'about:home', '#3B82F6', 0, NULL, 1, 0)")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
