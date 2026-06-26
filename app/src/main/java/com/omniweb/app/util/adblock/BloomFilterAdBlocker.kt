package com.omniweb.app.util.adblock
import android.content.Context
import android.net.Uri
import com.omniweb.app.util.AdBlockManager

class BloomFilterAdBlocker(private val context: Context) {
    init {
        AdBlockManager.init(context)
    }

    fun isAd(url: String): Boolean {
        val host = try {
            Uri.parse(url).host ?: return false
        } catch (e: Exception) { return false }

        return AdBlockManager.shouldBlock(host)
    }
}
