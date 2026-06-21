package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniweb.app.data.Settings
import com.omniweb.app.data.TabInfo

@Composable
fun SplitScreenView(
    topTab: TabInfo,
    bottomTab: TabInfo,
    viewModel: BrowserViewModel,
    settings: Settings,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            WebViewContainer(
                tab = topTab,
                viewModel = viewModel,
                settings = settings,
                onLoginDetected = { _, _, _ -> },
                onBookmarkletDetected = { },
                onTextExtracted = { },
                onScrollChanged = { _, _ -> },
                onContextMenu = { },
                onProgressChanged = { topTab.progress = it },
                onTitleReceived = { topTab.title = it },
                onIconReceived = { topTab.faviconBitmap = it },
                onConsoleLog = { _, _ -> }
            )
        }
        Spacer(modifier = Modifier.height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Box(modifier = Modifier.weight(1f)) {
            WebViewContainer(
                tab = bottomTab,
                viewModel = viewModel,
                settings = settings,
                onLoginDetected = { _, _, _ -> },
                onBookmarkletDetected = { },
                onTextExtracted = { },
                onScrollChanged = { _, _ -> },
                onContextMenu = { },
                onProgressChanged = { bottomTab.progress = it },
                onTitleReceived = { bottomTab.title = it },
                onIconReceived = { bottomTab.faviconBitmap = it },
                onConsoleLog = { _, _ -> }
            )
        }
    }
}
