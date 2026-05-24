package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSourceView(source: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Source") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Page Source", source)
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
            )
        }
    ) { padding ->
        val lines = source.split("\n")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1E1E1E))
                .padding(16.dp)
        ) {
            items(lines) { line ->
                val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                    var currentPos = 0
                    val tagRegex = Regex("<(/?[a-zA-Z0-9]+)([^>]*)>")
                    val attrRegex = Regex("([a-zA-Z0-9-]+)=\"([^\"]*)\"")

                    // Simple syntax highlighting logic
                    val matches = tagRegex.findAll(line)
                    matches.forEach { match ->
                        // Append text before the match
                        append(line.substring(currentPos, match.range.first))

                        // Highlight the tag
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF569CD6))) // Tag name blue
                        append("<${match.groupValues[1]}")
                        pop()

                        // Highlight attributes
                        val attrs = match.groupValues[2]
                        var attrPos = 0
                        attrRegex.findAll(attrs).forEach { attrMatch ->
                            append(attrs.substring(attrPos, attrMatch.range.first))
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF9CDCFE))) // Attr name light blue
                            append(attrMatch.groupValues[1])
                            pop()
                            append("=\"")
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFCE9178))) // Attr value orange
                            append(attrMatch.groupValues[2])
                            pop()
                            append("\"")
                            attrPos = attrMatch.range.last + 1
                        }
                        append(attrs.substring(attrPos))
                        append(">")

                        currentPos = match.range.last + 1
                    }
                    append(line.substring(currentPos))
                }
                Text(
                    text = annotatedString,
                    style = TextStyle(
                        color = Color(0xFFD4D4D4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
