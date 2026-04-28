package com.omniweb.app.util

import android.net.Uri
import android.util.Patterns

object UrlUtils {
    /**
     * Resolves a user input string into a valid URL or a search engine query.
     */
    fun resolveUrl(input: String, searchEngine: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:home"

        // Handle internal about: and javascript: schemes
        if (trimmed.startsWith("about:") || trimmed.startsWith("javascript:")) {
            return trimmed
        }

        // Handle chrome:// schemes by mapping them to about: equivalents or keeping them
        if (trimmed.startsWith("chrome://")) {
            if (trimmed == "chrome://home" || trimmed == "chrome://home/") {
                return "about:home"
            }
            return trimmed
        }

        // If it already has a protocol, return it
        if (trimmed.contains("://")) {
            return trimmed
        }

        // Check for common TLDs or localhost/IPs even if WEB_URL is picky
        val commonTlds = listOf(".com", ".org", ".net", ".io", ".gov", ".edu", ".me", ".info", ".biz", ".ai")
        val isLocalhost = trimmed.startsWith("localhost") || trimmed.startsWith("127.0.0.1")
        val hasCommonTld = commonTlds.any { trimmed.contains(it, ignoreCase = true) }

        // Check if it's a valid URL
        val isUrl = Patterns.WEB_URL.matcher(trimmed).matches()

        // A string is considered a URL if:
        // 1. It matches the WEB_URL pattern OR is localhost OR has a common TLD
        // 2. It contains a dot (for non-localhost)
        // 3. It does not contain spaces
        if ((isUrl || isLocalhost || hasCommonTld) && !trimmed.contains(" ")) {
            if (trimmed.contains(".") || isLocalhost) {
                return if (isLocalhost) "http://$trimmed" else "https://$trimmed"
            }
        }

        // Otherwise, treat as a search query
        return "$searchEngine${Uri.encode(trimmed)}"
    }

    /**
     * Checks if a string is a javascript: bookmarklet.
     */
    fun isBookmarklet(url: String): Boolean {
        return url.trim().startsWith("javascript:", ignoreCase = true)
    }
}
