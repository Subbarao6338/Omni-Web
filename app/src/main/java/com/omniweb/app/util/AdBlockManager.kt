package com.omniweb.app.util

object AdBlockManager {
    private val ADS_DOMAINS = hashSetOf(
        "doubleclick.net", "googleadservices.com", "adnxs.com", "googlesyndication.com",
        "zedo.com", "amazon-adsystem.com", "adservice.google.com", "ad.doubleclick.net",
        "googleads.g.doubleclick.net", "securepubads.g.doubleclick.net",
        "pagead2.googlesyndication.com", "pubads.g.doubleclick.net", "ads.google.com",
        "pagead2.googleadservices.com", "adservice.com", "adsystem.com",
        "moatads.com", "openx.net", "adroll.com", "outbrain.com", "taboola.com",
        "advertising.com", "adtech.de", "adtechus.com", "yieldmanager.com", "pubmatic.com",
        "ad-delivery.net", "adform.net", "adservice.com", "adspirit.de", "adtarget.me",
        "adthor.com", "ad-up.com", "advid.tv", "adzerk.net", "affise.com",
        "rubiconproject.com", "smartadserver.com", "criteo.com", "casalemedia.com",
        "atdmt.com", "adnxs-simple.com", "adgrx.com", "bidtheatre.com", "bidvertiser.com",
        "adhigh.net", "adinall.com", "adition.com", "admanmedia.com", "admicro.vn",
        "admixer.net", "adotmob.com", "adperium.com", "adriver.ru", "adrtx.com",
        "ads-pixie.com", "ads-union.com", "ads-zero.com", "adsafeprotected.com",
        "adsrvr.org", "adswizz.com", "adsymptotic.com", "bidswitch.net", "bluekai.com",
        "gumgum.com", "indexww.com", "lijit.com", "media.net", "mopub.com", "popads.net",
        "revcontent.com", "sharethrough.com", "sovrn.com",
        "adcolony.com", "applovin.com", "chartboost.com", "fyber.com", "ironsrc.com",
        "unityads.unity3d.com", "vungle.com", "flurry.com", "inmobi.com", "tapjoy.com",
        "mgid.com", "propellerads.com", "popcash.net", "yandex.ru", "mail.ru",
        "serving-sys.com", "contextweb.com", "adcash.com", "adsterra.com", "ad-maven.com",
        "clickadu.com", "hilltopads.com", "evadav.com", "activerevenue.com",
        "shorte.st", "adf.ly", "bitly.com", "tinyurl.com", "t.co",
        "yieldmo.com", "mediavine.com", "adthrive.com", "monetizemore.com", "ezoic.com",
        "buysellads.com", "srv.buysellads.com", "exponential.com", "dotomi.com", "quantcount.com",
        "ad-api.com", "ad-score.com", "ad-target.com", "ad-tracker.com", "ad-vent.com",
        "ad-zone.com", "ad.cx", "ad.gt", "ad.style", "ad120m.com", "ad127m.com",
        "taboola.com", "outbrain.com", "mgid.com", "revcontent.com", "popads.net",
        "popcash.net", "adcash.com", "propellerads.com", "adsterra.com", "exoclick.com",
        "cloclo.me", "mobicow.com", "juicyads.com", "ero-advertising.com", "plugrush.com",
        "trafficstars.com", "adnium.com", "adxpansion.com", "twinred.com", "trafficjunky.com",
        "carbonads.net", "buysellads.com", "srv.buysellads.com", "adhigh.net", "adinall.com",
        "popads.net", "popcash.net", "adcash.com", "propellerads.com", "adsterra.com",
        "media.net", "yieldmo.com", "mediavine.com", "adthrive.com", "monetizemore.com",
        "sharethrough.com", "sovrn.com", "indexww.com", "rubiconproject.com", "pubmatic.com"
    )

