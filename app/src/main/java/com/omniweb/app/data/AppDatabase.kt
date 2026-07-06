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
    entities = [Bookmark::class, HistoryEntry::class, Settings::class, DownloadTask::class, UserScript::class, Shortcut::class, TabEntry::class, PasswordEntry::class, PerSiteSettings::class, ReadingListEntry::class, NamedSession::class, NamedSessionTab::class, AnnotationEntity::class, CustomRedirectEntry::class, RssItemEntity::class],
    version = 25,
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
    abstract fun readingListDao(): ReadingListDao
    abstract fun namedSessionDao(): NamedSessionDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun customRedirectDao(): CustomRedirectDao
    abstract fun rssItemDao(): RssItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE userscripts ADD COLUMN type TEXT NOT NULL DEFAULT 'userscript'")
                db.execSQL("ALTER TABLE userscripts ADD COLUMN runAt TEXT NOT NULL DEFAULT 'end'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tabs ADD COLUMN scrollX INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tabs ADD COLUMN scrollY INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN clearDataOnExit INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN javaScriptEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN blockThirdPartyCookies INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN customUserAgent TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN customSearchEngines TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `passwords` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `site` TEXT NOT NULL, `username` TEXT NOT NULL, `password` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `per_site_settings` (`host` TEXT NOT NULL, `desktopMode` INTEGER NOT NULL DEFAULT 0, `adBlockEnabled` INTEGER NOT NULL DEFAULT 1, `javaScriptEnabled` INTEGER NOT NULL DEFAULT 1, `zoomLevel` REAL NOT NULL DEFAULT 1.0, PRIMARY KEY(`host`))")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reading_list` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `filePath` TEXT, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `passwords` RENAME TO `passwords_old`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `passwords` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `site` TEXT NOT NULL, `username` TEXT NOT NULL, `encryptedPassword` TEXT NOT NULL, `iv` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `passwords` (id, site, username, encryptedPassword, iv, timestamp) SELECT id, site, username, password, '', timestamp FROM `passwords_old`")
                db.execSQL("DROP TABLE `passwords_old`")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `httpsOnlyMode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `deepDarkMode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `strictPrivacyMode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `geminiApiKey` TEXT")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `named_sessions` (`name` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`name`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `named_session_tabs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionName` TEXT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, FOREIGN KEY(`sessionName`) REFERENCES `named_sessions`(`name`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `annotations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `text` TEXT NOT NULL, `note` TEXT, `timestamp` INTEGER NOT NULL, `color` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_redirects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `source` TEXT NOT NULL, `target` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `torEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `torProxyHost` TEXT NOT NULL DEFAULT '127.0.0.1'")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `torProxyPort` INTEGER NOT NULL DEFAULT 9050")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `parentalPassword` TEXT")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `blockedSites` TEXT")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `alwaysIncognito` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `textReflowEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `ampBlockingEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `invertPageEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `forceZoom` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `forceLightTheme` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `forceBlackTheme` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `readerFontSize` REAL NOT NULL DEFAULT 18.0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `readerTheme` TEXT NOT NULL DEFAULT 'system'")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `readerFontFamily` TEXT NOT NULL DEFAULT 'serif'")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `toolbarLocation` TEXT NOT NULL DEFAULT 'bottom'")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `firefoxUserId` TEXT")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `firefoxCollectionName` TEXT")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tabs` ADD COLUMN `parentTabId` TEXT")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `rss_items` (`link` TEXT NOT NULL, `title` TEXT, `pubDate` TEXT, `source` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`link`))")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `per_site_settings` ADD COLUMN `customUserAgent` TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omni_browser_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT OR IGNORE INTO settings (id, searchEngine, adBlockEnabled, themeMode, lastTabUrl, accentColor, darkMode, downloadPath, restoreTabsOnStart, clearDataOnExit, javaScriptEnabled, blockThirdPartyCookies, httpsOnlyMode, deepDarkMode, strictPrivacyMode, geminiApiKey, customUserAgent, customSearchEngines, torEnabled, torProxyHost, torProxyPort, parentalPassword, blockedSites, alwaysIncognito, textReflowEnabled, ampBlockingEnabled, invertPageEnabled, forceZoom, forceLightTheme, forceBlackTheme, readerFontSize, readerTheme, readerFontFamily, toolbarLocation, firefoxUserId, firefoxCollectionName) " +
                                "VALUES (0, 'https://www.google.com/search?q=', 1, 'system', 'about:home', '#3B82F6', 0, NULL, 1, 0, 1, 1, 0, 0, 0, NULL, NULL, NULL, 0, '127.0.0.1', 9050, NULL, NULL, 0, 0, 0, 0, 0, 0, 0, 18.0, 'system', 'serif', 'bottom', NULL, NULL)")
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
