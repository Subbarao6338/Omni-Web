package com.nature.browser.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Entity(tableName = "site_settings")
data class SiteSettings(
    @PrimaryKey val domain: String,
    val zoomLevel: Float = 1.0f,
    val isDesktopMode: Boolean = false,
    val customCss: String? = null,
    val customJs: String? = null,
    val isAdBlockEnabled: Boolean = true
)

@Dao
interface SiteSettingsDao {
    @Query("SELECT * FROM site_settings WHERE domain = :domain")
    suspend fun getSettingsForDomain(domain: String): SiteSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SiteSettings)

    @Delete
    suspend fun deleteSettings(settings: SiteSettings)
}

@Entity(tableName = "reading_list")
data class ReadingListEntity(
    @PrimaryKey val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val localFilePath: String? = null
)

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY timestamp DESC")
    fun getAllItems(): kotlinx.coroutines.flow.Flow<List<ReadingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ReadingListEntity)

    @Delete
    suspend fun deleteItem(item: ReadingListEntity)
}

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val text: String,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val color: Int = 0xFF57CC99.toInt() // Nature green default
)

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE url = :url ORDER BY timestamp DESC")
    fun getAnnotationsForUrl(url: String): kotlinx.coroutines.flow.Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)
}

@Database(entities = [SiteSettings::class, ReadingListEntity::class, NamedSession::class, NamedSessionTab::class, AnnotationEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteSettingsDao(): SiteSettingsDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun namedSessionDao(): NamedSessionDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `named_sessions` (`name` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`name`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `named_session_tabs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionName` TEXT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, FOREIGN KEY(`sessionName`) REFERENCES `named_sessions`(`name`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `annotations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `text` TEXT NOT NULL, `note` TEXT, `timestamp` INTEGER NOT NULL, `color` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nature_browser_db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
