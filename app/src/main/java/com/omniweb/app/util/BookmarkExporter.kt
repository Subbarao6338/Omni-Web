package com.omniweb.app.util

import com.omniweb.app.data.Bookmark

object BookmarkExporter {
    fun exportToHtml(bookmarks: List<Bookmark>): String {
        val sb = java.lang.StringBuilder()
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<!-- This is an automatically generated file.\n")
        sb.append("     It will be read and overwritten.\n")
        sb.append("     DO NOT EDIT! -->\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>Bookmarks</TITLE>\n")
        sb.append("<H1>Bookmarks</H1>\n")
        sb.append("<DL><p>\n")
        for (bookmark in bookmarks) {
            val title = bookmark.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            sb.append("    <DT><A HREF=\"${bookmark.url}\">${title}</A>\n")
        }
        sb.append("</DL><p>\n")
        return sb.toString()
    }

    fun exportToTxt(bookmarks: List<Bookmark>): String {
        val sb = java.lang.StringBuilder()
        for (bookmark in bookmarks) {
            sb.append("${bookmark.title} - ${bookmark.url}\n")
        }
        return sb.toString()
    }
}
