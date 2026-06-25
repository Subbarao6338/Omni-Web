package com.omniweb.app.util

import android.content.Context
import android.os.Environment
import android.graphics.Bitmap
import android.graphics.Canvas
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Toast
import com.google.ai.client.generativeai.GenerativeModel
import java.io.File
import java.io.FileOutputStream

object PageUtils {
    fun takeScreenshot(context: Context, webView: WebView, title: String) {
        try {
            val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            val fileName = "Screenshot_${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.png"
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "Screenshot saved: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            LogUtils.e("Failed to take screenshot", e)
            Toast.makeText(context, "Failed to take screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveAsPdf(context: Context, webView: WebView, title: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(title)
        val jobName = "OmniBrowser_Page_" + System.currentTimeMillis()
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    fun saveAsMhtml(context: Context, webView: WebView, title: String): String? {
        val fileName = "${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.mhtml"
        val dir = File(context.getExternalFilesDir(null), "offline")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        webView.saveWebArchive(file.absolutePath)
        Toast.makeText(context, "Saved for offline viewing", Toast.LENGTH_SHORT).show()
        return file.absolutePath
    }

    fun saveAsMarkdown(context: Context, html: String, title: String) {
        val markdown = htmlToMarkdown(html)
        val fileName = "${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.md"
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)
        file.writeText(markdown)
        Toast.makeText(context, "Saved as Markdown: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun htmlToMarkdown(html: String): String {
        val doc = org.jsoup.Jsoup.parse(html)
        val sb = StringBuilder()

        fun convert(element: org.jsoup.nodes.Element, indent: Int = 0) {
            for (node in element.childNodes()) {
                if (node is org.jsoup.nodes.TextNode) {
                    sb.append(node.text())
                } else if (node is org.jsoup.nodes.Element) {
                    when (node.tagName()) {
                        "h1" -> sb.append("\n# ").append(node.text()).append("\n\n")
                        "h2" -> sb.append("\n## ").append(node.text()).append("\n\n")
                        "h3" -> sb.append("\n### ").append(node.text()).append("\n\n")
                        "p" -> sb.append("\n").append(node.text()).append("\n\n")
                        "strong", "b" -> sb.append("**").append(node.text()).append("**")
                        "em", "i" -> sb.append("*").append(node.text()).append("*")
                        "a" -> sb.append("[").append(node.text()).append("](").append(node.attr("href")).append(")")
                        "img" -> sb.append("![").append(node.attr("alt")).append("](").append(node.attr("src")).append(")")
                        "ul" -> {
                            sb.append("\n")
                            convert(node, indent + 1)
                            sb.append("\n")
                        }
                        "li" -> {
                            sb.append("\n").append("  ".repeat(indent)).append("- ")
                            convert(node, indent)
                        }
                        "code" -> sb.append("`").append(node.text()).append("`")
                        "pre" -> sb.append("\n```\n").append(node.text()).append("\n```\n\n")
                        "br" -> sb.append("\n")
                        else -> convert(node, indent)
                    }
                }
            }
        }

        convert(doc.body())
        return sb.toString().trim()
            .replace(Regex("\n{3,}"), "\n\n")
    }

    fun extractArticleContent(html: String): String {
        return ArticleExtractor.extractArticleContent(html)
    }

    @Suppress("DEPRECATION")
    fun takeFullPageScreenshot(context: Context, webView: WebView, title: String) {
        try {
            val scale = webView.scale
            val width = webView.width
            val maxHeight = 12000 // Prevention of OutOfMemoryError
            val height = (webView.contentHeight * scale).toInt().coerceAtMost(maxHeight)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            val fileName = "FullScreenshot_${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.png"
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "Full page screenshot saved: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            LogUtils.e("Failed to take full screenshot", e)
            Toast.makeText(context, "Failed to take full screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun generateSummary(html: String, apiKey: String? = null): String {
        val articleHtml = extractArticleContent(html)
        val text = articleHtml.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

        if (text.length < 100) return "Not enough content to summarize."

        if (!apiKey.isNullOrBlank()) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )
                val prompt = "Summarize the following web page content in a concise way, focusing on the main points. Use bullet points for key takeaways:\n\n$text"
                val response = generativeModel.generateContent(prompt)
                return response.text ?: "AI failed to generate a summary."
            } catch (e: Exception) {
                LogUtils.e("Gemini summary failed", e)
                // Fallback to heuristic
            }
        }

        // Heuristic summarization: Take first few significant sentences and key points
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 20 }
        if (sentences.isEmpty()) return "Content structure is not suitable for summarization."

        val intro = sentences.take(3).joinToString(" ")

        // Find key points (sentences containing keywords or being in list items)
        val keywords = listOf("important", "key", "result", "finally", "because", "therefore", "essential", "main", "feature")
        val keyPoints = sentences.filter { s -> keywords.any { k -> s.contains(k, ignoreCase = true) } }
            .take(3)
            .joinToString("\n• ", prefix = "\n• ")

        val summary = StringBuilder()
        summary.append("📄 AI-Powered Summary (Local)\n\n")
        summary.append(intro)
        if (keyPoints.length > 10) {
            summary.append("\n\nKey Takeaways:")
            summary.append(keyPoints)
        }

        return summary.toString()
    }

    fun generateQRCode(url: String): Bitmap? {
        try {
            val size = 512
            val hints = java.util.HashMap<com.google.zxing.EncodeHintType, Any>()
            hints[com.google.zxing.EncodeHintType.MARGIN] = 1
            val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(url, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            return null
        }
    }
}
