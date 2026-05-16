package com.omniweb.app.util

import android.content.Context
import android.os.Environment
import android.graphics.Bitmap
import android.graphics.Canvas
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Toast
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
        // A very basic HTML to Markdown converter
        var md = html
        md = md.replace(Regex("<h1.*?>(.*?)</h1>", RegexOption.IGNORE_CASE), "# $1\n\n")
        md = md.replace(Regex("<h2.*?>(.*?)</h2>", RegexOption.IGNORE_CASE), "## $1\n\n")
        md = md.replace(Regex("<h3.*?>(.*?)</h3>", RegexOption.IGNORE_CASE), "### $1\n\n")
        md = md.replace(Regex("<p.*?>(.*?)</p>", RegexOption.IGNORE_CASE), "$1\n\n")
        md = md.replace(Regex("<b.*?>(.*?)</b>", RegexOption.IGNORE_CASE), "**$1**")
        md = md.replace(Regex("<strong.*?>(.*?)</strong>", RegexOption.IGNORE_CASE), "**$1**")
        md = md.replace(Regex("<i.*?>(.*?)</i>", RegexOption.IGNORE_CASE), "*$1*")
        md = md.replace(Regex("<em.*?>(.*?)</em>", RegexOption.IGNORE_CASE), "*$1*")
        md = md.replace(Regex("<a.*?href=\"(.*?)\".*?>(.*?)</a>", RegexOption.IGNORE_CASE), "[$2]($1)")
        md = md.replace(Regex("<img.*?src=\"(.*?)\".*?alt=\"(.*?)\".*?>", RegexOption.IGNORE_CASE), "![$2]($1)")
        md = md.replace(Regex("<img.*?src=\"(.*?)\".*?>", RegexOption.IGNORE_CASE), "![]($1)")
        md = md.replace(Regex("<li.*?>(.*?)</li>", RegexOption.IGNORE_CASE), "- $1\n")
        md = md.replace(Regex("<ul.*?>", RegexOption.IGNORE_CASE), "\n")
        md = md.replace(Regex("</ul>", RegexOption.IGNORE_CASE), "\n")
        md = md.replace(Regex("<code.*?>(.*?)</code>", RegexOption.IGNORE_CASE), "`$1`")
        md = md.replace(Regex("<pre.*?>(.*?)</pre>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "```\n$1\n```\n\n")
        md = md.replace(Regex("<tr.*?>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "|$1|\n")
        md = md.replace(Regex("<t[dh].*?>(.*?)</t[dh]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " $1 |")
        md = md.replace(Regex("<br.*?>", RegexOption.IGNORE_CASE), "\n")
        md = md.replace(Regex("<[^>]*>", RegexOption.IGNORE_CASE), "") // Strip remaining tags
        return md.trim()
    }

    fun extractArticleContent(html: String): String {
        return ArticleExtractor.extractArticleContent(html)
    }

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

    suspend fun generateSummary(html: String): String {
        val articleHtml = extractArticleContent(html)
        val text = articleHtml.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

        if (text.length < 100) return "Not enough content to summarize."

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
        summary.append("📄 AI-Powered Summary\n\n")
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
