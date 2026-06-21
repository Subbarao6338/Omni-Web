package com.omniweb.app.discovery

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedManager {
    private val parser = RssParser()

    suspend fun fetchFeed(url: String): RssChannel = withContext(Dispatchers.IO) {
        parser.getRssChannel(url)
    }

    fun getSmallWebFeeds(): List<String> {
        return listOf(
            "https://kagi.com/smallweb/rss",
            "https://marginalia.nu/feed.xml",
            "https://wiby.me/rss/"
        )
    }
}
