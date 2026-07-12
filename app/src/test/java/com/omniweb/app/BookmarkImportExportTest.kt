package com.omniweb.app

import com.omniweb.app.data.Bookmark
import com.omniweb.app.util.BookmarkExporter
import com.omniweb.app.util.NetscapeBookmarkFormatImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class BookmarkImportExportTest {

    @Test
    fun testHtmlExportAndImport() {
        val bookmarks = listOf(
            Bookmark(title = "Google", url = "https://www.google.com"),
            Bookmark(title = "GitHub", url = "https://github.com")
        )

        val htmlContent = BookmarkExporter.exportToHtml(bookmarks)
        assertTrue(htmlContent.contains("https://www.google.com"))
        assertTrue(htmlContent.contains("Google"))
        assertTrue(htmlContent.contains("https://github.com"))
        assertTrue(htmlContent.contains("GitHub"))

        val inputStream = ByteArrayInputStream(htmlContent.toByteArray())
        val imported = NetscapeBookmarkFormatImporter.import(inputStream)

        assertEquals(2, imported.size)
        assertEquals("Google", imported[0].title)
        assertEquals("https://www.google.com", imported[0].url)
        assertEquals("GitHub", imported[1].title)
        assertEquals("https://github.com", imported[1].url)
    }

    @Test
    fun testTxtExportAndImport() {
        val bookmarks = listOf(
            Bookmark(title = "Google", url = "https://www.google.com"),
            Bookmark(title = "GitHub", url = "https://github.com")
        )

        val txtContent = BookmarkExporter.exportToTxt(bookmarks)
        assertTrue(txtContent.contains("Google - https://www.google.com"))
        assertTrue(txtContent.contains("GitHub - https://github.com"))

        val inputStream = ByteArrayInputStream(txtContent.toByteArray())
        val imported = NetscapeBookmarkFormatImporter.import(inputStream)

        assertEquals(2, imported.size)
        assertEquals("Google", imported[0].title)
        assertEquals("https://www.google.com", imported[0].url)
        assertEquals("GitHub", imported[1].title)
        assertEquals("https://github.com", imported[1].url)
    }

    @Test
    fun testTxtImportWithVaryingFormats() {
        val txtContent = """
            Google - https://www.google.com
            GitHub: https://github.com
            https://example.com
        """.trimIndent()

        val inputStream = ByteArrayInputStream(txtContent.toByteArray())
        val imported = NetscapeBookmarkFormatImporter.import(inputStream)

        assertEquals(3, imported.size)

        assertEquals("Google", imported[0].title)
        assertEquals("https://www.google.com", imported[0].url)

        assertEquals("GitHub", imported[1].title)
        assertEquals("https://github.com", imported[1].url)

        assertEquals("https://example.com", imported[2].title)
        assertEquals("https://example.com", imported[2].url)
    }
}
