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
import androidx.compose.ui.platform.LocalContext
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.MediaItem
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo
import com.omniweb.app.ui.BrowserView
import com.omniweb.app.ui.HomeView
import java.util.UUID

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
fun OmniBrowserApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsState by database.settingsDao().getSettings().collectAsState(initial = Settings())
    val settings = settingsState ?: Settings()

    val tabs = remember { mutableStateListOf(TabInfo(UUID.randomUUID().toString(), "about:home", "Home")) }
    var activeTabId by remember { mutableStateOf(tabs.firstOrNull()?.id ?: "") }
    var detectedMedia by remember { mutableStateOf(listOf<MediaItem>()) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: TabInfo(UUID.randomUUID().toString(), "about:home", "Home")

    val accentColor = try {
        Color(android.graphics.Color.parseColor(settings.accentColor))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val darkTheme = settings.darkMode || (settings.darkMode == false && isSystemInDarkTheme())

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            surface = Color(0xFFF5F5F5),
            onSurface = Color(0xFF1A1A1A)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val currentScreen = if (activeTab.url == "about:home") "home" else "browser"

            when (currentScreen) {
                "home" -> {
                    HomeView(
                        onNavigate = { url ->
                            activeTab.url = url
                            activeTab.title = "Loading..."
                        },
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { id -> activeTabId = id },
                        onNewTab = {
                            val newTab = TabInfo(UUID.randomUUID().toString(), "about:home", "Home")
                            tabs.add(newTab)
                            activeTabId = newTab.id
                        },
                        onCloseTab = { id ->
                            val index = tabs.indexOfFirst { it.id == id }
                            if (index != -1) {
                                tabs.removeAt(index)
                                if (tabs.isEmpty()) {
                                    val newTab = TabInfo(UUID.randomUUID().toString(), "about:home", "Home")
                                    tabs.add(newTab)
                                    activeTabId = newTab.id
                                } else if (activeTabId == id) {
                                    activeTabId = tabs[maxOf(0, index - 1)].id
                                }
                            }
                        }
                    )
                }
                "browser" -> {
                    BrowserView(
                        url = activeTab.url,
                        onUrlChange = { newUrl ->
                            activeTab.url = newUrl
                        },
                        onBackToHome = {
                            activeTab.url = "about:home"
                            activeTab.title = "Home"
                        },
                        mediaItems = detectedMedia,
                        onMediaFound = { newMedia ->
                            val currentSrcs = detectedMedia.map { it.src }.toSet()
                            val uniqueNewMedia = newMedia.filter { it.src !in currentSrcs }
                            if (uniqueNewMedia.isNotEmpty()) {
                                detectedMedia = detectedMedia + uniqueNewMedia
                            }
                        },
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { id ->
                            activeTabId = id
                        },
                        onNewTab = {
                            val newTab = TabInfo(UUID.randomUUID().toString(), "about:home", "Home")
                            tabs.add(newTab)
                            activeTabId = newTab.id
                        },
                        onCloseTab = { id ->
                            val index = tabs.indexOfFirst { it.id == id }
                            if (index != -1) {
                                tabs.removeAt(index)
                                if (tabs.isEmpty()) {
                                    val newTab = TabInfo(UUID.randomUUID().toString(), "about:home", "Home")
                                    tabs.add(newTab)
                                    activeTabId = newTab.id
                                } else if (activeTabId == id) {
                                    activeTabId = tabs[maxOf(0, index - 1)].id
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
