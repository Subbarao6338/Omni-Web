package com.omniweb.app

import com.omniweb.app.util.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlUtilsTest {

    private val searchEngine = "https://www.google.com/search?q="

    @Test
    fun testResolveUrl_ValidUrl() {
        assertEquals("https://google.com", UrlUtils.resolveUrl("google.com", searchEngine))
        assertEquals("https://www.github.com", UrlUtils.resolveUrl("www.github.com", searchEngine))
        assertEquals("https://example.com/path", UrlUtils.resolveUrl("https://example.com/path", searchEngine))
    }

    @Test
    fun testResolveUrl_SearchQuery() {
        assertEquals("${searchEngine}hello%20world", UrlUtils.resolveUrl("hello world", searchEngine))
        assertEquals("${searchEngine}testing", UrlUtils.resolveUrl("testing", searchEngine))
    }

    @Test
    fun testResolveUrl_Localhost() {
        assertEquals("http://localhost:8080", UrlUtils.resolveUrl("localhost:8080", searchEngine))
        assertEquals("http://127.0.0.1", UrlUtils.resolveUrl("127.0.0.1", searchEngine))
    }

    @Test
    fun testResolveUrl_AboutHome() {
        assertEquals("about:home", UrlUtils.resolveUrl("", searchEngine))
        assertEquals("about:home", UrlUtils.resolveUrl("   ", searchEngine))
    }

    @Test
    fun testIsBookmarklet() {
        assertEquals(true, UrlUtils.isBookmarklet("javascript:alert(1)"))
        assertEquals(false, UrlUtils.isBookmarklet("https://google.com"))
    }
}
