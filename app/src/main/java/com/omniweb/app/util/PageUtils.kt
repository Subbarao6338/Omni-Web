package com.omniweb.app.util

import android.content.Context
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Toast
import java.io.File

object PageUtils {
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
}
