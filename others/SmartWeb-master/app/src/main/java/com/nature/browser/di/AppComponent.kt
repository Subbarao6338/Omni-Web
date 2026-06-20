package com.nature.browser.di

import com.nature.browser.BrowserApp
import com.nature.browser.adblock.BloomFilterAdBlocker
import com.nature.browser.adblock.NoOpAdBlocker
import com.nature.browser.browser.SearchBoxModel
import com.nature.browser.browser.activity.BrowserActivity
import com.nature.browser.browser.activity.ThemableBrowserActivity
import com.nature.browser.browser.bookmarks.BookmarksDrawerView
import com.nature.browser.device.BuildInfo
import com.nature.browser.dialog.LightningDialogBuilder
import com.nature.browser.download.LightningDownloadListener
import com.nature.browser.reading.activity.ReadingActivity
import com.nature.browser.search.SuggestionsAdapter
import com.nature.browser.settings.activity.SettingsActivity
import com.nature.browser.settings.activity.ThemableSettingsActivity
import com.nature.browser.settings.fragment.*
import com.nature.browser.view.SmartCookieChromeClient
import com.nature.browser.view.SmartCookieView
import com.nature.browser.view.NatureBrowserClient
import android.app.Application
import com.nature.browser.download.DownloadActivity
import com.nature.browser.history.HistoryActivity
import com.nature.browser.popup.PopUpClass
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class), (AppBindsModule::class)])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun buildInfo(buildInfo: BuildInfo): Builder

        fun build(): AppComponent
    }

    fun inject(activity: BrowserActivity)

    fun inject(activity: DownloadActivity)

    fun inject(activity: HistoryActivity)

    fun inject(fragment: ExportSettingsFragment)

    fun inject(builder: LightningDialogBuilder)

    fun inject(smartCookieView: SmartCookieView)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(advancedSettingsFragment: AdvancedSettingsFragment)

    fun inject(app: BrowserApp)

    fun inject(activity: ReadingActivity)

    fun inject(webClient: NatureBrowserClient)

    fun inject(activity: SettingsActivity)

    fun inject(activity: ThemableSettingsActivity)

    fun inject(listener: LightningDownloadListener)

    fun inject(fragment: PrivacySettingsFragment)

    fun inject(fragment: DebugSettingsFragment)

    fun inject(fragment: ExtensionsSettingsFragment)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(chromeClient: SmartCookieChromeClient)

    fun inject(searchBoxModel: SearchBoxModel)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(displaySettingsFragment: DisplaySettingsFragment)

    fun inject(adBlockSettingsFragment: AdBlockSettingsFragment)

    fun inject(drawerSettingsFragment: DrawerSettingsFragment)

    fun inject(homepageSettingsFragment: HomepageSettingsFragment)

    fun inject(themeSettingsFragment: ThemeSettingsFragment)

    fun inject(drawerOffsetFragment: DrawerOffsetFragment)

    fun inject(parentalSettingsFragment: ParentalControlSettingsFragment)

    fun inject(bookmarksView: BookmarksDrawerView)

    fun provideBloomFilterAdBlocker(): BloomFilterAdBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

    fun inject(popUpClass: PopUpClass)
}
