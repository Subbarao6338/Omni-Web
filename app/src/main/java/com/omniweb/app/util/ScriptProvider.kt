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
        forceLightTheme: Boolean = false,
        forceBlackTheme: Boolean = false,
        adBlockEnabled: Boolean = false
    ): String {
        val sb = StringBuilder()
        sb.append("(function() {\n")
        if (cookieBlock) sb.append(getScript("CookieBlock.js")).append("\n")
        if (textReflow) sb.append(getScript("TextReflow.js")).append("\n")
        if (blockAMP) sb.append(getScript("AmpBlock.js")).append("\n")
        if (invertPage) sb.append(getScript("InvertPage.js")).append("\n")

        if (adBlockEnabled) {
            sb.append(AdBlockManager.getAdBlockScript(context)).append("\n")
        }

        if (forceLightTheme) {
            sb.append("""
                if (!window.omniForceLight) {
                    window.omniForceLight = true;
                    const style = document.createElement('style');
                    style.id = 'omni-force-light';
                    style.innerHTML = `
                        html, body {
                            background-color: #ffffff !important;
                            color: #1a1a1a !important;
                        }
                        * {
                            background-color: transparent !important;
                            color: inherit !important;
                        }
                        input, textarea, select, button {
                            background-color: #f0f0f0 !important;
                            color: #000000 !important;
                            border: 1px solid #ccc !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
            """.trimIndent()).append("\n")
        }

        if (forceBlackTheme) {
            sb.append("""
                if (!window.omniForceBlack) {
                    window.omniForceBlack = true;
                    const style = document.createElement('style');
                    style.id = 'omni-force-black';
                    style.innerHTML = `
                        :root {
                            color-scheme: dark !important;
                        }
                        html, body {
                            background-color: #000000 !important;
                            color: #ffffff !important;
                        }
                        div:not([class*="bg-"]):not([style*="background"]),
                        section:not([class*="bg-"]):not([style*="background"]),
                        article, p, span, li, h1, h2, h3, h4, h5, h6 {
                            background-color: transparent !important;
                            color: #e0e0e0 !important;
                        }
                        input, textarea, select {
                            background-color: #1a1a1a !important;
                            color: #ffffff !important;
                            border: 1px solid #333 !important;
                        }
                        a {
                            color: #8ab4f8 !important;
                        }
                        img, video {
                            filter: brightness(0.7) contrast(1.2) !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
            """.trimIndent()).append("\n")
        }

        if (deepDarkMode && !forceBlackTheme) {
            sb.append("""
                if (!window.omniDeepDark) {
                    window.omniDeepDark = true;
                    const style = document.createElement('style');
                    style.innerHTML = `
                        :root {
                            color-scheme: dark !important;
                        }
                        html, body {
                            background-color: #121212 !important;
                            color: #e0e0e0 !important;
                        }
                        div:not([class*="bg-"]):not([style*="background"]),
                        section:not([class*="bg-"]):not([style*="background"]),
                        article, p, span, li, h1, h2, h3, h4, h5, h6 {
                            background-color: transparent !important;
                            color: #e0e0e0 !important;
                        }
                        input, textarea, select {
                            background-color: #242424 !important;
                            color: #e0e0e0 !important;
                            border: 1px solid #444 !important;
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
