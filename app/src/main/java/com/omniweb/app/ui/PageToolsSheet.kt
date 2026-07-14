package com.omniweb.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.*
import com.omniweb.app.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageToolsSheet(
    activeTab: TabInfo,
    viewModel: BrowserViewModel,
    isSplitScreen: Boolean,
    isDesktopMode: Boolean,
    bookmarks: List<Bookmark>,
    database: AppDatabase,
    onBackToHome: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onShowAiChat: () -> Unit,
    onShowTranslate: () -> Unit,
    onShowSource: (String) -> Unit,
    onShowConsole: () -> Unit,
    onShowMediaGrabber: () -> Unit,
    onShowBookmarklets: () -> Unit,
    onReaderMode: (String) -> Unit,
    onSummarize: (String) -> Unit,
    onQrCode: () -> Unit,
    onInsights: (AnalysisResult) -> Unit,
    onVideoSpeed: () -> Unit,
    onInspectMode: () -> Unit,
    onFindInPage: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Text("Page Tools", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))

            val toolsWebView = viewModel.getOrCreateWebView(activeTab.id, context)

            ToolCategory("Navigation") {
                item { ToolButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", Color(0xFF3B82F6)) {
                    if (toolsWebView.canGoBack()) toolsWebView.goBack() else onBackToHome()
                    onDismiss()
                }}
                item { ToolButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Forward", Color(0xFF3B82F6)) {
                    if (toolsWebView.canGoForward()) toolsWebView.goForward()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Add, "New Tab", Color(0xFF10B981)) {
                    viewModel.createTab()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Home, "Home", Color(0xFF6B7280)) {
                    onBackToHome()
                    onDismiss()
                }}
                item {
                    val isBookmarkedInternal = bookmarks.any { it.url == activeTab.url }
                    ToolButton(if (isBookmarkedInternal) Icons.Default.Star else Icons.Default.StarBorder, if (isBookmarkedInternal) "Bookmarked" else "Bookmark", Color(0xFFFFB000)) {
                        scope.launch {
                            if (isBookmarkedInternal) {
                                bookmarks.find { it.url == activeTab.url }?.let { database.bookmarkDao().deleteBookmark(it) }
                            } else {
                                database.bookmarkDao().insertBookmark(Bookmark(title = activeTab.title, url = activeTab.url))
                            }
                        }
                        onDismiss()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ToolCategory("Page Actions") {
                item { ToolButton(if (isSplitScreen) Icons.Default.Fullscreen else Icons.Default.VerticalSplit, if (isSplitScreen) "Single Screen" else "Split Screen", Color(0xFFF59E0B)) {
                    viewModel.toggleSplitScreen()
                    onDismiss()
                }}
                item { ToolButton(Icons.AutoMirrored.Filled.OpenInNew, "Open in App", Color(0xFF3B82F6)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No app can handle this link", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Share, "Share", Color(0xFF3B82F6)) {
                    toolsWebView.url?.let {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, it)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Link"))
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.AutoMirrored.Filled.Chat, "AI Chat", Color(0xFF8B5CF6)) {
                    onShowAiChat()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.ContentCopy, "Copy Link", Color(0xFF8B5CF6)) {
                    toolsWebView.url?.let {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", it))
                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Search, "Find", Color(0xFF3B82F6)) {
                    onFindInPage()
                    onDismiss()
                }}
                item { ToolButton(Icons.AutoMirrored.Filled.MenuBook, "Reader", Color(0xFFEA580C)) {
                    toolsWebView.evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
                        onReaderMode(source ?: "")
                    }
                    onDismiss()
                }}
                item { ToolButton(if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone, if (isDesktopMode) "Mobile" else "Desktop", Color(0xFF6366F1)) {
                    onToggleDesktopMode()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.AutoAwesome, "Summarize", Color(0xFF8B5CF6)) {
                    toolsWebView.evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
                        onSummarize(source ?: "")
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.QrCode, "QR Code", Color(0xFF10B981)) {
                    onQrCode()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Insights, "Insights", Color(0xFF10B981)) {
                    PageAnalyzer.analyze(toolsWebView) {
                        onInsights(it)
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.SlowMotionVideo, "Video Speed", Color(0xFFEA580C)) {
                    onVideoSpeed()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.SelfImprovement, "Zen Mode", Color(0xFF10B981)) {
                    viewModel.toggleZenMode()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.VideoLibrary, "Media Grabber", Color(0xFFEC4899)) {
                    onShowMediaGrabber()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Download, "Downloads", Color(0xFF3B82F6)) {
                    onOpenDownloads()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Translate, "Translate", Color(0xFF3B82F6)) {
                    onShowTranslate()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.AddHome, "Add Home", Color(0xFF10B981)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                        if (shortcutManager!!.isRequestPinShortcutSupported) {
                            val pinShortcutInfo = ShortcutInfo.Builder(context, activeTab.url)
                                .setShortLabel(toolsWebView.title ?: "Web Page")
                                .setIcon(if (activeTab.faviconBitmap != null) Icon.createWithBitmap(activeTab.faviconBitmap) else Icon.createWithResource(context, com.omniweb.app.R.mipmap.ic_launcher))
                                .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url)))
                                .build()
                            shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                        }
                    }
                    onDismiss()
                }}
            }

            Spacer(modifier = Modifier.height(24.dp))
            ToolCategory("Save & Print") {
                item { ToolButton(Icons.Default.CameraAlt, "Full Shot", Color(0xFF06B6D4)) {
                    PageUtils.takeFullPageScreenshot(context, toolsWebView, activeTab.title)
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.PictureAsPdf, "Save PDF", Color(0xFFEF4444)) {
                    PageUtils.saveAsPdf(context, toolsWebView, activeTab.title)
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Archive, "Save MHTML", Color(0xFF8B5CF6)) {
                    scope.launch {
                        val path = PageUtils.saveAsMhtml(context, toolsWebView, activeTab.title)
                        database.readingListDao().insertEntry(com.omniweb.app.data.ReadingListEntry(title = activeTab.title, url = activeTab.url, filePath = path))
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Description, "Save MD", Color(0xFF10B981)) {
                    toolsWebView.evaluateJavascript("(function(){ const clone = document.body.cloneNode(true); clone.querySelectorAll('script, style, iframe, noscript').forEach(el => el.remove()); return clone.innerHTML; })()") { source ->
                        val clean = if (source != null && source.startsWith("\"") && source.endsWith("\"")) source.substring(1, source.length - 1).replace("\\\"", "\"").replace("\\n", "\n") else source ?: ""
                        PageUtils.saveAsMarkdown(context, clean, activeTab.title)
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Print, "Print", Color(0xFF4B5563)) {
                    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                    printManager.print("Omni Document", toolsWebView.createPrintDocumentAdapter("Document"), null)
                    onDismiss()
                }}
            }

            Spacer(modifier = Modifier.height(24.dp))
            ToolCategory("Developer Tools") {
                item { ToolButton(Icons.Default.RecordVoiceOver, "Speak", Color(0xFF10B981)) {
                    onSpeak()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.VoiceOverOff, "Stop Voice", Color(0xFFEF4444)) {
                    viewModel.stopSpeaking()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Code, "Source", Color(0xFFEA580C)) {
                    toolsWebView.evaluateJavascript("document.documentElement.outerHTML") { source ->
                        onShowSource(source ?: "")
                    }
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Terminal, "Console", Color(0xFF10B981)) {
                    onShowConsole()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.Javascript, "Bookmarklets", Color(0xFFFACC15)) {
                    onShowBookmarklets()
                    onDismiss()
                }}
                item { ToolButton(Icons.Default.BugReport, "Inspect", Color(0xFFEF4444)) {
                    onInspectMode()
                    onDismiss()
                }}
            }

            Spacer(modifier = Modifier.height(24.dp))
            ToolCategory("Browser") {
                item { ToolButton(Icons.Default.Settings, "Settings", Color(0xFF4B5563)) { onOpenSettings(); onDismiss() }}
                item { ToolButton(Icons.Default.History, "History", Color(0xFF607D8B)) { onOpenHistory(); onDismiss() }}
                item { ToolButton(Icons.Default.Star, "Bookmarks", Color(0xFFFFB000)) { onOpenBookmarks(); onDismiss() }}
                item { ToolButton(Icons.Default.Download, "Downloads", Color(0xFF3B82F6)) { onOpenDownloads(); onDismiss() }}
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
