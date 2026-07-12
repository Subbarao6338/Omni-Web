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
    private val onLoginFormDetected: (String, String) -> Unit = { _, _ -> },
    private val onGetAnnotations: () -> String = { "[]" },
    private val onPageReadable: (Boolean) -> Unit = {}
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMedia(json: String?) {
        if (json == null || json.isEmpty() || json.length > 100000) return // Basic length limit
        try {
            val array = JSONArray(json)
            val list = mutableListOf<MediaItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val src = obj.optString("src")
                if (src.isNullOrBlank()) continue

                list.add(MediaItem(
                    id = obj.optString("id", Math.random().toString()),
                    type = obj.optString("type", "video"),
                    src = src,
                    title = obj.optString("title", "Media File")
                ))
            }
            if (list.isNotEmpty()) {
                handler.post { onMediaDetected(list) }
            }
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
    fun getAnnotations(): String {
        return onGetAnnotations()
    }

    @JavascriptInterface
    fun onPageReadable(isReadable: Boolean) {
        handler.post { onPageReadable(isReadable) }
    }

    @JavascriptInterface
    fun onLoginDetected(user: String?, pass: String?) {
        if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            val trimmedUser = user.trim()
            val trimmedPass = pass.trim()

            if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) return

            // Basic sanitization/length limit
            val sanitizedUser = if (trimmedUser.length > 255) trimmedUser.substring(0, 255) else trimmedUser
            val sanitizedPass = if (trimmedPass.length > 255) trimmedPass.substring(0, 255) else trimmedPass
            handler.post { onLoginFormDetected(sanitizedUser, sanitizedPass) }
        }
    }
}
