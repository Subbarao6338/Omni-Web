package com.omniweb.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    suggestions: List<Suggestion>,
    onSuggestionClick: (Suggestion) -> Unit,
    blockedCount: Int = 0
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.statusBarsPadding()) {
        Column {
            if (isFindMode) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = findQuery,
                        onValueChange = onFindQueryChange,
                        modifier = Modifier.weight(1f).height(48.dp),
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
                        )
                    )
                    TextButton(onClick = onCloseFind) {
                        Text("Done")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onHomeClick) { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(modifier = Modifier.weight(1f)) {
                        Box {
                            TextField(
                                value = urlInput,
                                onValueChange = onUrlChange,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                leadingIcon = {
                                    if (pageFavicon != null) {
                                        Image(
                                            bitmap = pageFavicon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).clickable { onPrivacyClick() }
                                        )
                                    } else {
                                        val icon = if (urlInput.startsWith("https")) Icons.Default.Lock else Icons.Default.Info
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp).clickable { onPrivacyClick() },
                                            tint = if (urlInput.startsWith("https")) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (blockedCount > 0) {
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.clickable { onPrivacyClick() }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF10B981))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(blockedCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                                }
                                            }
                                        }
                                        if (urlInput.isNotEmpty()) {
                                            IconButton(onClick = { onUrlChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { onGo() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )

                            if (isLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
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
                                            modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { if (isLoading) onStop() else onRefresh() }) {
                        Icon(
                            if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
    onShowMedia: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowMenu: () -> Unit
) {
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), modifier = Modifier.navigationBarsPadding(), contentPadding = PaddingValues(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            NavButton(Icons.Default.Layers, "Tabs", badge = tabCount) { onShowTabs() }
            NavButton(Icons.Default.Add, "New") { onNewTab() }
            NavButton(Icons.Default.VideoLibrary, "Media", badge = mediaCount) { onShowMedia() }
            NavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back") { onBack() }
            NavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Forward") { onForward() }
            NavButton(Icons.Default.Download, "Files") { onShowDownloads() }
            NavButton(Icons.Default.MoreVert, "Menu") { onShowMenu() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<TabInfo>,
    activeTabId: String,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onCloseAll: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Row {
                    IconButton(onClick = onCloseAll) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Close All Tabs", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { onNewTab(true) }) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "New Incognito Tab")
                    }
                    IconButton(onClick = { onNewTab(false) }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tabs) { tab ->
                    val isSelected = tab.id == activeTabId
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable {
                                onTabSelect(tab.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        elevation = if (isSelected) CardDefaults.outlinedCardElevation(defaultElevation = 8.dp) else CardDefaults.outlinedCardElevation()
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = { onTabClose(tab.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                if (tab.faviconBitmap != null) {
                                    Image(
                                        bitmap = tab.faviconBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(tab.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
