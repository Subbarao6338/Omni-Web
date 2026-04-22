package com.omniweb.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TabInfo(val id: String, initialUrl: String, initialTitle: String) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf(initialTitle)
}

data class MediaItem(val id: String, val type: String, val src: String, val title: String)
