package com.omniweb.app.util

object AdBlockManager {
    private val ADS_DOMAINS = setOf(
        "doubleclick.net", "googleadservices.com", "adnxs.com", "googlesyndication.com",
        "zedo.com", "amazon-adsystem.com", "adservice.google.com", "ad.doubleclick.net",
        "pagead2.googlesyndication.com", "pubads.g.doubleclick.net", "ads.google.com",
        "moatads.com", "openx.net", "adroll.com", "outbrain.com", "taboola.com",
        "advertising.com", "adtech.de", "adtechus.com", "yieldmanager.com", "pubmatic.com",
        "adnxs.com", "carbonads.net", "ad-delivery.net", "adform.net",
        "rubiconproject.com", "smartadserver.com", "criteo.com", "casalemedia.com",
        "atdmt.com", "ad-delivery.net", "adnxs-simple.com", "adform.net", "adgrx.com",
        "adhigh.net", "adinall.com", "adition.com", "admanmedia.com", "admicro.vn",
        "admixer.net", "adotmob.com", "adperium.com", "adriver.ru", "adrtx.com",
        "ads-pixie.com", "ads-union.com", "ads-zero.com", "adsafeprotected.com",
        "adsrvr.org", "adswizz.com", "adsymptotic.com", "bidswitch.net", "bluekai.com",
        "gumgum.com", "indexww.com", "lijit.com", "media.net", "mopub.com", "popads.net",
        "revcontent.com", "rubiconproject.com", "sharethrough.com", "sovrn.com",
        "adcolony.com", "applovin.com", "chartboost.com", "fyber.com", "ironsrc.com",
        "unityads.unity3d.com", "vungle.com", "flurry.com", "inmobi.com", "tapjoy.com",
        "mgid.com", "propellerads.com", "popcash.net", "popads.net", "yandex.ru", "mail.ru",
        "serving-sys.com", "adnxs.com", "contextweb.com", "bidswitch.net", "rubiconproject.com",
        "popads.net", "popcash.net", "adcash.com", "adsterra.com", "ad-maven.com",
        "propellerads.com", "clickadu.com", "hilltopads.com", "evadav.com", "activerevenue.com",
        "ad-maven.com", "shorte.st", "adf.ly", "bitly.com", "tinyurl.com", "t.co",
        "adform.net", "bidswitch.net", "casalemedia.com", "criteo.com", "openx.net",
        "pubmatic.com", "rubiconproject.com", "smartadserver.com", "yieldmo.com"
    )

    private val ANALYTICS_DOMAINS = setOf(
        "google-analytics.com", "analytics.google.com", "googletagmanager.com",
        "googletagservices.com", "hotjar.com", "mouseflow.com", "crazyegg.com",
        "optimizely.com", "mixpanel.com", "segment.com", "clarity.ms", "quantserve.com",
        "scorecardresearch.com", "chartbeat.com", "clicky.com", "newrelic.com",
        "amplitude.com", "statcounter.com", "inspectlet.com", "fullstory.com",
        "bugsnag.com", "sentry.io", "crashlytics.com", "app-measurement.com",
        "matomo.org", "piwik.pro", "heap.io", "pendo.io", "logrocket.com", "intercom.io",
        "fullstory.com", "inspectlet.com", "hotjar.com", "crazyegg.com", "mouseflow.com",
        "luckyorange.com", "clicktale.com", "sessionstack.com", "smartlook.com",
        "userway.org", "equalweb.com", "accessibe.com", "audioeye.com"
    )

    private val SOCIAL_DOMAINS = setOf(
        "fbcdn.net", "facebook.com", "ads.linkedin.com", "static.ads-twitter.com",
        "ads-twitter.com", "analytics.twitter.com", "analytics.facebook.com",
        "ads-api.twitter.com", "pixel.facebook.com", "connect.facebook.net",
        "snapads.com", "pinterest.com", "tiktok.com", "twimg.com", "t.co",
        "instagram.com", "lnkd.in", "redditstatic.com", "redditmedia.com"
    )

    fun getCategory(host: String): String? {
        return when {
            ADS_DOMAINS.any { host.contains(it) } -> "[Ad]"
            ANALYTICS_DOMAINS.any { host.contains(it) } -> "[Analytics]"
            SOCIAL_DOMAINS.any { host.contains(it) } -> "[Social]"
            else -> null
        }
    }

    fun shouldBlock(host: String): Boolean {
        return getCategory(host) != null
    }

    fun getAdBlockScript(): String {
        return """
            (function() {
                const selectors = [
                    "div[class*='ad-']", "div[id*='ad-']", "div[class*='Ads']",
                    "div[class*='banner-ad']", "ins.adsbygoogle", "iframe[id*='google_ads']",
                    "div[id*='taboola']", "div[id*='outbrain']", "div[class*='sponsored-content']",
                    "[id^='ad-']", "[class^='ad-']", "[class*='sponsored']", ".trc_rbox_container",
                    "div[id^='google_ads_iframe']", "aside[class*='ad']", "section[class*='ad']",
                    ".ad-container", "[class*='ad-unit']", ".sponsored-content",
                    "div[class*='AdContainer']", "div[class*='promoted']", "div[class*='sponsored']",
                    "iframe[src*='doubleclick.net']", "iframe[src*='googleads']",
                    "div[id*='ad-wrapper']", "div[class*='ad-wrapper']", ".native-ad"
                ];
                const style = document.createElement('style');
                style.innerHTML = selectors.join(', ') + ' { display: none !important; }';
                document.head.appendChild(style);
            })();
        """.trimIndent()
    }
}
