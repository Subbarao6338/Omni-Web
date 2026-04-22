package com.omniweb.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniBrowserApp()
        }
    }
}

// Data Classes
data class Shortcut(val title: String, val url: String)
data class TabInfo(val id: String, var url: String, var title: String)
data class MediaItem(val id: String, val type: String, val src: String, val title: String)

// JavaScript Interface for communication
class WebAppInterface(
    private val onMediaDetected: (List<MediaItem>) -> Unit,
    private val onTextExtracted: (String) -> Unit
) {
    @JavascriptInterface
    fun postMedia(json: String) {
        try {
            val array = JSONArray(json)
            val list = mutableListOf<MediaItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(MediaItem(
                    id = obj.optString("id", Math.random().toString()),
                    type = obj.optString("type", "video"),
                    src = obj.getString("src"),
                    title = obj.optString("title", "Media File")
                ))
            }
            onMediaDetected(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun postText(text: String) {
        onTextExtracted(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniBrowserApp() {
    var currentUrl by remember { mutableStateOf("about:home") }
    var tabs by remember { mutableStateOf(listOf(TabInfo("1", "about:home", "Home"))) }
    var activeTabId by remember { mutableStateOf("1") }
    var detectedMedia by remember { mutableStateOf(listOf<MediaItem>()) }

    val scope = rememberCoroutineScope()

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
                        // Merge and deduplicate
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

@Composable
fun HomeView(onNavigate: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val shortcuts = remember {
        mutableStateListOf(
            Shortcut("Google", "https://www.google.com"),
            Shortcut("YouTube", "https://www.youtube.com"),
            Shortcut("GitHub", "https://www.github.com"),
            Shortcut("Reddit", "https://www.reddit.com"),
            Shortcut("Wikipedia", "https://www.wikipedia.org"),
            Shortcut("Amazon", "https://www.amazon.com"),
            Shortcut("X", "https://x.com"),
            Shortcut("Instagram", "https://www.instagram.com")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(top = 96.dp, start = 32.dp, end = 32.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(text = "Omni Browser", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Spacer(modifier = Modifier.height(40.dp))

        val focusManager = LocalFocusManager.current
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search or type URL", fontSize = 18.sp) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotEmpty()) {
                    var target = query
                    if (!target.contains(".") || target.contains(" ")) {
                        target = "https://www.google.com/search?q=${android.net.Uri.encode(target)}"
                    } else if (!target.startsWith("http")) {
                        target = "https://$target"
                    }
                    onNavigate(target)
                }
                focusManager.clearFocus()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFE5E7EB),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )

        Spacer(modifier = Modifier.height(64.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            userScrollEnabled = false
        ) {
            items(shortcuts) { shortcut ->
                ShortcutItem(shortcut, onClick = { onNavigate(shortcut.url) })
            }
            item { AddShortcutItem() }
        }
    }
}

@Composable
fun ShortcutItem(shortcut: Shortcut, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Language, contentDescription = null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = shortcut.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)
    }
}

@Composable
fun AddShortcutItem() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserView(
    url: String,
    onUrlChange: (String) -> Unit,
    onBackToHome: () -> Unit,
    mediaItems: List<MediaItem>,
    onMediaFound: (List<MediaItem>) -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(url) }
    var showTools by remember { mutableStateOf(false) }
    var aiSummary by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var pageText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onBackToHome()
    }

    Scaffold(
        topBar = {
            Surface(color = Color.White.copy(alpha = 0.95f), shadowElevation = 2.dp, modifier = Modifier.statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onBackToHome) { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            var target = urlInput
                            if (!target.startsWith("http") && !target.startsWith("about:")) target = "https://$target"
                            webView?.loadUrl(target)
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                    IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.White.copy(alpha = 0.95f), modifier = Modifier.navigationBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    NavButton(Icons.Default.Layers, "Tabs") {}
                    NavButton(Icons.Default.Download, "Files") {}
                    NavButton(Icons.Default.MoreVert, "Menu") { showTools = true }
                    NavButton(Icons.Default.VideoLibrary, "Media", badge = mediaItems.size) {}
                    NavButton(Icons.Default.AutoAwesome, "AI") { showTools = true }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        addJavascriptInterface(WebAppInterface(
                            onMediaDetected = { onMediaFound(it) },
                            onTextExtracted = { pageText = it }
                        ), "Android")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                                url?.let { urlInput = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { onUrlChange(it) }

                                // Extract text for AI
                                view?.evaluateJavascript("Android.postText(document.body.innerText)", null)

                                // Sniff media
                                view?.evaluateJavascript("""
                                    (function() {
                                        function sniff() {
                                            const media = [];
                                            document.querySelectorAll('video, audio, source').forEach(el => {
                                                const src = el.src || el.getAttribute('src');
                                                if (src && src.startsWith('http')) {
                                                    media.push({
                                                        id: Math.random().toString(36).substr(2, 9),
                                                        src: src,
                                                        type: el.tagName.toLowerCase(),
                                                        title: document.title || 'Media File'
                                                    });
                                                }
                                            });
                                            if (media.length > 0) {
                                                Android.postMedia(JSON.stringify(media));
                                            }
                                        }
                                        setInterval(sniff, 3000);
                                        sniff();
                                    })();
                                """.trimIndent(), null)
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val host = request?.url?.host ?: ""
                                if (host.contains("doubleclick.net") || host.contains("googleadservices.com") || host.contains("adnxs.com")) {
                                    return WebResourceResponse("text/plain", "UTF-8", null)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter), color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showTools) {
        ModalBottomSheet(onDismissRequest = { showTools = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
                Text("Page Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    ToolButton(Icons.Default.AutoAwesome, "AI Summary", Color(0xFF9333EA)) {
                        scope.launch {
                            isAiLoading = true
                            aiSummary = ""
                            try {
                                // In a real app, API key should be in local.properties and injected via BuildConfig
                                val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = "YOUR_API_KEY")
                                val response = model.generateContent(content { text("Summarize this web page content concisely: $pageText") })
                                aiSummary = response.text ?: "No summary available."
                            } catch (e: Exception) {
                                aiSummary = "To use AI features, please configure a Gemini API key in the source code. Error: ${e.message}"
                            } finally {
                                isAiLoading = false
                            }
                        }
                    }
                    ToolButton(Icons.Default.Code, "View Source", Color(0xFFEA580C)) {}
                    ToolButton(Icons.Default.History, "History", Color(0xFF2563EB)) {}
                    ToolButton(Icons.Default.Settings, "Settings", Color(0xFF4B5563)) {}
                }

                if (isAiLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
                if (aiSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(color = Color(0xFFF5F3FF), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(aiSummary, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun NavButton(icon: ImageVector, label: String, badge: Int = 0, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = Color.Gray)
            if (badge > 0) {
                Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), shape = CircleShape) {
                    Text(badge.toString(), color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}

@Composable
fun ToolButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
