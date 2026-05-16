package com.omniweb.app.util

import android.webkit.JavascriptInterface
import com.omniweb.app.data.MediaItem
import org.json.JSONArray
import android.os.Handler
import android.os.Looper

// JavaScript Interface for communication
class WebAppInterface(
    private val onMediaDetected: (List<MediaItem>) -> Unit,
    private val onTextExtracted: (String) -> Unit,
    private val onLoginFormDetected: (String, String) -> Unit = { _, _ -> }
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMedia(json: String?) {
        if (json == null || json.length > 100000) return // Basic length limit
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
            LogUtils.e("Error parsing media items in WebAppInterface", e)
        }
    }

    @JavascriptInterface
    fun postText(text: String?) {
        if (text == null || text.length > 500000) return
        handler.post { onTextExtracted(text) }
    }

    @JavascriptInterface
    fun onLoginDetected(user: String?, pass: String?) {
        if (user != null && pass != null) {
            // Basic sanitization/length limit
            val sanitizedUser = if (user.length > 255) user.substring(0, 255) else user
            val sanitizedPass = if (pass.length > 255) pass.substring(0, 255) else pass
            handler.post { onLoginFormDetected(sanitizedUser, sanitizedPass) }
        }
    }
}
