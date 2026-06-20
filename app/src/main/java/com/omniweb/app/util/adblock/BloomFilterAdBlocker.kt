package com.omniweb.app.util.adblock
import android.content.Context
import android.net.Uri
import com.omniweb.app.util.adblock.hash.MurmurHashStringAdapter
class BloomFilterAdBlocker(private val context: Context) {
    private var bloomFilter: DefaultBloomFilter<String>? = null
    private val adHosts = mutableSetOf<String>()
    init {
        val defaultHosts = listOf("doubleclick.net", "google-analytics.com", "adservice.google.com", "quantserve.com", "scorecardresearch.com")
        adHosts.addAll(defaultHosts)
        bloomFilter = DefaultBloomFilter(
            numberOfElements = adHosts.size.coerceAtLeast(100),
            falsePositiveRate = 0.01,
            hashingAlgorithm = MurmurHashStringAdapter()
        )
        adHosts.forEach { bloomFilter?.put(it) }
    }
    fun isAd(url: String): Boolean {
        val domain = try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return false
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) { return false }
        if (bloomFilter?.mightContain(domain) == true) {
            return adHosts.contains(domain)
        }
        return false
    }
}
