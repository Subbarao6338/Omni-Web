package com.nature.browser.security

import android.content.Context
import org.mozilla.geckoview.ContentBlocking

class TrackerBlocker(private val context: Context) {
    // Extended domain list for tracker blocking, including Fanboy's Annoyance items
    private val blockedDomains = mutableSetOf(
        "doubleclick.net", "google-analytics.com", "facebook.com",
        "adservice.google.com", "quantserve.com", "scorecardresearch.com",
        "adnxs.com", "casalemedia.com", "rubiconproject.com",
        "googlesyndication.com", "googleadservices.com", "taboola.com",
        "outbrain.com", "adroll.com", "criteo.com", "openx.net",
        "pixel.facebook.com", "analytics.google.com", "ad.doubleclick.net",
        "amazon-adsystem.com", "adnxs.com", "ads-twitter.com", "taboola.com",
        "outbrain.com", "smartadserver.com", "pubmatic.com", "yieldmo.com",
        "hotjar.com", "optimizely.com", "clicky.com", "mixpanel.com",
        "clarity.ms", "googletagmanager.com", "adform.net",
        "fanboy.co.nz", "adblockplus.org", "easylist.to", "pgl.yoyo.org"
    )

    private val uBlockRules = mutableListOf<String>()

    init {
        // Basic uBlock syntax rules
        uBlockRules.add("||googlesyndication.com^")
        uBlockRules.add("||googleadservices.com^")
        uBlockRules.add("||taboola.com^")
        uBlockRules.add("||outbrain.com^")

        // Peter Lowe's tracker list items (subset)
        blockedDomains.addAll(listOf(
            "101com.com", "101order.com", "123log.de", "123stat.com",
            "247-inc.net", "24log.com", "24log.de", "2o7.net",
            "360yield.com", "ad-score.com", "ad-vantage.org", "ad-world.ws",
            "ad-x.co.uk", "ad-xs.com", "ad.777-luck.com", "ad.99.com",
            "ad.admitad.com", "ad.afy11.net", "ad.amung.us", "ad.as-us.com"
        ))

        // Additional Fanboy's and EasyList items
        uBlockRules.add("||ad-delivery.net^")
        uBlockRules.add("||ad-maven.com^")
        uBlockRules.add("||ad-score.com^")
        uBlockRules.add("||adapi.org^")
    }

    fun getSettings(): ContentBlocking.Settings {
        return ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .safeBrowsing(ContentBlocking.SafeBrowsing.MALWARE)
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)
            .build()
    }

    fun shouldBlock(url: String): Boolean {
        val uri = try { android.net.Uri.parse(url) } catch (e: Exception) { null } ?: return false
        val host = uri.host?.lowercase() ?: return false

        // Match exceptions first (@@||domain^)
        for (rule in uBlockRules) {
            if (rule.startsWith("@@||")) {
                val domain = rule.substring(4).split("^")[0]
                if (host == domain || host.endsWith(".$domain")) return false
            }
        }

        // Match static domains
        if (blockedDomains.any { host == it || host.endsWith(".$it") }) return true

        // Match basic uBlock syntax ||domain^ and handle third-party checks
        for (rule in uBlockRules) {
            if (rule.startsWith("||") && !rule.startsWith("@@")) {
                val parts = rule.substring(2).split("^")
                val domain = parts[0]
                val options = if (parts.size > 1) parts[1] else ""

                if (host == domain || host.endsWith(".$domain")) {
                    // Only block if it doesn't require third-party context we don't have
                    if (options.contains("\$third-party")) {
                        // For now, we skip third-party rules to avoid over-blocking without context
                        continue
                    }
                    return true
                }
            }
        }

        return false
    }
}
