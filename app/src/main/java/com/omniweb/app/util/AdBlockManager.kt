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
            "moatads.com", "taboola.com", "outbrain.com", "adservice.google.com",
            "adnxs.com", "criteo.com", "carbonads.net", "amazon-adsystem.com",
            "pubmatic.com", "rubiconproject.com", "openx.net", "media.net",
            "smartadserver.com", "bidswitch.net", "triplelift.com", "indexww.com"
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

    private class TrieNode {
        val children = HashMap<String, TrieNode>()
        var category: String? = null
    }

    @Volatile
    private var root = TrieNode().apply {
        // Initialize with default domains for unit tests where init() might not be called with assets
        listOf(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "moatads.com", "taboola.com", "outbrain.com", "adservice.google.com",
            "adnxs.com", "criteo.com", "carbonads.net", "amazon-adsystem.com",
            "pubmatic.com", "rubiconproject.com", "openx.net", "media.net",
            "smartadserver.com", "bidswitch.net", "triplelift.com", "indexww.com"
        ).forEach { insertToTrie(this, it, "[Ad]") }
        listOf(
            "google-analytics.com", "googletagmanager.com", "hotjar.com", "clarity.ms",
            "mixpanel.com", "amplitude.com", "segment.com"
        ).forEach { insertToTrie(this, it, "[Analytics]") }
        listOf("facebook.com", "fbcdn.net", "ads-twitter.com").forEach { insertToTrie(this, it, "[Social]") }
    }

    @Volatile
    private var bloomFilter: DefaultBloomFilter<String>? = null

    @Volatile
    private var initJob: Job? = null

    fun init(context: Context): Job {
        val existingJob = initJob
        if (existingJob != null && (existingJob.isActive || existingJob.isCompleted)) {
            return existingJob
        }

        return synchronized(this) {
            initJob ?: CoroutineScope(Dispatchers.IO).launch {
                val parser = HostsFileParser()

                val loadHosts1 = async { loadHosts(context, "hosts.txt", ADS_DOMAINS, parser) }
                val loadHosts2 = async { loadHosts(context, "malware.txt", MALWARE_DOMAINS, parser) }

                awaitAll(loadHosts1, loadHosts2)

                val newRoot = TrieNode()
                ADS_DOMAINS.forEach { insertToTrie(newRoot, it, "[Ad]") }
                ANALYTICS_DOMAINS.forEach { insertToTrie(newRoot, it, "[Analytics]") }
                SOCIAL_DOMAINS.forEach { insertToTrie(newRoot, it, "[Social]") }
                MALWARE_DOMAINS.forEach { insertToTrie(newRoot, it, "[Malware]") }
                root = newRoot

                val totalSize = ADS_DOMAINS.size + ANALYTICS_DOMAINS.size + SOCIAL_DOMAINS.size + MALWARE_DOMAINS.size
                bloomFilter = DefaultBloomFilter(
                    numberOfElements = totalSize.coerceAtLeast(50000),
                    falsePositiveRate = 0.01,
                    hashingAlgorithm = MurmurHashStringAdapter()
                ).apply {
                    ADS_DOMAINS.forEach { put(it) }
                    ANALYTICS_DOMAINS.forEach { put(it) }
                    SOCIAL_DOMAINS.forEach { put(it) }
                    MALWARE_DOMAINS.forEach { put(it) }
                }
            }.also { initJob = it }
        }
    }

    private fun insertToTrie(root: TrieNode, domain: String, category: String) {
        val parts = domain.lowercase().removeSuffix(".").split('.').reversed()
        var current = root
        for (part in parts) {
            if (part.isBlank()) continue
            current = current.children.getOrPut(part) { TrieNode() }
        }
        current.category = category
    }

    suspend fun awaitIdling() {
        initJob?.join()
    }

    fun isInitialized(): Boolean = initJob?.isCompleted == true && bloomFilter != null

    private fun loadHosts(context: Context, fileName: String, targetSet: MutableSet<String>, parser: HostsFileParser) {
        try {
            context.assets.open(fileName).use { inputStream ->
                parser.parseInput(InputStreamReader(inputStream)).forEach {
                    val domain = it.lowercase().removeSuffix(".")
                    if (domain.isNotBlank()) {
                        targetSet.add(domain)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Failed to load hosts: $fileName", e)
        }
    }

    fun getAllBlockedDomains(): Set<String> {
        val result = HashSet<String>(ADS_DOMAINS.size + ANALYTICS_DOMAINS.size + SOCIAL_DOMAINS.size + MALWARE_DOMAINS.size)
        result.addAll(ADS_DOMAINS)
        result.addAll(ANALYTICS_DOMAINS)
        result.addAll(SOCIAL_DOMAINS)
        result.addAll(MALWARE_DOMAINS)
        return result
    }

    fun getCategory(host: String): String? {
        if (host.isEmpty()) return null
        val lowerHost = host.lowercase().removeSuffix(".")

        val parts = lowerHost.split('.')
        var current = root
        for (i in parts.indices.reversed()) {
            val part = parts[i]
            if (part.isBlank()) continue
            current = current.children[part] ?: break
            if (current.category != null) return current.category
        }

        return null
    }

    fun shouldBlock(host: String): Boolean {
        if (host.isEmpty()) return false
        val lowerHost = host.lowercase().removeSuffix(".")

        // Fast path: iterative suffix check in Bloom Filter
        var currentHost = lowerHost
        while (currentHost.contains('.')) {
            if (bloomFilter?.mightContain(currentHost) == true) return true
            val firstDot = currentHost.indexOf('.')
            if (firstDot == -1 || firstDot == currentHost.length - 1) break
            currentHost = currentHost.substring(firstDot + 1)
        }
        if (bloomFilter?.mightContain(currentHost) == true) return true

        // Fallback to Trie for precise matching
        return getCategory(lowerHost) != null
    }

    @Volatile
    private var adBlockScript: String? = null

    fun getAdBlockScript(context: Context? = null): String {
        adBlockScript?.let { return it }
        if (context == null) return "" // Should have been initialized

        return try {
            context.assets.open("AdBlock.js").use { inputStream ->
                InputStreamReader(inputStream).readText().also { adBlockScript = it }
            }
        } catch (e: Exception) {
            LogUtils.e("Failed to load AdBlock.js", e)
            ""
        }
    }
}
