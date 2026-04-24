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
            e.printStackTrace()
            Toast.makeText(context, "Failed to take screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveAsPdf(context: Context, webView: WebView, title: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(title)
        val jobName = "OmniBrowser_Page_" + System.currentTimeMillis()
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    fun saveAsMhtml(context: Context, webView: WebView, title: String) {
        val fileName = "${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.mhtml"
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)
        webView.saveWebArchive(file.absolutePath)
        Toast.makeText(context, "Saved as MHTML: ${file.name}", Toast.LENGTH_SHORT).show()
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
        md = md.replace(Regex("<br.*?>", RegexOption.IGNORE_CASE), "\n")
        md = md.replace(Regex("<[^>]*>", RegexOption.IGNORE_CASE), "") // Strip remaining tags
        return md.trim()
    }

    fun extractArticleContent(html: String): String {
        // Simple logic to extract main content by finding the block with most <p> tags
        // or just stripping clutter. In a real app, this would be more complex.
        val bodyMatch = Regex("<body.*?>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        var content = bodyMatch?.groupValues?.get(1) ?: html

        // Remove unwanted tags
        val tagsToRemove = listOf("script", "style", "nav", "footer", "header", "aside", "iframe", "noscript", "svg", "form", "button")
        tagsToRemove.forEach { tag ->
            content = content.replace(Regex("<$tag.*?>.*?</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        }

        // Heuristic: Find elements with a lot of <p> tags and prioritize them
        // For now, we'll just try to find the <article> tag or the largest <div>
        val articleMatch = Regex("<article.*?>(.*?)</article>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(content)
        if (articleMatch != null) {
            content = articleMatch.groupValues[1]
        } else {
            val mainMatch = Regex("<main.*?>(.*?)</main>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(content)
            if (mainMatch != null) {
                content = mainMatch.groupValues[1]
            }
        }

        // Clean up remaining tags but keep structure
        content = content.replace(Regex("<div.*?>", RegexOption.IGNORE_CASE), "<div>")
        content = content.replace(Regex("<span.*?>", RegexOption.IGNORE_CASE), "<span>")

        return content.trim()
    }
}
