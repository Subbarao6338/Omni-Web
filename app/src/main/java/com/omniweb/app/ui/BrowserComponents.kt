package com.omniweb.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.TabInfo
import com.omniweb.app.ui.Suggestion
import com.omniweb.app.util.UrlUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserAddressBar(
    modifier: Modifier = Modifier,
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onGo: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    isLoading: Boolean,
    pageFavicon: Bitmap?,
    onPrivacyClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isBookmarked: Boolean,
    isFindMode: Boolean,
    findQuery: String,
    onFindQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    findMatchStatus: String = "",
    onCloseFind: () -> Unit,
    onHomeClick: () -> Unit,
    onVoiceClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    suggestions: List<Suggestion>,
    onSuggestionClick: (Suggestion) -> Unit,
    blockedCount: Int = 0,
    tabCount: Int = 0,
    isIncognito: Boolean = false,
    mediaCount: Int = 0,
    isPageReadable: Boolean = false,
    onReaderClick: () -> Unit = {},
    onShowTabs: () -> Unit = {},
    onShowMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val leafScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leafScale"
    )
    var clipboardContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(urlInput) {
        if (urlInput.isEmpty()) {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && (text.startsWith("http") || text.contains("."))) {
                    clipboardContent = text
                }
            }
        } else {
            clipboardContent = null
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = modifier.statusBarsPadding()) {
        Column {
            if (isFindMode) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = findQuery,
                        onValueChange = onFindQueryChange,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        placeholder = { Text("Find in page...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (findMatchStatus.isNotEmpty()) {
                                    Text(findMatchStatus, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                                }
                                IconButton(onClick = { onFindNext(false) }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous") }
                                IconButton(onClick = { onFindNext(true) }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next") }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    )
                    TextButton(onClick = onCloseFind) {
                        Text("Done")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onHomeClick) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (isPageReadable) {
                        IconButton(onClick = onReaderClick) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Reader Mode", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (blockedCount > 0) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onPrivacyClick() }
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = "Privacy Shield", modifier = Modifier.size(20.dp), tint = Color(0xFF10B981))
                            }
                        }
                    } else {
                        IconButton(onClick = onPrivacyClick) {
                            Icon(Icons.Default.Shield, contentDescription = "Privacy Shield", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Box {
                            TextField(
                                value = urlInput,
                                onValueChange = onUrlChange,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                singleLine = true,
                                leadingIcon = {
                                    if (pageFavicon != null) {
                                        Image(
                                            bitmap = pageFavicon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                    } else {
                                        val isSecure = urlInput.startsWith("https")
                                        Icon(
                                            if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp).scale(if (isLoading) leafScale else 1f),
                                            tint = if (isSecure) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (clipboardContent != null && urlInput.isEmpty()) {
                                            IconButton(onClick = {
                                                onUrlChange(clipboardContent!!)
                                                onGo()
                                            }) {
                                                Icon(Icons.Default.ContentPasteGo, contentDescription = "Paste & Go", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        if (urlInput.isEmpty()) {
                                            IconButton(onClick = onVoiceClick) { Icon(Icons.Default.Mic, contentDescription = "Voice Search", modifier = Modifier.size(18.dp)) }
                                            IconButton(onClick = onScanClick) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(18.dp)) }
                                        }
                                        IconButton(onClick = onBookmarkClick) {
                                            Icon(
                                                if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Bookmark",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (isBookmarked) Color(0xFFFFB000) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (urlInput.isNotEmpty()) {
                                            IconButton(onClick = { onUrlChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp)) }
                                        } else {
                                            IconButton(onClick = { if (isLoading) onStop() else onRefresh() }) {
                                                Icon(if (isLoading) Icons.Default.Close else Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { onGo() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = if (isIncognito) Color(0xFF2D2D2D) else MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = if (isIncognito) Color(0xFF2D2D2D) else MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface,
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(color = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLoading,
                                enter = fadeIn() + androidx.compose.animation.expandVertically(),
                                exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .align(Alignment.BottomCenter)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Transparent
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = suggestions.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column {
                                    suggestions.forEach { suggestion ->
                                        ListItem(
                                            headlineContent = { Text(suggestion.title, maxLines = 1) },
                                            supportingContent = { Text(suggestion.url, maxLines = 1, fontSize = 12.sp) },
                                            leadingContent = {
                                                val icon = when {
                                                    suggestion.isHistory -> Icons.Default.History
                                                    suggestion.url == suggestion.title -> Icons.Default.Search
                                                    else -> Icons.Default.Star
                                                }
                                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = onShowTabs) {
                        BadgedBox(badge = { if (tabCount > 0) Badge { Text(tabCount.toString()) } }) {
                            Icon(Icons.Default.Layers, contentDescription = "Tabs")
                        }
                    }
                    IconButton(onClick = onShowMenu) {
                        BadgedBox(badge = { if (mediaCount > 0) Badge { Text(mediaCount.toString()) } }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserBottomBar(
    tabCount: Int,
    mediaCount: Int,
    onShowTabs: () -> Unit,
    onNewTab: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowMenu: () -> Unit
) {
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), modifier = Modifier.navigationBarsPadding(), contentPadding = PaddingValues(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            NavButton(Icons.Default.Layers, "Tabs", badge = tabCount) { onShowTabs() }
            NavButton(Icons.Default.Add, "New") { onNewTab() }
            NavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back") { onBack() }
            NavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Forward") { onForward() }
            NavButton(Icons.Default.Download, "Files") { onShowDownloads() }
            NavButton(Icons.Default.MoreVert, "Menu", badge = mediaCount) { onShowMenu() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabGridSwitcher(
    tabs: List<TabInfo>,
    activeTabId: String,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tabs, key = { it.id }) { tab ->
            val isSelected = tab.id == activeTabId
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                        onTabClose(tab.id)
                        true
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.2f)
                        SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color))
                },
                content = {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { onTabSelect(tab.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder(true).copy(width = 2.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else CardDefaults.outlinedCardBorder(true)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (tab.faviconBitmap != null) {
                                        Image(
                                            bitmap = tab.faviconBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(2.dp))
                                        )
                                    } else {
                                        Icon(
                                            if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Language,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (tab.isIncognito) Color(0xFF6366F1) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onTabClose(tab.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tab.thumbnail != null) {
                                    Image(
                                        bitmap = tab.thumbnail!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tab.url,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        }
    }
}
