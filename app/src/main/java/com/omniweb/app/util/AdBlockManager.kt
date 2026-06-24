package com.omniweb.app.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object AdBlockManager {
    private val ADS_DOMAINS = ConcurrentHashMap.newKeySet<String>().apply {
        addAll(listOf(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "moatads.com", "taboola.com", "outbrain.com", "adservice.google.com"
        ))
    }
    private val ANALYTICS_DOMAINS = ConcurrentHashMap.newKeySet<String>().apply {
        addAll(listOf(
            "google-analytics.com", "googletagmanager.com", "hotjar.com", "clarity.ms"
        ))
    }
    private val SOCIAL_DOMAINS = ConcurrentHashMap.newKeySet<String>().apply {
        addAll(listOf("facebook.com", "fbcdn.net", "ads-twitter.com"))
    }
    private val MALWARE_DOMAINS = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var initJob: Job? = null

    fun init(context: Context): Job {
        return initJob ?: synchronized(this) {
            initJob ?: CoroutineScope(Dispatchers.IO).launch {
                loadHosts(context, "hosts.txt", ADS_DOMAINS)
                loadHosts(context, "malware.txt", MALWARE_DOMAINS)
            }.also { initJob = it }
        }
    }

    suspend fun awaitIdling() {
        initJob?.join()
    }

    fun isInitialized(): Boolean = initJob?.isCompleted == true

    private fun loadHosts(context: Context, fileName: String, targetSet: MutableSet<String>) {
        try {
            context.assets.open(fileName).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmedLine = line!!.trim()
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) continue

                    // More efficient parsing than split(Regex)
                    val firstSpace = trimmedLine.indexOf(' ')
                    val firstTab = trimmedLine.indexOf('\t')
                    val splitIdx = when {
                        firstSpace != -1 && firstTab != -1 -> minOf(firstSpace, firstTab)
                        firstSpace != -1 -> firstSpace
                        else -> firstTab
                    }

                    if (splitIdx != -1) {
                        val hostPart = trimmedLine.substring(splitIdx).trim()
                        if (hostPart.isNotEmpty()) {
                            // Extract only the domain, ignoring any trailing comments
                            val domain = hostPart.split('#')[0].trim()
                            if (domain != "localhost" && domain != "127.0.0.1" && domain != "0.0.0.0") {
                                targetSet.add(domain)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Failed to load hosts: $fileName", e)
        }
    }

    fun getAllBlockedDomains(): Set<String> {
        return ADS_DOMAINS + ANALYTICS_DOMAINS + SOCIAL_DOMAINS + MALWARE_DOMAINS
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