    private val ANALYTICS_DOMAINS = hashSetOf(
        "google-analytics.com", "analytics.google.com", "googletagmanager.com",
        "googletagservices.com", "hotjar.com", "mouseflow.com", "crazyegg.com",
        "optimizely.com", "mixpanel.com", "segment.com", "clarity.ms", "quantserve.com",
        "scorecardresearch.com", "chartbeat.com", "clicky.com", "newrelic.com",
        "amplitude.com", "statcounter.com", "inspectlet.com", "fullstory.com",
        "bugsnag.com", "sentry.io", "crashlytics.com", "app-measurement.com",
        "crashlytics-reports-pa.googleapis.com", "firebase-settings.crashlytics.com",
        "matomo.org", "piwik.pro", "heap.io", "pendo.io", "logrocket.com", "intercom.io",
        "luckyorange.com", "clicktale.com", "sessionstack.com", "smartlook.com",
        "userway.org", "equalweb.com", "accessibe.com", "audioeye.com",
        "branch.io", "appsflyer.com", "adjust.com", "kochava.com", "singular.net",
        "braze.com", "mparticle.com", "tealiumiq.com", "qualtrics.com", "usercentrics.com",
        "onetrust.com", "cookielaw.org", "trustarc.com", "didomi.io", "civiccomputing.com"
    )

    private val SOCIAL_DOMAINS = hashSetOf(
        "fbcdn.net", "facebook.com", "ads.linkedin.com", "static.ads-twitter.com",
        "ads-twitter.com", "analytics.twitter.com", "analytics.facebook.com",
        "ads-api.twitter.com", "pixel.facebook.com", "connect.facebook.net",
        "snapads.com", "pinterest.com", "tiktok.com", "twimg.com", "t.co",
        "instagram.com", "lnkd.in", "redditstatic.com", "redditmedia.com",
        "doubleclick.net", "snapchat.com", "whatsapp.com", "truthsocial.com",
        "gab.com", "parler.com", "gettr.com", "rumble.com", "bitchute.com"
    )

    fun getCategory(host: String): String? {
        val parts = host.split(".")
        for (i in parts.indices) {
            val domain = parts.subList(i, parts.size).joinToString(".")
            if (ADS_DOMAINS.contains(domain)) return "[Ad]"
            if (ANALYTICS_DOMAINS.contains(domain)) return "[Analytics]"
            if (SOCIAL_DOMAINS.contains(domain)) return "[Social]"
        }
        return null
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
                    "div[id*='ad-wrapper']", "div[class*='ad-wrapper']", ".native-ad",
                    ".ad-slot", ".ad-label", ".ad-text", "div[data-ad-client]", "div[data-ad-slot]",
                    "[class*='advertisement']", "[id*='advertisement']", "div[class*='display-ad']",
                    "iframe[src*='ads']", "iframe[src*='advert']", "iframe[src*='track']",
                    "img[src*='pixel']", "div[class*='tracker']", "div[id*='tracker']",
                    "[aria-label*='Advertisement']", "[title*='Advertisement']", "[id*='AdFrame']",
                    "iframe[id*='aswift']", "iframe[name*='google_ads_frame']",
                    ".ad-box", ".ad-placard", ".ad-sign", ".ad-spacer", ".ad-wrap",
                    "[class*='ad-banner']", "[id*='ad-banner']", "[class*='ad-container']",
                    "[id*='ad-container']", "[class*='ad-content']", "[id*='ad-content']",
                    "[class*='ad-footer']", "[id*='ad-footer']", "[class*='ad-header']",
                    "[id*='ad-header']", "[class*='ad-sidebar']", "[id*='ad-sidebar']"
                ];
                const style = document.createElement('style');
                style.id = 'omni-adblock-style';
                style.innerHTML = selectors.join(', ') + ' { display: none !important; pointer-events: none !important; height: 0 !important; width: 0 !important; opacity: 0 !important; visibility: hidden !important; z-index: -9999 !important; }';
                if (!document.getElementById('omni-adblock-style')) {
                    document.head.appendChild(style);
                }

                // Aggressive element removal
                function clean() {
                    selectors.forEach(s => {
                        document.querySelectorAll(s).forEach(el => {
                            if (el.parentElement) {
                                // el.remove(); // Sometimes too aggressive, display:none is safer
                            }
                        });
                    });
                }
                setInterval(clean, 2000);
            })();
        """.trimIndent()
    }
}
