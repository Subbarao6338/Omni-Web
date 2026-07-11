package com.omniweb.app.util

import android.net.Uri
import android.util.Patterns

object UrlUtils {
    private val BANGS = mapOf(
        "!g" to "https://www.google.com/search?q=",
        "!ddg" to "https://duckduckgo.com/?q=",
        "!w" to "https://en.wikipedia.org/wiki/Special:Search?search=",
        "!y" to "https://www.youtube.com/results?search_query=",
        "!yt" to "https://www.youtube.com/results?search_query=",
        "!b" to "https://www.bing.com/search?q=",
        "!gh" to "https://github.com/search?q=",
        "!a" to "https://www.amazon.com/s?k=",
        "!reddit" to "https://www.reddit.com/search/?q=",
        "!m" to "https://www.google.com/maps/search/",
        "!map" to "https://www.google.com/maps/search/",
        "!tw" to "https://twitter.com/search?q=",
        "!imdb" to "https://www.imdb.com/find?q=",
        "!stack" to "https://stackoverflow.com/search?q=",
        "!ebay" to "https://www.ebay.com/sch/i.html?_nkw=",
        "!eco" to "https://www.ecosia.org/search?q=",
        "!pkg" to "https://search.nixos.org/packages?query=",
        "!proton" to "https://mail.proton.me/u/0/search/all?keyword=",
        "!ya" to "https://yandex.ru/yandsearch?lr=21411&text=",
        "!yh" to "https://search.yahoo.com/search?p=",
        "!k" to "https://kotlinlang.org/?q=",
        "!android" to "https://developer.android.com/s/results?q=",
        "!dg" to "https://duckduckgo.com/?q=",
        "!so" to "https://stackoverflow.com/search?q=",
        "!mdn" to "https://developer.mozilla.org/search?q=",
        "!duck" to "https://duckduckgo.com/?q="
    )

    fun resolveUrl(input: String, searchEngine: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:home"

        // Handle Bangs
        if (trimmed.startsWith("!")) {
            val spaceIndex = trimmed.indexOf(" ")
            if (spaceIndex != -1) {
                val bang = trimmed.substring(0, spaceIndex)
                val query = trimmed.substring(spaceIndex + 1).trim()
                BANGS[bang]?.let {
                    return it + Uri.encode(query)
                }
            } else {
                BANGS[trimmed]?.let {
                    return it // Just the bang without query, maybe redirect to home?
                }
            }
        }

        if (trimmed.startsWith("about:") || trimmed.startsWith("javascript:")) {
            return trimmed
        }

        if (trimmed.startsWith("chrome://")) {
            if (trimmed == "chrome://home" || trimmed == "chrome://home/") {
                return "about:home"
            }
            return trimmed
        }

        if (trimmed.contains("://")) {
            return trimmed
        }

        val ipRegex = Regex("""^(\d{1,3}\.){3}\d{1,3}(:\d+)?$""")
        val portRegex = Regex(""".*:\d+$""")
        val pathRegex = Regex(""".*/.*""")
        val paramRegex = Regex(""".*\?.*=.*""")
        val isLocalhost = trimmed.startsWith("localhost") || trimmed.startsWith("127.0.0.1") || ipRegex.matches(trimmed)

        val commonTlds = setOf("com", "org", "net", "edu", "gov", "io", "me", "co", "info", "biz", "us", "uk", "ca", "de", "jp", "fr", "au", "in", "it", "nl", "br")

        // Comprehensive check for URLs vs Search queries
        val isLikelyUrl = !trimmed.contains(" ") && (
            (trimmed.contains(".") && (
                trimmed.substringAfterLast(".").substringBefore("/").substringBefore("?").lowercase() in commonTlds ||
                (trimmed.substringAfterLast(".").substringBefore("/").substringBefore("?").all { it.isLetterOrDigit() } &&
                 trimmed.substringAfterLast(".").substringBefore("/").substringBefore("?").length >= 2)
            )) ||
            isLocalhost ||
            portRegex.matches(trimmed) ||
            trimmed.startsWith("/") ||
            (pathRegex.matches(trimmed) && trimmed.contains(".")) ||
            paramRegex.matches(trimmed)
        )

        if (isLikelyUrl) {
            val protocol = if (isLocalhost || portRegex.matches(trimmed) || trimmed.startsWith("127.")) "http://" else "https://"
            return if (trimmed.startsWith("/")) "file://$trimmed" else protocol + trimmed
        }

        val encoded = try {
            Uri.encode(trimmed)
        } catch (e: Exception) {
            trimmed.replace(" ", "%20")
        }
        return "$searchEngine$encoded"
    }

    fun isBookmarklet(url: String): Boolean {
        return url.trim().startsWith("javascript:", ignoreCase = true)
    }
}
