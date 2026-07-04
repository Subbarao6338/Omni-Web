package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.RssItemEntity
import com.omniweb.app.discovery.FeedManager
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RSSView(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val feedManager = remember { FeedManager() }
    val cachedItems by database.rssItemDao().getAllRssItems().collectAsStateWithLifecycle(initialValue = emptyList())
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (cachedItems.isEmpty()) {
            loading = true
        }

        withContext(Dispatchers.IO) {
            val allItems = mutableListOf<RssItemEntity>()
            feedManager.getSmallWebFeeds().forEach { url ->
                try {
                    val channel = feedManager.fetchFeed(url)
                    allItems.addAll(channel.items.map {
                        RssItemEntity(
                            link = it.link ?: "",
                            title = it.title,
                            pubDate = it.pubDate,
                            source = channel.title ?: "Unknown"
                        )
                    })
                } catch (e: Exception) {}
            }
            if (allItems.isNotEmpty()) {
                database.rssItemDao().insertRssItems(allItems)
            }
        }
        loading = false
    }

    if (loading && cachedItems.isEmpty()) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(cachedItems, key = { it.link }) { item ->
                ListItem(
                    headlineContent = { Text(item.title ?: "No Title") },
                    supportingContent = { Text("${item.source} • ${item.pubDate ?: ""}") },
                    modifier = Modifier.padding(8.dp).clickable { onNavigate(item.link) }
                )
                HorizontalDivider()
            }
        }
    }
}
