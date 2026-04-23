package com.omniweb.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TabInfo(
    val id: String,
    initialUrl: String,
    initialTitle: String,
    val isIncognito: Boolean = false
) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf(initialTitle)
    var faviconUrl by mutableStateOf<String?>(null)
}

data class MediaItem(val id: String, val type: String, val src: String, val title: String)
