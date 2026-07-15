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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omniweb.app.ui.*
import com.omniweb.app.data.*
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

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[BrowserViewModel::class.java]
        when (level) {
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                viewModel.hibernateTabsIfNeeded(force = true, isCritical = true)
            }
            android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                viewModel.hibernateTabsIfNeeded(force = true)
            }
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
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (isInPictureInPictureMode) {
            // Hide system UI in PiP mode
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Restore system UI when exiting PiP
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[BrowserViewModel::class.java]
        viewModel.stopSpeaking()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[BrowserViewModel::class.java]
        viewModel.handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        android.webkit.WebView.enableSlowWholeDocumentDraw()

        val viewModel = androidx.lifecycle.ViewModelProvider(this)[BrowserViewModel::class.java]
        if (savedInstanceState == null) {
            viewModel.handleIntent(intent)
        }

        com.omniweb.app.util.AdBlockManager.init(this)

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
    val navController = rememberNavController()
    val pendingNavigation by viewModel.pendingNavigation

    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { route ->
            navController.navigate(route) {
                launchSingleTop = true
            }
            viewModel.onNavigationHandled()
        }
    }

    val appContext = androidx.compose.ui.platform.LocalContext.current
    val isInPiP = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        (appContext as? android.app.Activity)?.isInPictureInPictureMode ?: false
    } else false

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
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
                            onConsoleLog = { _, _ -> },
                            onDownload = { url, name ->
                                val downloadManager = com.omniweb.app.util.OmniDownloadManager(appContext)
                                downloadManager.startDownload(url, name)
                            }
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
                            onOpenDownloads = { navController.navigate("downloads") },
                            onOpenScanner = { navController.navigate("qr_scanner") }
                        )
                    }
                }
                composable("settings") {
                    SettingsView(
                        database = AppDatabase.getDatabase(appContext),
                        onBack = { navController.popBackStack() },
                        onOpenScripts = { navController.navigate("scripts") },
                        onOpenPasswords = { navController.navigate("passwords") },
                        onOpenSearchEngines = { navController.navigate("search_engines") },
                        onOpenParentalControls = { navController.navigate("parental_controls") }
                    )
                }
                composable("parental_controls") {
                    ParentalControlView(
                        database = AppDatabase.getDatabase(appContext),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("search_engines") {
                    SearchEngineManagerView(
                        database = AppDatabase.getDatabase(appContext),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("passwords") {
                    PasswordManagerView(
                        database = AppDatabase.getDatabase(appContext),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("scripts") {
                    ScriptManagerView(
                        database = AppDatabase.getDatabase(appContext),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("bookmarks") {
                    BookmarksView(
                        database = AppDatabase.getDatabase(appContext),
                        onNavigate = { url ->
                            activeTab.url = url
                            navController.navigate("browser") {
                                popUpTo("home")
                            }
                        },
                        onBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )
                }
                composable("history") {
                    HistoryView(
                        database = AppDatabase.getDatabase(appContext),
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
                    DownloadsView(
                        database = AppDatabase.getDatabase(appContext),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("qr_scanner") {
                    QRScannerView(
                        onScan = { url ->
                            activeTab.url = url
                            navController.navigate("browser") {
                                popUpTo("home")
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
