package com.omniweb.app.util

import android.webkit.JavascriptInterface
import com.omniweb.app.data.MediaItem
import org.json.JSONArray
import android.os.Handler
import android.os.Looper

// JavaScript Interface for communication
class WebAppInterface(
    private val onMediaDetected: (List<MediaItem>) -> Unit,
    private val onTextExtracted: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMedia(json: String?) {
        if (json == null) return
        try {
            val array = JSONArray(json)
            val list = mutableListOf<MediaItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(MediaItem(
                    id = obj.optString("id", Math.random().toString()),
                    type = obj.optString("type", "video"),
                    src = obj.getString("src"),
                    title = obj.optString("title", "Media File")
                ))
            }
            handler.post { onMediaDetected(list) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun postText(text: String?) {
        if (text == null) return
        handler.post { onTextExtracted(text) }
    }
}
