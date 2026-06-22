package com.omniweb.app.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.min

object ArticleExtractor {
    fun extractArticleContent(html: String): String {
        try {
            val doc: Document = Jsoup.parse(html)

            // 1. Pre-cleanup
            val junkSelectors = listOf(
                "script", "style", "aside", "iframe", "noscript", "svg", "form",
                "button", "canvas", "video", "audio", "nav", "header", "footer",
                ".ads", ".ad-container", "#comments", ".social-share", ".related-posts",
                ".newsletter", ".trending", ".sidebar", ".menu", ".nav", ".footer", ".header",
                "[aria-hidden='true']", "meta", "link", "input", "select", "textarea"
            )
            junkSelectors.forEach { doc.select(it).remove() }

            // 2. Scoring Based Candidate Selection
            var bestCandidate: Element? = null
            var bestScore = 0f

            doc.select("div, section, article, main").forEach { el ->
                val score = calculateScore(el)
                if (score > bestScore) {
                    bestScore = score
                    bestCandidate = el
                }
            }

            val finalElement = bestCandidate ?: doc.body()

            // 3. Final Content Refinement
            val result = StringBuilder()
            val title = doc.title()
            if (title.isNotEmpty()) {
                result.append("<h1 class='reader-title'>").append(title).append("</h1>")
            }

            val allowedTags = setOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "img", "blockquote", "pre", "code", "table", "tr", "td", "th")

            finalElement.allElements.forEach { el ->
                if (allowedTags.contains(el.tagName())) {
                    val parent = el.parent()
                    if (parent == finalElement || parent == null || !allowedTags.contains(parent.tagName())) {
                        // Scoring individual items within candidate
                        if (el.tagName() == "p" && el.text().length < 10) return@forEach
                        result.append(el.outerHtml())
                    }
                }
            }

            return result.toString()
        } catch (e: Exception) {
            return html
        }
    }

    private fun calculateScore(el: Element): Float {
        var score = 0f
        val text = el.text()
        val words = text.split(Regex("\\s+")).size
        
        // Text density
        score += words.toFloat()

        // Paragraph count
        val pCount = el.select("p").size
        score += pCount * 10f

        // Commas indicate prose
        val commas = text.count { it == ',' }
        score += commas * 1f

        // Link density (higher density = lower score)
        val linkTextLength = el.select("a").sumOf { it.text().length }
        val totalTextLength = text.length.coerceAtLeast(1)
        val linkDensity = min(linkTextLength.toFloat() / totalTextLength, 1.0f)

        if (linkDensity > 0.3f) {
            score *= (1 - linkDensity)
        }

        // Class/ID Bonuses
        val attrString = (el.className() + " " + el.id()).lowercase()
        if (attrString.contains("content") || attrString.contains("article") || attrString.contains("post") || attrString.contains("body")) {
            score += 50f
        }
        if (attrString.contains("sidebar") || attrString.contains("comment") || attrString.contains("footer") || attrString.contains("menu") || attrString.contains("nav")) {
            score -= 50f
        }

        // Penalty for too many links compared to paragraphs
        if (el.select("a").size > el.select("p").size * 5) {
            score -= 20f
        }

        return score
    }
}
