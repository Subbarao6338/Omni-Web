package com.omniweb.app.util

import android.content.Context
import java.io.InputStreamReader

class ScriptProvider(private val context: Context) {

    fun getScript(name: String): String {
        return try {
            val inputStream = context.assets.open(name)
            InputStreamReader(inputStream).use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    fun getAllInjectedScripts(
        blockAMP: Boolean = false,
        cookieBlock: Boolean = false,
        textReflow: Boolean = false,
        darkMode: Boolean = false
    ): String {
        val sb = StringBuilder()
        if (blockAMP) sb.append(getScript("AmpBlock.js")).append("\n")
        if (cookieBlock) sb.append(getScript("CookieBlock.js")).append("\n")
        if (textReflow) sb.append(getScript("TextReflow.js")).append("\n")
        if (darkMode) sb.append(getScript("DarkMode.js")).append("\n")
        return sb.toString()
    }
}
