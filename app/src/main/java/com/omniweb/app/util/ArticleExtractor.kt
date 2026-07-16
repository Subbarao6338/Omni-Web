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
            val junkSelector = "script, style, aside, iframe, noscript, svg, form, " +
                "button, canvas, video, audio, nav, header, footer, " +
                ".ads, .ad-container, #comments, .social-share, .related-posts, " +
                ".newsletter, .trending, .sidebar, .menu, .nav, .footer, .header, " +
                "[aria-hidden='true'], meta, link, input, select, textarea, " +
                ".breadcrumb, .tags, .author-info, .widget, .popup, .modal, " +
                ".share, .social, .ad, .advert, .banner, .cookie, .paywall, " +
                "[id*='ad-'], [class*='ad-'], .cookie-notice, .consent-banner, " +
                ".newsletter-signup, .promotion, [role='complementary'], [role='navigation'], " +
                ".outbrain, .taboola, .revcontent, .z-ad, .recommended-articles, .suggested-content, " +
                ".article-sidebar, .post-sidebar, .entry-sidebar, .right-column, .left-column, " +
                ".newsletter-box, .wp-block-buttons, .wp-block-separator, .sharedaddy, .jp-relatedposts, " +
                ".entry-utility, .entry-related, .inline-ad, .inline-newsletter, " +
                ".cookie-consent, .gdpr, .notice-banner, .top-nav, .bottom-nav, .mobile-nav, " +
                ".search-form, .search-box, .login-box, .signup-box, .user-profile, " +
                ".pagination, .next-post, .prev-post, .related-links, .popular-posts, " +
                ".sidebar-content, .sidebar-widget, .sticky-ad, .floating-ad, .interstitial, " +
                ".survey-box, .feedback-form, .rating-stars, .comment-form, .comment-list"
            doc.select(junkSelector).remove()

            // 2. Scoring Based Candidate Selection
            var bestCandidate: Element? = null
            var bestScore = 0f

            doc.select("div, section, article, main, [role='main'], [role='article']").forEach { el ->
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
                    // Only take elements that are not inside known junk containers that might have survived pre-cleanup
                    var curr = el.parent()
                    var isInsideForbidden = false
                    while (curr != null && curr != finalElement) {
                        val cls = curr.className().lowercase()
                        val id = curr.id().lowercase()
                        if (curr.tagName() == "aside" || curr.tagName() == "nav" ||
                            cls.contains("sidebar") || cls.contains("comment") ||
                            cls.contains("ad-") || id.contains("sidebar") || id.contains("comment")) {
                            isInsideForbidden = true
                            break
                        }
                        curr = curr.parent()
                    }
                    if (isInsideForbidden) return@forEach

                    val parent = el.parent()
                    if (parent == finalElement || parent == null || !allowedTags.contains(parent.tagName())) {
                        // Handle images specially (lazy loading, etc)
                        if (el.tagName() == "img") {
                            val src = el.attr("src")
                            val dataSrc = el.attr("data-src")
                            val dataLazySrc = el.attr("data-lazy-src")
                            val dataOriginal = el.attr("data-original")
                            val dataActualSrc = el.attr("data-actual-src")
                            val fileSrc = el.attr("file-src")
                            val srcset = el.attr("srcset")

                            val finalSrc = when {
                                !dataOriginal.isNullOrBlank() -> dataOriginal
                                !dataActualSrc.isNullOrBlank() -> dataActualSrc
                                !fileSrc.isNullOrBlank() -> fileSrc
                                !dataSrc.isNullOrBlank() -> dataSrc
                                !dataLazySrc.isNullOrBlank() -> dataLazySrc
                                !src.isNullOrBlank() -> src
                                else -> ""
                            }

                            if (finalSrc.isNotBlank()) {
                                el.attr("src", finalSrc)
                                if (srcset.isNotBlank()) el.removeAttr("srcset")
                                el.removeAttr("loading")
                                result.append(el.outerHtml())
                            }
                            return@forEach
                        }

                        // Scoring individual items within candidate
                        if (el.tagName() == "p" && el.text().trim().length < 10) return@forEach
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
        val totalText = el.text()
        
        val words = totalText.split(Regex("\\s+")).filter { it.length > 2 }.size
        if (words < 5) return 0f

        // 1. Core density score
        score += words.toFloat()

        // 2. Structural multipliers
        val pCount = el.select("p").size
        score += pCount * 25f // Increased from 20

        val hCount = el.select("h1, h2, h3, h4").size
        score += hCount * 15f // Increased from 10

        // 3. Punctuation (prose indicator)
        val punctuation = totalText.count { it == ',' || it == '.' || it == ';' || it == ':' || it == '?' || it == '!' }
        val punctuationDensity = if (totalText.length > 0) punctuation.toFloat() / totalText.length else 0f
        score += punctuation * 12f // Increased from 8
        if (punctuationDensity > 0.02) score += 100f // Bonus for high punctuation density (likely prose)

        // 4. Link density penalty (strongest factor)
        val linkTextLength = el.select("a").sumOf { it.text().length }
        val totalTextLength = totalText.length.coerceAtLeast(1)
        val linkDensity = (linkTextLength.toFloat() / totalTextLength).coerceIn(0f, 1f)

        if (linkDensity > 0.35f) {
            score *= 0.01f // Even more aggressive penalty for link farms
        } else if (linkDensity > 0.1f) {
            score *= (1f - linkDensity * 2.5f).coerceAtLeast(0f)
        }

        // 4b. Multi-p bonus (Strong content indicator)
        val directPCount = el.select("> p").size
        if (directPCount > 3) {
            score += 100f // Increased from 50
        }

        // 4c. Text-to-tag ratio bonus (High density of text relative to tags)
        val tagCount = el.allElements.size.coerceAtLeast(1)
        val textToTagRatio = words.toFloat() / tagCount.toFloat()
        if (textToTagRatio > 20f) {
            score *= 2.5f
        } else if (textToTagRatio > 15f) {
            score *= 2.0f
        } else if (textToTagRatio > 10f) {
            score *= 1.5f
        } else if (textToTagRatio > 5f) {
            score *= 1.2f
        }

        // 5. Semantic Bonuses/Penalties
        val attrString = (el.className() + " " + el.id() + " " + el.attr("role")).lowercase()
        val positivePatterns = listOf("article", "content", "post", "body", "main", "entry", "story", "text", "description", "prose", "main-column", "content-area", "paper", "main-content", "article-body", "post-content")
        val negativePatterns = listOf("sidebar", "comment", "footer", "menu", "nav", "widget", "promo", "banner", "ad-", "social", "related", "share", "meta", "recommend", "header", "toolbar", "aside", "navigation", "breadcrumb", "tags", "author", "popup", "modal", "utility")

        if (positivePatterns.any { attrString.contains(it) }) {
            score += 300f
            if (attrString.contains("article") || attrString.contains("story") || attrString.contains("post") || attrString.contains("main-content") || attrString.contains("article-body")) score += 200f
        }
        if (negativePatterns.any { attrString.contains(it) }) {
            score -= 300f
            if (attrString.contains("nav") || attrString.contains("menu") || attrString.contains("sidebar")) score -= 200f
        }

        // 6. Image/Media bonus (if within a content block)
        val imgCount = el.select("img").size
        score += imgCount * 10f

        return score
    }
}
