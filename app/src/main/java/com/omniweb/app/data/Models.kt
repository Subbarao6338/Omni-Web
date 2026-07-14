package com.omniweb.app.data

import android.graphics.Bitmap
import androidx.compose.runtime.*

class TabInfo(
    val id: String,
    initialUrl: String,
    initialTitle: String,
    val isIncognito: Boolean = false,
    initialScrollX: Int = 0,
    initialScrollY: Int = 0,
    val parentTabId: String? = null
) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf(initialTitle)
    var faviconUrl by mutableStateOf<String?>(null)
    var faviconBitmap by mutableStateOf<Bitmap?>(null)
    var scrollX by mutableStateOf(initialScrollX)
    var scrollY by mutableStateOf(initialScrollY)
    var isLoading by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var playbackSpeed by mutableFloatStateOf(1.0f)
    val detectedMedia = mutableStateListOf<MediaItem>()
    var scrollProgress by mutableFloatStateOf(0f)
    var isPageReadable by mutableStateOf(false)
    var thumbnail by mutableStateOf<Bitmap?>(null)
}

data class MediaItem(val id: String, val type: String, val src: String, val title: String)

data class ConsoleLog(val message: String, val level: String)
