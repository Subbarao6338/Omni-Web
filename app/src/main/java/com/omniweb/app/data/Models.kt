package com.omniweb.app.data

data class Shortcut(val title: String, val url: String)
data class TabInfo(val id: String, var url: String, var title: String)
data class MediaItem(val id: String, val type: String, val src: String, val title: String)
