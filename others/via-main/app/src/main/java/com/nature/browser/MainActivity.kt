package com.nature.browser

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nature.browser.ui.theme.NatureBrowserTheme
import com.nature.browser.ui.components.*
import com.nature.browser.ui.screens.*
import com.nature.browser.ui.tools.QRScannerScreen

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: BrowserViewModel

    private var activeFilePrompt: org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt? = null
    private val filePickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            activeFilePrompt?.confirm(this, it)
        } ?: activeFilePrompt?.dismiss()
        activeFilePrompt = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel = viewModel()

            val windowInsetsController = androidx.compose.ui.platform.LocalView.current.let { view ->
                androidx.core.view.WindowCompat.getInsetsController(window, view)
            }

            LaunchedEffect(viewModel.engine) {
                viewModel.engine.onFullScreenHandler = { isFullScreen ->
                    if (isFullScreen) {
                        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    }
                }
                viewModel.engine.onFilePromptHandler = { prompt ->
                    activeFilePrompt = prompt
                    filePickerLauncher.launch("*/*")
                }
            }
            val storage = remember { BrowserStorage(this) }
            var appTheme by remember { mutableStateOf(storage.appTheme) }

            LaunchedEffect(intent) {
                handleIntent(intent)
            }

            NatureBrowserTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel, storage) { appTheme = it }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.let { url ->
                viewModel.addTab(url)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BrowserViewModel,
    storage: BrowserStorage,
    onThemeChanged: (com.nature.browser.ui.theme.AppTheme) -> Unit
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val isSplitScreen by viewModel.isSplitScreen.collectAsState()
    val splitTabId by viewModel.splitTabId.collectAsState()

    var currentScreen by remember { mutableStateOf("browser") }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showVideoSpeedController by remember { mutableStateOf(false) }
    var currentVideoSpeed by remember { mutableStateOf(1.0f) }
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var offlineItem by remember { mutableStateOf<com.nature.browser.db.ReadingListEntity?>(null) }

    val activeTab = tabs.find { it.id == activeTabId }
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = activeTab != null && !showTabSwitcher && currentScreen == "browser") {
        viewModel.goBack(activeTab!!.id)
    }

    if (showTabSwitcher) {
        TabSwitcherGrid(
            viewModel = viewModel,
            onTabSelected = { viewModel.switchTab(it) },
            onClose = { showTabSwitcher = false }
        )
        return
    }

    when (currentScreen) {
        "settings" -> SettingsScreen(storage, { currentScreen = "browser" }, onThemeChanged)
        "downloads" -> DownloadsScreen { currentScreen = "browser" }
        "reading_list" -> ReadingListScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "browser" },
            onItemClick = { item ->
                offlineItem = item
                viewModel.addTab(item.url)
                currentScreen = "browser"
            }
        )
        "qr_scanner" -> QRScannerScreen(
            onScan = {
                activeTab?.session?.loadUri(it)
                currentScreen = "browser"
            },
            onBack = { currentScreen = "browser" }
        )
        "privacy" -> PrivacyReportScreen { currentScreen = "browser" }
        else -> {
            Scaffold(
                bottomBar = {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        actions = {
                            IconButton(onClick = { activeTab?.let { viewModel.goBack(it.id) } }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(onClick = { activeTab?.let { viewModel.goForward(it.id) } }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                            }
                            IconButton(onClick = { viewModel.addTab() }) {
                                Icon(Icons.Default.Add, contentDescription = "New Tab")
                            }
                            IconButton(onClick = { showTabSwitcher = true }) {
                                BadgedBox(badge = {
                                    Badge { Text(tabs.size.toString()) }
                                }) {
                                    Icon(Icons.Default.Layers, contentDescription = "Tabs")
                                }
                            }
                            IconButton(onClick = { currentScreen = "reading_list" }) {
                                Icon(Icons.Default.Bookmark, contentDescription = "Reading List")
                            }
                            IconButton(onClick = { currentScreen = "settings" }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = { showVideoSpeedController = true }) {
                                Icon(Icons.Default.SlowMotionVideo, contentDescription = "Video Speed")
                            }
                            IconButton(onClick = {
                                val text = activeTab?.selectedText?.value ?: ""
                                if (text.isNotEmpty()) {
                                    selectedText = text
                                    showAnnotationDialog = true
                                }
                            }) {
                                Icon(Icons.Default.EditNote, contentDescription = "Annotate")
                            }
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { currentScreen = "qr_scanner" },
                                containerColor = MaterialTheme.colorScheme.primary,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = 150
                                    val switchThreshold = 400
                                    if (kotlin.math.abs(totalDrag) > threshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (totalDrag > switchThreshold) {
                                        val currentIndex = tabs.indexOf(activeTab)
                                        if (currentIndex > 0) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.switchTab(tabs[currentIndex - 1].id)
                                        }
                                    } else if (totalDrag < -switchThreshold) {
                                        val currentIndex = tabs.indexOf(activeTab)
                                        if (currentIndex >= 0 && currentIndex < tabs.size - 1) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.switchTab(tabs[currentIndex + 1].id)
                                        }
                                    } else if (totalDrag > threshold) {
                                        activeTab?.let {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.goBack(it.id)
                                        }
                                    } else if (totalDrag < -threshold) {
                                        activeTab?.let {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.goForward(it.id)
                                        }
                                    }
                                    totalDrag = 0f
                                },
                                onDragCancel = { totalDrag = 0f }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y < -50) { // Swipe up
                                    showTabSwitcher = true
                                }
                            }
                        }
                ) {
                    activeTab?.let { tab ->
                            val progress by tab.progress.collectAsState()
                            if (progress == 0 && tab.url.value.isNotEmpty()) {
                                // Assume connection error or initial load
                                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE76F51)).padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.retry_stream), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.weight(1f))
                                        TextButton(onClick = { tab.session.reload() }) {
                                            Text("Retry", color = Color.White)
                                        }
                                    }
                                }
                            }

                        if (offlineItem != null && offlineItem!!.url == tab.url.value) {
                            OfflineBanner(date = offlineItem!!.timestamp, onRetry = {
                                tab.session.reload()
                                offlineItem = null
                            })
                        }

                        SmartAddressBar(
                            tab = tab,
                            onUrlSubmit = { url ->
                                val formattedUrl = if (url.contains(".") && !url.contains(" ")) {
                                    if (url.startsWith("http")) url else "https://$url"
                                } else {
                                    val searchUrl = storage.searchEngine + url
                                    searchUrl
                                }
                                tab.session.loadUri(formattedUrl)
                                offlineItem = null
                            }
                        )

                        if (tab.isIncognito) {
                            Text(
                                stringResource(R.string.incognito_message),
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showVideoSpeedController) {
                            VideoSpeedController(
                                currentSpeed = currentVideoSpeed,
                                onSpeedChange = { speed ->
                                    currentVideoSpeed = speed
                                    viewModel.setVideoSpeed(tab.id, speed)
                                },
                                onDismiss = { showVideoSpeedController = false }
                            )
                        }

                        if (showAnnotationDialog) {
                            var note by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showAnnotationDialog = false },
                                title = { Text("Leaf Note") },
                                text = {
                                    Column {
                                        Text("Highlight: $selectedText", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextField(value = note, onValueChange = { note = it }, label = { Text("Your reflection...") })
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        viewModel.addAnnotation(com.nature.browser.db.AnnotationEntity(
                                            url = tab.url.value,
                                            text = selectedText,
                                            note = note
                                        ))
                                        showAnnotationDialog = false
                                    }) { Text("Preserve") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAnnotationDialog = false }) { Text("Let it drift away") }
                                }
                            )
                        }

                        val readerContent by tab.readerContent.collectAsState()
                        if (readerContent != null) {
                            val title by tab.title.collectAsState()
                            com.nature.browser.reader.ReaderModeView(
                                title = title,
                                content = readerContent!!,
                                onClose = { viewModel.toggleReaderMode(tab.id) }
                            )
                        } else if (isSplitScreen && splitTabId != null) {
                            val splitTab = tabs.find { it.id == splitTabId }
                            if (splitTab != null) {
                                SplitScreenContainer(topTab = tab, bottomTab = splitTab, modifier = Modifier.weight(1f))
                            }
                        } else {
                            GeckoViewWrapper(tab = tab, modifier = Modifier.weight(1f))
                        }
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NewTabPage(
                            onShortcutClick = { viewModel.addTab(it) },
                            onSearch = {
                                val searchUrl = "${storage.searchEngine}$it"
                                viewModel.addTab(searchUrl)
                            }
                        )
                    }
                }
            }
        }
    }
}
