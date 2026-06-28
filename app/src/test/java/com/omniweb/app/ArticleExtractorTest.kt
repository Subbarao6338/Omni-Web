package com.omniweb.app

import com.omniweb.app.util.ArticleExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleExtractorTest {

    @Test
    fun testExtractArticleContent_Basic() {
        val html = """
            <html>
            <body>
                <header>Navigation</header>
                <article>
                    <h1>Test Title</h1>
                    <p>This is a test paragraph that should be extracted because it is within an article tag and has some length to it.</p>
                    <p>Another paragraph with more content to ensure it meets the density requirements for the extractor logic.</p>
                </article>
                <footer>Footer</footer>
            </body>
            </html>
        """.trimIndent()
        val extracted = ArticleExtractor.extractArticleContent(html)
        assertEquals(true, extracted.contains("Test Title"))
        assertEquals(true, extracted.contains("test paragraph"))
        assertEquals(false, extracted.contains("Navigation"))
        assertEquals(false, extracted.contains("Footer"))
    }

    @Test
    fun testExtractArticleContent_DivScore() {
        val html = """
            <html>
            <body>
                <div class="sidebar">Ads and links</div>
                <div id="main-content">
                    <p>Main content paragraph one.</p>
                    <p>Main content paragraph two.</p>
                    <p>Main content paragraph three.</p>
                    <p>Main content paragraph four.</p>
                    <p>Main content paragraph five.</p>
                    <p>Main content paragraph six.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
        val extracted = ArticleExtractor.extractArticleContent(html)
        assertEquals(true, extracted.contains("Main content paragraph"))
        assertEquals(false, extracted.contains("sidebar"))
    }

    @Test
    fun testExtractArticleContent_Complex() {
        val html = """
            <html>
            <head><title>Complex Test</title></head>
            <body>
                <div class="wrapper">
                    <div class="header">Header</div>
                    <div class="container">
                        <aside>Sidebar</aside>
                        <main>
                            <article>
                                <h1 class="title">Complex Test</h1>
                                <div class="content">
                                    <p>This is the actual content that should be extracted reliably even with multiple nested wrappers.</p>
                                    <div class="newsletter-signup">Sign up for our newsletter!</div>
                                </div>
                            </article>
                        </main>
                    </div>
                    <footer class="footer">Footer Info</footer>
                </div>
            </body>
            </html>
        """.trimIndent()
        val extracted = ArticleExtractor.extractArticleContent(html)
        assertEquals(true, extracted.contains("Complex Test"))
        assertEquals(true, extracted.contains("actual content"))
        assertEquals(false, extracted.contains("Sidebar"))
        assertEquals(false, extracted.contains("Header"))
        assertEquals(false, extracted.contains("Footer Info"))
        assertEquals(false, extracted.contains("Sign up for our newsletter"))
    }
}
