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
                "[aria-hidden='true']", "meta", "link", "input", "select", "textarea",
                ".breadcrumb", ".tags", ".author-info", ".widget", ".popup", ".modal"
            )
            junkSelectors.forEach { doc.select(it).remove() }

            // 2. Scoring Based Candidate Selection
            var bestCandidate: Element? = null
            var bestScore = 0f

            doc.select("div, section, article, main, [role='main']").forEach { el ->
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
        val words = text.split(Regex("\\s+")).filter { it.length > 2 }.size
        
        // 1. Core density score
        score += words.toFloat()

        // 2. Structural multipliers
        val pCount = el.select("p").size
        score += pCount * 15f

        val hCount = el.select("h1, h2, h3").size
        score += hCount * 5f

        // 3. Punctuation (prose indicator)
        val punctuation = text.count { it == ',' || it == '.' || it == ';' || it == ':' }
        score += punctuation * 2f

        // 4. Link density penalty (strongest factor)
        val linkTextLength = el.select("a").sumOf { it.text().length }
        val totalTextLength = text.length.coerceAtLeast(1)
        val linkDensity = (linkTextLength.toFloat() / totalTextLength).coerceIn(0f, 1f)

        if (linkDensity > 0.2f) {
            score *= (1f - linkDensity * 1.5f).coerceAtLeast(0f)
        }

        // 5. Semantic Bonuses/Penalties
        val attrString = (el.className() + " " + el.id() + " " + el.attr("role")).lowercase()
        val positivePatterns = listOf("article", "content", "post", "body", "main", "entry", "story")
        val negativePatterns = listOf("sidebar", "comment", "footer", "menu", "nav", "widget", "promo", "banner", "ad-", "social", "related")

        if (positivePatterns.any { attrString.contains(it) }) score += 100f
        if (negativePatterns.any { attrString.contains(it) }) score -= 150f

        // 6. Image/Media bonus (if within a content block)
        val imgCount = el.select("img").size
        score += imgCount * 10f

        return score
    }
}
