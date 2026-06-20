package com.omniweb.app.util

import com.omniweb.app.data.CustomRedirectEntry

class RedirectManager(private val redirects: List<CustomRedirectEntry>) {
    fun getRedirect(url: String): String? {
        for (redirect in redirects) {
            if (url.contains(redirect.source)) {
                return url.replace(redirect.source, redirect.target)
            }
        }
        return null
    }
}
