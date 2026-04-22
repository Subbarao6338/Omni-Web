package com.omniweb.app.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView

object PageUtils {
    fun saveAsPdf(context: Context, webView: WebView, title: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(title)
        val jobName = "OmniBrowser_Page_" + System.currentTimeMillis()
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
