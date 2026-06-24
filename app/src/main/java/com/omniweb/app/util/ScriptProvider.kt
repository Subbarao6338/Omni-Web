package com.omniweb.app.util

import android.content.Context
import java.io.InputStreamReader

class ScriptProvider(private val context: Context) {
    private val scriptCache = mutableMapOf<String, String>()

    fun getScript(name: String): String {
        return scriptCache.getOrPut(name) {
            try {
                val inputStream = context.assets.open(name)
                InputStreamReader(inputStream).use { it.readText() }
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun getAllInjectedScripts(
        blockAMP: Boolean = false,
        cookieBlock: Boolean = false,
        textReflow: Boolean = false,
        invertPage: Boolean = false,
        deepDarkMode: Boolean = false,
        adBlockEnabled: Boolean = false
    ): String {
        val sb = StringBuilder()
        sb.append("(function() {\n")
        if (cookieBlock) sb.append(getScript("CookieBlock.js")).append("\n")
        if (textReflow) sb.append(getScript("TextReflow.js")).append("\n")
        if (blockAMP) sb.append(getScript("AmpBlock.js")).append("\n")
        if (invertPage) sb.append(getScript("InvertPage.js")).append("\n")

        if (adBlockEnabled) {
            sb.append(AdBlockManager.getAdBlockScript()).append("\n")
        }

        if (deepDarkMode) {
            sb.append("""
                if (!window.omniDeepDark) {
                    window.omniDeepDark = true;
                    const style = document.createElement('style');
                    style.innerHTML = `
                        html, body {
                            background-color: #121212 !important;
                            color: #e0e0e0 !important;
                        }
                        div, section, article, p, span, li, h1, h2, h3, h4, h5, h6 {
                            background-color: transparent !important;
                            color: #e0e0e0 !important;
                        }
                        a {
                            color: #bb86fc !important;
                        }
                        img, video {
                            filter: brightness(0.8) contrast(1.2) !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
            """.trimIndent()).append("\n")
        }
        sb.append("})();")
        return sb.toString()
    }
}
