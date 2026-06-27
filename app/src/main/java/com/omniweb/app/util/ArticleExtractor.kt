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
                ".breadcrumb", ".tags", ".author-info", ".widget", ".popup", ".modal",
                ".share", ".social", ".ad", ".advert", ".banner", ".cookie", ".paywall",
                "[id*='ad-']", "[class*='ad-']"
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
        val ownText = el.ownText()
        val totalText = el.text()
        
        val words = totalText.split(Regex("\\s+")).filter { it.length > 2 }.size
        if (words < 5) return 0f

        // 1. Core density score
        score += words.toFloat()

        // 2. Structural multipliers
        val pCount = el.select("p").size
        score += pCount * 20f

        val hCount = el.select("h1, h2, h3, h4").size
        score += hCount * 10f

        // 3. Punctuation (prose indicator)
        val punctuation = totalText.count { it == ',' || it == '.' || it == ';' || it == ':' || it == '?' || it == '!' }
        score += punctuation * 3f

        // 4. Link density penalty (strongest factor)
        val linkTextLength = el.select("a").sumOf { it.text().length }
        val totalTextLength = totalText.length.coerceAtLeast(1)
        val linkDensity = (linkTextLength.toFloat() / totalTextLength).coerceIn(0f, 1f)

        if (linkDensity > 0.25f) {
            // Heavier penalty for higher link density
            score *= (1f - linkDensity * 2.0f).coerceAtLeast(0f)
        } else if (linkDensity > 0.1f) {
            score *= (1f - linkDensity * 1.2f).coerceAtLeast(0.1f)
        }

        // 4b. Text-to-tag ratio bonus (High density of text relative to tags)
        val tagCount = el.allElements.size.coerceAtLeast(1)
        val textToTagRatio = words.toFloat() / tagCount.toFloat()
        if (textToTagRatio > 5f) {
            score *= 1.2f
        }

        // 5. Semantic Bonuses/Penalties
        val attrString = (el.className() + " " + el.id() + " " + el.attr("role")).lowercase()
        val positivePatterns = listOf("article", "content", "post", "body", "main", "entry", "story", "text", "description")
        val negativePatterns = listOf("sidebar", "comment", "footer", "menu", "nav", "widget", "promo", "banner", "ad-", "social", "related", "share", "meta", "recommend")

        if (positivePatterns.any { attrString.contains(it) }) score += 150f
        if (negativePatterns.any { attrString.contains(it) }) score -= 200f

        // 6. Image/Media bonus (if within a content block)
        val imgCount = el.select("img").size
        score += imgCount * 10f

        return score
    }
}
