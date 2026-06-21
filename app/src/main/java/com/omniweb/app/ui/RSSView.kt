package com.omniweb.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniweb.app.discovery.FeedManager
import com.prof18.rssparser.model.RssItem

@Composable
fun RSSView(onNavigate: (String) -> Unit) {
    val feedManager = remember { FeedManager() }
    var items by remember { mutableStateOf(listOf<RssItem>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val allItems = mutableListOf<RssItem>()
        feedManager.getSmallWebFeeds().forEach { url ->
            try {
                val channel = feedManager.fetchFeed(url)
                allItems.addAll(channel.items)
            } catch (e: Exception) {}
        }
        items = allItems.sortedByDescending { it.pubDate }
        loading = false
    }

    if (loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.title ?: "No Title") },
                    supportingContent = { Text(item.pubDate ?: "") },
                    modifier = Modifier.padding(8.dp).clickable { item.link?.let { onNavigate(it) } }
                )
                HorizontalDivider()
            }
        }
    }
}
