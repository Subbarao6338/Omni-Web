package com.omniweb.app

import android.os.Bundle
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omniweb.app.ui.*
import com.omniweb.app.data.AppDatabase
import com.yausername.youtubedl_android.YoutubeDL
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            val db = AppDatabase.getDatabase(this)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val settings = db.settingsDao().getSettings().firstOrNull()
                if (settings?.clearDataOnExit == true) {
                    db.historyDao().clearHistory()
                    db.tabDao().clearAllTabs()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().removeAllCookies(null)
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            val viewModel = androidx.lifecycle.ViewModelProvider(this)[BrowserViewModel::class.java]
            viewModel.hibernateTabsIfNeeded(force = true)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val viewModel = androidx.lifecycle.ViewModelProvider(this)[BrowserViewModel::class.java]
            val activeTabId = viewModel.activeTabId.value
            val activeTab = viewModel.tabs.find { it.id == activeTabId }
            if (activeTab != null && activeTab.detectedMedia.any { it.type == "video" }) {
                enterPictureInPictureMode(PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build())
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Adjust UI if needed, e.g., hide controls in PiP mode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        android.webkit.WebView.enableSlowWholeDocumentDraw()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@MainActivity)
            } catch (e: Exception) {
                Log.e("YoutubeDL", "failed to initialize youtubedl-android", e)
            }
        }

        enableEdgeToEdge()
        setContent {
            OmniBrowserApp()
        }
    }
}

@Composable
fun OmniBrowserApp(viewModel: BrowserViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isInPiP = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        (context as? android.app.Activity)?.isInPictureInPictureMode ?: false
    } else false

    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: com.omniweb.app.data.Settings()

    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: com.omniweb.app.data.TabInfo("default", "about:home", "Home")

    val accentColor = try {
        Color(android.graphics.Color.parseColor(settings.accentColor))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            surface = Color(0xFFF9FAFB),
            onSurface = Color(0xFF111827),
            surfaceVariant = Color(0xFFF3F4F6)
        )
    }

    val navController = rememberNavController()

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeView(
                        onNavigate = { url ->
                            activeTab.url = url
                            activeTab.title = "Loading..."
                            navController.navigate("browser")
                        },
                        viewModel = viewModel,
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenBookmarks = { navController.navigate("bookmarks") },
                        onOpenHistory = { navController.navigate("history") },
                        onOpenDownloads = { navController.navigate("downloads") }
                    )
                }
                composable("browser") {
                    if (isInPiP) {
                        WebViewContainer(
                            tab = activeTab,
                            viewModel = viewModel,
                            settings = settings,
                            onLoginDetected = { _, _, _ -> },
                            onBookmarkletDetected = { },
                            onTextExtracted = { },
                            onScrollChanged = { _, _ -> },
                            onContextMenu = { },
                            onProgressChanged = { activeTab.progress = it },
                            onTitleReceived = { activeTab.title = it },
                            onIconReceived = { activeTab.faviconBitmap = it },
                            onConsoleLog = { _, _ -> }
                        )
                    } else {
                        BrowserView(
                            activeTab = activeTab,
                            onBackToHome = {
                                activeTab.url = "about:home"
                                activeTab.title = "Home"
                                navController.popBackStack("home", inclusive = false)
                            },
                            viewModel = viewModel,
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenBookmarks = { navController.navigate("bookmarks") },
                            onOpenHistory = { navController.navigate("history") },
                            onOpenDownloads = { navController.navigate("downloads") }
                        )
                    }
                }
                composable("settings") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SettingsView(
                        database = AppDatabase.getDatabase(context),
                        onBack = { navController.popBackStack() },
                        onOpenScripts = { navController.navigate("scripts") },
                        onOpenPasswords = { navController.navigate("passwords") }
                    )
                }
                composable("passwords") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    PasswordManagerView(
                        database = AppDatabase.getDatabase(context),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("scripts") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ScriptManagerView(
                        database = AppDatabase.getDatabase(context),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("bookmarks") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    BookmarksView(
                        database = AppDatabase.getDatabase(context),
                        onNavigate = { url ->
                            activeTab.url = url
                            navController.navigate("browser") {
                                popUpTo("home")
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("history") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    HistoryView(
                        database = AppDatabase.getDatabase(context),
                        onNavigate = { url ->
                            activeTab.url = url
                            navController.navigate("browser") {
                                popUpTo("home")
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("downloads") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    DownloadsView(
                        database = AppDatabase.getDatabase(context),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
