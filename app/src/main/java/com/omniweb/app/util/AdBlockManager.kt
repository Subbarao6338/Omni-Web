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

    suspend fun awaitIdling() {
        initJob?.join()
    }

    fun isInitialized(): Boolean = initJob?.isCompleted == true && bloomFilter != null

    private fun loadHosts(context: Context, fileName: String, targetSet: MutableSet<String>, parser: HostsFileParser) {
        try {
            context.assets.open(fileName).use { inputStream ->
                parser.parseInput(InputStreamReader(inputStream)).forEach {
                    targetSet.add(it)
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
        val lowerHost = host.lowercase()

        // 1. Try full host first
        getDirectCategory(lowerHost)?.let { return it }

        // 2. Try parent domains (e.g., ad.doubleclick.net -> doubleclick.net)
        var dotIdx = lowerHost.indexOf('.')
        while (dotIdx != -1 && dotIdx < lowerHost.length - 1) {
            val suffix = lowerHost.substring(dotIdx + 1)
            if (suffix.isEmpty()) break

            getDirectCategory(suffix)?.let { return it }

            dotIdx = lowerHost.indexOf('.', dotIdx + 1)
        }
        return null
    }

    private fun getDirectCategory(lowerHost: String): String? {
        // Fast path check using Bloom Filter if initialized
        val filter = bloomFilter
        if (filter != null && !filter.mightContain(lowerHost)) {
            return null
        }

        // Precise check against individual sets
        if (MALWARE_DOMAINS.contains(lowerHost)) return "[Malware]"
        if (ADS_DOMAINS.contains(lowerHost)) return "[Ad]"
        if (ANALYTICS_DOMAINS.contains(lowerHost)) return "[Analytics]"
        if (SOCIAL_DOMAINS.contains(lowerHost)) return "[Social]"

        return null
    }

    fun shouldBlock(host: String): Boolean {
        return getCategory(host) != null
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
