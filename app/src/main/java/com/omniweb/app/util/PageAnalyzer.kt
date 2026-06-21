package com.omniweb.app.util

import android.webkit.WebView
import org.json.JSONObject

data class AnalysisResult(
    val seoScore: Int,
    val seoIssues: List<String>,
    val accessibilityScore: Int,
    val accessibilityIssues: List<String>,
    val performanceMetrics: Map<String, String>,
    val wordCount: Int,
    val readabilityScore: String
)

object PageAnalyzer {
    fun analyze(webView: WebView, callback: (AnalysisResult) -> Unit) {
        val script = """
            (function() {
                const result = {
                    seoScore: 0,
                    seoIssues: [],
                    accessibilityScore: 0,
                    accessibilityIssues: [],
                    performanceMetrics: {},
                    wordCount: 0,
                    readabilityScore: ""
                };

                // Real SEO Analysis
                const title = document.title;
                if (title) result.seoScore += 20;
                else result.seoIssues.push("Missing page title");

                const metaDesc = document.querySelector('meta[name="description"]');
                if (metaDesc) result.seoScore += 20;
                else result.seoIssues.push("Missing meta description");

                const h1 = document.querySelectorAll('h1');
                if (h1.length === 1) result.seoScore += 20;
                else if (h1.length === 0) result.seoIssues.push("Missing H1 heading");
                else result.seoIssues.push("Multiple H1 headings detected");

                const alts = document.querySelectorAll('img:not([alt])');
                if (alts.length === 0) result.seoScore += 20;
                else result.seoIssues.push(alts.length + " images missing alt text");
                
                result.seoScore = Math.min(result.seoScore + 20, 100);

                // Real Accessibility Analysis
                const lang = document.documentElement.lang;
                if (lang) result.accessibilityScore += 30;
                else result.accessibilityIssues.push("Language not defined on HTML tag");
                
                const buttons = document.querySelectorAll('button, a.button');
                let smallButtons = 0;
                buttons.forEach(b => { if(b.offsetWidth < 44 || b.offsetHeight < 44) smallButtons++; });
                if (smallButtons === 0) result.accessibilityScore += 40;
                else result.accessibilityIssues.push(smallButtons + " buttons are smaller than 44px");

                result.accessibilityScore = Math.min(result.accessibilityScore + 30, 100);

                // Real Performance Metrics
                const timing = window.performance.timing;
                if (timing) {
                    result.performanceMetrics["Load Time"] = (timing.loadEventEnd - timing.navigationStart) + "ms";
                    result.performanceMetrics["DOM Ready"] = (timing.domContentLoadedEventEnd - timing.navigationStart) + "ms";
                    result.performanceMetrics["TTFB"] = (timing.responseStart - timing.navigationStart) + "ms";
                }

                // Metrics
                const text = document.body.innerText || "";
                result.wordCount = text.split(/\s+/).length;
                
                // Readability (Flesch-Kincaid basic approximation)
                const sentences = text.split(/[.!?]+/).length;
                const avgWordsPerSentence = result.wordCount / sentences;
                if (avgWordsPerSentence < 12) result.readabilityScore = "Easy";
                else if (avgWordsPerSentence < 20) result.readabilityScore = "Moderate";
                else result.readabilityScore = "Complex";
                
                return JSON.stringify(result);
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { json ->
            try {
                val cleanJson = if (json.startsWith("\"") && json.endsWith("\"")) {
                    json.substring(1, json.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                } else {
                    json
                }
                val obj = JSONObject(cleanJson)
                val res = AnalysisResult(
                    seoScore = obj.getInt("seoScore"),
                    seoIssues = obj.getJSONArray("seoIssues").let { arr -> List(arr.length()) { arr.getString(it) } },
                    accessibilityScore = obj.getInt("accessibilityScore"),
                    accessibilityIssues = obj.getJSONArray("accessibilityIssues").let { arr -> List(arr.length()) { arr.getString(it) } },
                    performanceMetrics = obj.getJSONObject("performanceMetrics").let { p -> 
                        val map = mutableMapOf<String, String>()
                        p.keys().forEach { map[it] = p.getString(it) }
                        map
                    },
                    wordCount = obj.getInt("wordCount"),
                    readabilityScore = obj.getString("readabilityScore")
                )
                callback(res)
            } catch (e: Exception) {
                LogUtils.e("Analysis failed for JSON: $json", e)
            }
        }
    }
}
