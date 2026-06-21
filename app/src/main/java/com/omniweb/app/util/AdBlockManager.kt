package com.omniweb.app.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object AdBlockManager {
    private val ADS_DOMAINS = hashSetOf<String>()
    private val ANALYTICS_DOMAINS = hashSetOf<String>()
    private val SOCIAL_DOMAINS = hashSetOf<String>()
    private val MALWARE_DOMAINS = hashSetOf<String>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        loadHosts(context, "hosts.txt", ADS_DOMAINS)
        loadHosts(context, "malware.txt", MALWARE_DOMAINS)
        
        // Fallback for analytics/social if not in hosts
        ANALYTICS_DOMAINS.addAll(listOf(
            "google-analytics.com", "analytics.google.com", "googletagmanager.com",
            "googletagservices.com", "hotjar.com", "mouseflow.com", "crazyegg.com",
            "optimizely.com", "mixpanel.com", "segment.com", "clarity.ms", "quantserve.com"
        ))
        
        SOCIAL_DOMAINS.addAll(listOf(
            "fbcdn.net", "facebook.com", "ads.linkedin.com", "static.ads-twitter.com",
            "ads-twitter.com", "analytics.twitter.com", "analytics.facebook.com"
        ))

        isInitialized = true
    }

    private fun loadHosts(context: Context, fileName: String, targetSet: MutableSet<String>) {
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) continue
                
                val parts = trimmedLine.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val host = parts[1]
                    if (host != "localhost" && host != "127.0.0.1") {
                        targetSet.add(host)
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            LogUtils.e("Failed to load hosts: $fileName", e)
        }
    }

    fun getCategory(host: String): String? {
        if (host.isEmpty()) return null

        var current = host
        while (current.contains(".")) {
            if (MALWARE_DOMAINS.contains(current)) return "[Malware]"
            if (ADS_DOMAINS.contains(current)) return "[Ad]"
            if (ANALYTICS_DOMAINS.contains(current)) return "[Analytics]"
            if (SOCIAL_DOMAINS.contains(current)) return "[Social]"
            current = current.substringAfter(".", "")
        }
        return null
    }

    fun shouldBlock(host: String): Boolean {
        return getCategory(host) != null
    }

    fun getAdBlockScript(): String {
        return """
            (function() {
                if (window.omniAdBlockApplied) return;
                window.omniAdBlockApplied = true;

                const selectors = [
                    "div[class*='ad-']", "div[id*='ad-']", "div[class*='Ads']",
                    "div[class*='banner-ad']", "ins.adsbygoogle", "iframe[id*='google_ads']",
                    "div[id*='taboola']", "div[id*='outbrain']", "div[class*='sponsored-content']",
                    "[id^='ad-']", "[class^='ad-']", "[class*='sponsored']", ".trc_rbox_container",
                    "div[id^='google_ads_iframe']", "aside[class*='ad']", "section[class*='ad']",
                    ".ad-container", "[class*='ad-unit']", ".sponsored-content",
                    "div[class*='AdContainer']", "div[class*='promoted']", "div[class*='sponsored']",
                    "iframe[src*='doubleclick.net']", "iframe[src*='googleads']",
                    "div[id*='ad-wrapper']", "div[class*='ad-wrapper']", ".native-ad",
                    ".ad-slot", ".ad-label", ".ad-text", "div[data-ad-client]", "div[data-ad-slot]",
                    "[class*='advertisement']", "[id*='advertisement']", "div[class*='display-ad']",
                    "div[class*='ad-container']", "div[id*='ad-container']", "div[class*='ad-box']",
                    "iframe[src*='ads']", "iframe[src*='advert']", "iframe[src*='track']"
                ];

                const style = document.createElement('style');
                style.id = 'omni-adblock-style';
                style.innerHTML = selectors.join(', ') + ' { display: none !important; pointer-events: none !important; height: 0 !important; width: 0 !important; opacity: 0 !important; visibility: hidden !important; z-index: -9999 !important; }';
                document.head.appendChild(style);

                function hideElement(el) {
                    el.style.setProperty('display', 'none', 'important');
                    el.style.setProperty('visibility', 'hidden', 'important');
                    el.style.setProperty('pointer-events', 'none', 'important');
                }

                const observer = new MutationObserver((mutations) => {
                    mutations.forEach((mutation) => {
                        if (mutation.addedNodes.length) {
                             mutation.addedNodes.forEach(node => {
                                 if (node.nodeType === 1) { 
                                     selectors.forEach(s => {
                                         if (node.matches(s)) hideElement(node);
                                         node.querySelectorAll(s).forEach(el => hideElement(el));
                                     });
                                 }
                             });
                        }
                    });
                });
                observer.observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()
    }
}
