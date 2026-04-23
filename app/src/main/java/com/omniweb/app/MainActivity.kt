package com.omniweb.app

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniBrowserApp()
        }
    }
}

@Composable
fun OmniBrowserApp(viewModel: BrowserViewModel = viewModel()) {
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: com.omniweb.app.data.Settings()

    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

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
                composable("settings") {
                    SettingsView(
                        database = com.omniweb.app.data.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("bookmarks") {
                    BookmarksView(
                        database = com.omniweb.app.data.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current),
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
                    HistoryView(
                        database = com.omniweb.app.data.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current),
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
                        database = com.omniweb.app.data.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
