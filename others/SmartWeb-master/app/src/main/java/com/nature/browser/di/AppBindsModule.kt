package com.nature.browser.di

import com.nature.browser.adblock.allowlist.AllowListModel
import com.nature.browser.adblock.allowlist.SessionAllowListModel
import com.nature.browser.adblock.source.AssetsHostsDataSource
import com.nature.browser.adblock.source.HostsDataSource
import com.nature.browser.adblock.source.HostsDataSourceProvider
import com.nature.browser.adblock.source.PreferencesHostsDataSourceProvider
import com.nature.browser.database.adblock.HostsDatabase
import com.nature.browser.database.adblock.HostsRepository
import com.nature.browser.database.allowlist.AdBlockAllowListDatabase
import com.nature.browser.database.allowlist.AdBlockAllowListRepository
import com.nature.browser.database.bookmark.BookmarkDatabase
import com.nature.browser.database.bookmark.BookmarkRepository
import com.nature.browser.database.downloads.DownloadsDatabase
import com.nature.browser.database.downloads.DownloadsRepository
import com.nature.browser.database.history.HistoryDatabase
import com.nature.browser.database.history.HistoryRepository
import com.nature.browser.database.javascript.JavaScriptDatabase
import com.nature.browser.database.javascript.JavaScriptRepository
import com.nature.browser.ssl.SessionSslWarningPreferences
import com.nature.browser.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
abstract class AppBindsModule {

    @Binds
    abstract fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    abstract fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    abstract fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    abstract fun providesJavaScriptModel(javaScriptDatabase: JavaScriptDatabase): JavaScriptRepository

    @Binds
    abstract fun providesAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    abstract fun providesAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    abstract fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    abstract fun providesHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    abstract fun providesHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    abstract fun providesHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider
}
