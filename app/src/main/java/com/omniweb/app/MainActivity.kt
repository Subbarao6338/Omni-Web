package com.omniweb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.omniweb.app.data.MediaItem
import com.omniweb.app.data.TabInfo
import com.omniweb.app.ui.BrowserView
import com.omniweb.app.ui.HomeView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniBrowserApp()
        }
    }
}

@Composable
fun OmniBrowserApp() {
    var currentUrl by remember { mutableStateOf("about:home") }
    var tabs by remember { mutableStateOf(listOf(TabInfo("1", "about:home", "Home"))) }
    var activeTabId by remember { mutableStateOf("1") }
    var detectedMedia by remember { mutableStateOf(listOf<MediaItem>()) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF3B82F6),
            onPrimary = Color.White,
            surface = Color(0xFFF5F5F5),
            onSurface = Color(0xFF1A1A1A)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (currentUrl == "about:home") {
                HomeView(
                    onNavigate = { url ->
                        currentUrl = url
                        tabs.find { it.id == activeTabId }?.url = url
                    }
                )
            } else {
                BrowserView(
                    url = currentUrl,
                    onUrlChange = {
                        currentUrl = it
                        tabs.find { it.id == activeTabId }?.url = it
                    },
                    onBackToHome = { currentUrl = "about:home" },
                    mediaItems = detectedMedia,
                    onMediaFound = { newMedia ->
                        val currentSrcs = detectedMedia.map { it.src }.toSet()
                        val uniqueNewMedia = newMedia.filter { it.src !in currentSrcs }
                        if (uniqueNewMedia.isNotEmpty()) {
                            detectedMedia = detectedMedia + uniqueNewMedia
                        }
                    }
                )
            }
        }
    }
}
