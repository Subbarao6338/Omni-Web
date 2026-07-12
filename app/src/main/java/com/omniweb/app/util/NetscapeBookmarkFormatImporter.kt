package com.omniweb.app.util

import com.omniweb.app.data.Bookmark
import java.io.InputStream

object NetscapeBookmarkFormatImporter {
    fun import(inputStream: InputStream): List<Bookmark> {
        val content = inputStream.bufferedReader().use { it.readText() }
        val trimmed = content.trim()

        return if (trimmed.contains("<A ", ignoreCase = true) || trimmed.contains("<DL", ignoreCase = true)) {
            importHtml(content)
        } else {
            importTxt(content)
        }
    }

    fun importHtml(content: String): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        val regex = """<A HREF="([^"]+)"[^>]*>([^<]+)</A>""".toRegex(RegexOption.IGNORE_CASE)
        val matches = regex.findAll(content)
        for (match in matches) {
            val url = match.groupValues[1]
            val title = match.groupValues[2]
            bookmarks.add(Bookmark(title = title, url = url))
        }
        return bookmarks
    }

    fun importTxt(content: String): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        val lines = content.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val httpIndex = trimmed.indexOf("http://", ignoreCase = true)
            val httpsIndex = trimmed.indexOf("https://", ignoreCase = true)

            val urlStartIndex = if (httpIndex != -1 && httpsIndex != -1) {
                minOf(httpIndex, httpsIndex)
            } else if (httpIndex != -1) {
                httpIndex
            } else if (httpsIndex != -1) {
                httpsIndex
            } else {
                -1
            }

            if (urlStartIndex > 0) {
                val partBefore = trimmed.substring(0, urlStartIndex).trim()
                val url = trimmed.substring(urlStartIndex).trim()
                val title = partBefore.removeSuffix("-").removeSuffix(":").trim()
                if (url.isNotEmpty()) {
                    bookmarks.add(Bookmark(title = if (title.isEmpty()) url else title, url = url))
                }
            } else if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                bookmarks.add(Bookmark(title = trimmed, url = trimmed))
            }
        }
        return bookmarks
    }
}
