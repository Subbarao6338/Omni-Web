package com.omniweb.app.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object ArticleExtractor {
    fun extractArticleContent(html: String): String {
        try {
            val doc: Document = Jsoup.parse(html)

            // 1. Remove obvious junk
            val tagsToRemove = listOf(
                "script", "style", "aside", "iframe", "noscript", "svg", "form",
                "button", "canvas", "video", "audio", "nav", "header", "footer",
                ".ads", ".ad-container", "#comments", ".social-share", ".related-posts"
            )
            tagsToRemove.forEach { doc.select(it).remove() }

            // 2. Identify candidate
            val prioritySelectors = listOf("article", "main", "[role='main']", "#content", ".content", ".post", ".article", "#main", ".main")
            var bestCandidate: Element? = null

            for (selector in prioritySelectors) {
                val element = doc.select(selector).maxByOrNull { it.text().length }
                if (element != null && element.text().length > 500) {
                    bestCandidate = element
                    break
                }
            }

            if (bestCandidate == null) {
                // Scoring fallback
                var maxScore = 0
                doc.select("div, section").forEach { el ->
                    val text = el.ownText()
                    val pCount = el.select("p").size
                    val linkDensity = calculateLinkDensity(el)
                    val score = (pCount * 20) + (text.length / 50)
                    val finalScore = (score * (1 - linkDensity)).toInt()

                    if (finalScore > maxScore) {
                        maxScore = finalScore
                        bestCandidate = el
                    }
                }
            }

            val finalElement = bestCandidate ?: doc.body()

            // 3. Clean and format the result
            val result = StringBuilder()
            val title = doc.title()
            if (title.isNotEmpty()) {
                result.append("<h1>").append(title).append("</h1>")
            }

            // Only keep certain tags in the final output
            val allowedTags = setOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "img", "blockquote", "pre", "code")

            finalElement.allElements.forEach { el ->
                if (allowedTags.contains(el.tagName())) {
                    // Avoid nested repetitions
                    if (el.parent() == finalElement || !allowedTags.contains(el.parent().tagName())) {
                         result.append(el.outerHtml())
                    }
                }
            }

            return result.toString()
        } catch (e: Exception) {
            return html // Fallback to raw if Jsoup fails
        }
    }

    private fun calculateLinkDensity(el: Element): Float {
        val linkTextLength = el.select("a").sumOf { it.text().length }
        val totalTextLength = el.text().length.coerceAtLeast(1)
        return linkTextLength.toFloat() / totalTextLength
    }
}
