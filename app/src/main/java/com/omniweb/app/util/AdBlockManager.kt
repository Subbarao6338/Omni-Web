package com.omniweb.app.util

import android.content.Context
import com.omniweb.app.util.adblock.DefaultBloomFilter
import com.omniweb.app.util.adblock.HostsFileParser
import com.omniweb.app.util.adblock.hash.MurmurHashStringAdapter
import kotlinx.coroutines.*
import java.io.InputStreamReader
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
            "google-analytics.com", "googletagmanager.com", "hotjar.com", "clarity.ms",
            "mixpanel.com", "amplitude.com", "segment.com"
        ))
    }
    private val SOCIAL_DOMAINS = ConcurrentHashMap.newKeySet<String>().apply {
        addAll(listOf("facebook.com", "fbcdn.net", "ads-twitter.com"))
    }
    private val MALWARE_DOMAINS = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var bloomFilter: DefaultBloomFilter<String>? = null

    @Volatile
    private var initJob: Job? = null

    fun init(context: Context): Job {
        return initJob ?: synchronized(this) {
            initJob ?: CoroutineScope(Dispatchers.IO).launch {
                val parser = HostsFileParser()

                loadHosts(context, "hosts.txt", ADS_DOMAINS, parser)
                loadHosts(context, "malware.txt", MALWARE_DOMAINS, parser)

                val allDomains = getAllBlockedDomains()
                bloomFilter = DefaultBloomFilter(
                    numberOfElements = allDomains.size.coerceAtLeast(1000),
                    falsePositiveRate = 0.01,
                    hashingAlgorithm = MurmurHashStringAdapter()
                ).apply {
                    putAll(allDomains)
                }
            }.also { initJob = it }
        }
    }

    suspend fun awaitIdling() {
        initJob?.join()
    }

    fun isInitialized(): Boolean = initJob?.isCompleted == true && bloomFilter != null

    private fun loadHosts(context: Context, fileName: String, targetSet: MutableSet<String>, parser: HostsFileParser) {
        try {
            context.assets.open(fileName).use { inputStream ->
                val domains = parser.parseInput(InputStreamReader(inputStream))
                targetSet.addAll(domains)
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

        // 1. Try full host first
        getDirectCategory(host)?.let { return it }

        // 2. Try parent domains (e.g., ad.doubleclick.net -> doubleclick.net)
        var dotIdx = host.indexOf('.')
        while (dotIdx != -1 && dotIdx < host.length - 1) {
            val suffix = host.substring(dotIdx + 1)
            if (suffix.isEmpty()) break

            getDirectCategory(suffix)?.let { return it }

            dotIdx = host.indexOf('.', dotIdx + 1)
        }
        return null
    }

    private fun getDirectCategory(host: String): String? {
        // Fast path check using Bloom Filter if initialized
        val filter = bloomFilter
        if (filter != null && !filter.mightContain(host)) {
            return null
        }

        // Precise check against individual sets
        if (MALWARE_DOMAINS.contains(host)) return "[Malware]"
        if (ADS_DOMAINS.contains(host)) return "[Ad]"
        if (ANALYTICS_DOMAINS.contains(host)) return "[Analytics]"
        if (SOCIAL_DOMAINS.contains(host)) return "[Social]"

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
                    "iframe[src*='ads']", "iframe[src*='advert']", "iframe[src*='track']",
                    "[id*='-ad-']", "[class*='-ad-']", "div[class*='sponsored']"
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
