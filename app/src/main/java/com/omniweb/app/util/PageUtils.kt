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
        val bodyMatch = Regex("<body.*?>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        var content = bodyMatch?.groupValues?.get(1) ?: html

        // Pre-emptively remove common structural elements that usually don't contain the main article
        content = content.replace(Regex("<header.*?>.*?</header>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        content = content.replace(Regex("<footer.*?>.*?</footer>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        content = content.replace(Regex("<nav.*?>.*?</nav>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

        // Remove non-content elements aggressively
        val tagsToRemove = listOf("script", "style", "aside", "iframe", "noscript", "svg", "form", "button", "canvas", "video", "audio")
        tagsToRemove.forEach { tag ->
            content = content.replace(Regex("<$tag.*?>.*?</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        }

        // Priority tags
        val priorityTags = listOf("article", "main", "[role='main']", "div#content", "div.content", "div.post", "div.article")
        for (tag in priorityTags) {
            val pattern = if (tag.contains("#") || tag.contains(".") || tag.contains("[")) {
                val part = tag.split("#", ".", "[").first()
                val attrValue = if (tag.contains("#")) tag.split("#").last()
                               else if (tag.contains(".")) tag.split(".").last()
                               else tag.split("[").last().split("=").last().replace("]", "").replace("\"", "").replace("'", "")

                Regex("<$part[^>]*?(?:id|class|role)\\s*=\\s*['\"]\\s*[^'\"]*?$attrValue[^'\"]*?\\s*['\"][^>]*?>(.*?)</$part>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            } else {
                Regex("<$tag.*?>(.*?)</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            }

            val match = pattern.find(content)
            if (match != null && match.groupValues[1].length > 400) {
                content = match.groupValues[1]
                break
            }
        }

        // Density-based scoring system for paragraphs and structural elements
        val blocks = Regex("<(div|section|article).*?>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).findAll(content)
        var bestScore = 0
        var bestContent = content

        blocks.forEach { match ->
            val tag = match.groupValues[1].lowercase()
            val inner = match.groupValues[2]
            val pCount = Regex("<p.*?>").findAll(inner).count()
            val imgCount = Regex("<img.*?>").findAll(inner).count()
            val linkCount = Regex("<a.*?>").findAll(inner).count()

            // Heuristic: Paragraphs are good, too many links relative to text is bad (navigation), images are okay
            // article/section tags get a bonus
            val tagBonus = if (tag == "article") 50 else if (tag == "section") 20 else 0
            val score = (pCount * 25) + (inner.length / 80) - (linkCount * 12) + (imgCount * 8) + tagBonus

            if (score > bestScore && inner.length > 150) {
                bestScore = score
                bestContent = inner
            }
        }
        content = bestContent

        // Formatting cleanup
        content = content.replace(Regex("<p.*?>", RegexOption.IGNORE_CASE), "<p>")
        content = content.replace(Regex("<h([1-6]).*?>", RegexOption.IGNORE_CASE), "<h$1>")
        content = content.replace(Regex("<div.*?>", RegexOption.IGNORE_CASE), "<div>")

        return content.trim()
    }
}
