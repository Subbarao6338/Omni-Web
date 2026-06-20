package com.nature.browser

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import org.mozilla.geckoview.GeckoSession
import java.util.UUID

data class TabModel(
    val id: String = UUID.randomUUID().toString(),
    var session: GeckoSession,
    var url: MutableStateFlow<String> = MutableStateFlow(""),
    var title: MutableStateFlow<String> = MutableStateFlow(""),
    var progress: MutableStateFlow<Int> = MutableStateFlow(0),
    var isSecure: MutableStateFlow<Boolean> = MutableStateFlow(false),
    var readerContent: MutableStateFlow<String?> = MutableStateFlow(null),
    var lastActive: Long = System.currentTimeMillis(),
    var isIncognito: Boolean = false,
    val navigationStack: NavigationStack = NavigationStack(),
    var scrollX: Int = 0,
    var scrollY: Int = 0,
    var thumbnail: MutableStateFlow<Bitmap?> = MutableStateFlow(null),
    var selectedText: MutableStateFlow<String> = MutableStateFlow(""),
    var savedState: GeckoSession.SessionState? = null,
    var onPageStop: (() -> Unit)? = null
)
