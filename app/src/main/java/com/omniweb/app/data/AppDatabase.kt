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
    entities = [Bookmark::class, HistoryEntry::class, Settings::class, DownloadTask::class, UserScript::class, Shortcut::class, TabEntry::class, PasswordEntry::class, PerSiteSettings::class],
    version = 11,
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
    abstract fun passwordDao(): PasswordDao
    abstract fun perSiteSettingsDao(): PerSiteSettingsDao

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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN javaScriptEnabled INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN blockThirdPartyCookies INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN customUserAgent TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN customSearchEngines TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `passwords` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `site` TEXT NOT NULL, `username` TEXT NOT NULL, `password` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `per_site_settings` (`host` TEXT NOT NULL, `desktopMode` INTEGER NOT NULL DEFAULT 0, `adBlockEnabled` INTEGER NOT NULL DEFAULT 1, `javaScriptEnabled` INTEGER NOT NULL DEFAULT 1, `zoomLevel` REAL NOT NULL DEFAULT 1.0, PRIMARY KEY(`host`))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omni_browser_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT OR IGNORE INTO settings (id, searchEngine, adBlockEnabled, themeMode, lastTabUrl, accentColor, darkMode, downloadPath, restoreTabsOnStart, clearDataOnExit, javaScriptEnabled, blockThirdPartyCookies, customUserAgent, customSearchEngines) " +
                                "VALUES (0, 'https://www.google.com/search?q=', 1, 'system', 'about:home', '#3B82F6', 0, NULL, 1, 0, 1, 1, NULL, NULL)")
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
