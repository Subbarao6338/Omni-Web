package com.omniweb.app.util

import com.omniweb.app.data.Bookmark
import java.io.InputStream

object NetscapeBookmarkFormatImporter {
    fun import(inputStream: InputStream): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        val content = inputStream.bufferedReader().use { it.readText() }
        val regex = """<A HREF="([^"]+)"[^>]*>([^<]+)</A>""".toRegex(RegexOption.IGNORE_CASE)
        val matches = regex.findAll(content)
        for (match in matches) {
            val url = match.groupValues[1]
            val title = match.groupValues[2]
            bookmarks.add(Bookmark(title = title, url = url))
        }
        return bookmarks
    }
}
