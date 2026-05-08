package com.omniweb.app.util

object ArticleExtractor {
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
        val priorityTags = listOf("article", "main", "[role='main']", "div#content", "div.content", "div.post", "div.article", "div#main", "div.main")
        for (tag in priorityTags) {
            val pattern = if (tag.contains("#") || tag.contains(".") || tag.contains("[")) {
                val parts = tag.split("#", ".", "[")
                val part = parts.first().ifEmpty { "div" }
                val attrValue = if (tag.contains("#")) tag.split("#").last()
                               else if (tag.contains(".")) tag.split(".").last()
                               else tag.split("[").last().split("=").last().replace("]", "").replace("\"", "").replace("'", "")

                Regex("<$part[^>]*?(?:id|class|role)\\s*=\\s*['\"]\\s*[^'\"]*?$attrValue[^'\"]*?\\s*['\"][^>]*?>(.*?)</$part>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            } else {
                Regex("<$tag.*?>(.*?)</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            }

            val matches = pattern.findAll(content)
            val bestMatch = matches.maxByOrNull { it.groupValues[1].length }
            if (bestMatch != null && bestMatch.groupValues[1].length > 400) {
                content = bestMatch.groupValues[1]
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
            val pBonus = if (pCount > 5) 100 else 0
            val score = (pCount * 25) + (inner.length / 80) - (linkCount * 12) + (imgCount * 8) + tagBonus + pBonus

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
