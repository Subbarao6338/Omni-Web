package com.omniweb.app.util

import com.omniweb.app.data.Bookmark
import org.json.JSONArray
import org.json.JSONObject

object BookmarkExporter {
    fun exportToJson(bookmarks: List<Bookmark>): String {
        val array = JSONArray()
        bookmarks.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            array.put(obj)
        }
        return array.toString(4)
    }

    fun importFromJson(json: String): List<Bookmark> {
        val list = mutableListOf<Bookmark>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(Bookmark(title = obj.getString("title"), url = obj.getString("url")))
        }
        return list
    }
}
