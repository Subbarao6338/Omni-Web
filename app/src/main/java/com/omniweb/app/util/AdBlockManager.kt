package com.omniweb.app.util

object AdBlockManager {
    private val ADS_DOMAINS = hashSetOf(
        "activerevenue.com", "ad-api.com", "ad-delivery.net", "ad-maven.com", "ad-score.com",
        "ad-target.com", "ad-tracker.com", "ad-up.com", "ad-vent.com", "ad-zone.com",
        "ad.cx", "ad.doubleclick.net", "ad.gt", "ad.style", "ad120m.com", "ad127m.com",
        "adcash.com", "adcolony.com", "adf.ly", "adform.net", "adgrx.com", "adhigh.net",
        "adinall.com", "adinplay.com", "adition.com", "admanmedia.com", "admicro.vn",
        "admixer.net", "adnium.com", "adnxs-simple.com", "adnxs.com", "adotmob.com",
        "adperium.com", "adriver.ru", "adroll.com", "adrtx.com", "ads-pixie.com",
        "ads-union.com", "ads-zero.com", "ads.google.com", "adsafeprotected.com",
        "adservice.com", "adservice.google.com", "adspirit.de", "adsrvr.org", "adsterra.com",
        "adswizz.com", "adsymptotic.com", "adsystem.com", "adtarget.me", "adtech.de",
        "adtechus.com", "adthor.com", "adthrive.com", "advertising.com", "advid.tv",
        "adxpansion.com", "adzerk.net", "affise.com", "amazon-adsystem.com", "applovin.com",
        "atdmt.com", "bidswitch.net", "bidtheatre.com", "bidvertiser.com", "bitly.com",
        "bluekai.com", "buysellads.com", "carbonads.net", "casalemedia.com", "chartboost.com",
        "clickadu.com", "cloclo.me", "contextweb.com", "criteo.com", "dotomi.com",
        "doubleclick.net", "ero-advertising.com", "evadav.com", "exoclick.com", "exponential.com",
        "ezoic.com", "flurry.com", "fyber.com", "googleads.g.doubleclick.net", "googleadservices.com",
        "googlesyndication.com", "gumgum.com", "hilltopads.com", "indexww.com", "inmobi.com",
        "ironsrc.com", "juicyads.com", "lijit.com", "mail.ru", "media.net", "mediavine.com",
        "mgid.com", "moatads.com", "mobicow.com", "monetizemore.com", "mopub.com",
        "openx.net", "outbrain.com", "pagead2.googleadservices.com", "pagead2.googlesyndication.com",
        "plugrush.com", "popads.net", "popcash.net", "propellerads.com", "pubads.g.doubleclick.net",
        "pubmatic.com", "quantcount.com", "revcontent.com", "rubiconproject.com",
        "securepubads.g.doubleclick.net", "serving-sys.com", "sharethrough.com",
        "shorte.st", "smartadserver.com", "sovrn.com", "srv.buysellads.com", "t.co",
        "taboola.com", "tapjoy.com", "tinyurl.com", "trafficjunky.com", "trafficstars.com",
        "twinred.com", "unityads.unity3d.com", "vungle.com", "yandex.ru", "yieldmanager.com",
        "yieldmo.com", "zedo.com", "a.buysellads.com", "ad-delivery.net", "ad.doubleclick.net",
        "ad.yieldlab.net", "adform.net", "adservice.google.com", "ads.pubmatic.com",
        "ads.stickyadstv.com", "amazon-adsystem.com", "bid.g.doubleclick.net",
        "cdn.doubleclick.net", "googleads.g.doubleclick.net", "pagead2.googlesyndication.com",
        "partnerad.l.doubleclick.net", "pubads.g.doubleclick.net", "securepubads.g.doubleclick.net",
        "static.doubleclick.net", "tpc.googlesyndication.com"
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
        "onetrust.com", "cookielaw.org", "trustarc.com", "didomi.io", "civiccomputing.com",
        "api-global.mixpanel.com", "api.mixpanel.com", "collect.doubleclick.net",
        "stats.g.doubleclick.net", "telemetry.mozilla.org", "telemetry.sdk.appcenter.ms"
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
        if (host.isEmpty()) return null

        // Fast path for common non-ad domains
        if (host.endsWith("google.com") || host.endsWith("apple.com") || host.endsWith("microsoft.com")) {
             // Still check subdomains but be careful
             if (host == "google.com" || host == "www.google.com") return null
        }

        var current = host
        while (current.contains(".")) {
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
                    "iframe[src*='ads']", "iframe[src*='advert']", "iframe[src*='track']",
                    "img[src*='pixel']", "div[class*='tracker']", "div[id*='tracker']",
                    "[aria-label*='Advertisement']", "[title*='Advertisement']", "[id*='AdFrame']",
                    "iframe[id*='aswift']", "iframe[name*='google_ads_frame']",
                    ".ad-box", ".ad-placard", ".ad-sign", ".ad-spacer", ".ad-wrap",
                    "[class*='ad-banner']", "[id*='ad-banner']", "[class*='ad-container']",
                    "[id*='ad-container']", "[class*='ad-content']", "[id*='ad-content']",
                    "[class*='ad-footer']", "[id*='ad-footer']", "[class*='ad-header']",
                    "[id*='ad-header']", "[class*='ad-sidebar']", "[id*='ad-sidebar']",
                    "amp-ad", "amp-embed[type='adsense']",
                    "div[class*='ad_box']", "div[class*='ad_container']", "div[class*='ad_slot']",
                    "div[class*='ad_unit']", "div[class*='ad_wrapper']", "div[class*='ad_wrapper_inner']",
                    "div[id*='ad_box']", "div[id*='ad_container']", "div[id*='ad_slot']",
                    "div[id*='ad_unit']", "div[id*='ad_wrapper']", "div[id*='ad_wrapper_inner']",
                    "div[class*='adbox']", "div[class*='adcontainer']", "div[class*='adslot']",
                    "div[class*='adunit']", "div[class*='adwrapper']", "div[class*='adwrapper_inner']",
                    "div[id*='adbox']", "div[id*='adcontainer']", "div[id*='adslot']",
                    "div[id*='adunit']", "div[id*='adwrapper']", "div[id*='adwrapper_inner']",
                    "div[class*='ads_box']", "div[class*='ads_container']", "div[class*='ads_slot']",
                    "div[class*='ads_unit']", "div[class*='ads_wrapper']", "div[class*='ads_wrapper_inner']",
                    "div[id*='ads_box']", "div[id*='ads_container']", "div[id*='ads_slot']",
                    "div[id*='ads_unit']", "div[id*='ads_wrapper']", "div[id*='ads_wrapper_inner']",
                    "div[class*='adsbox']", "div[class*='adscontainer']", "div[class*='adsslot']",
                    "div[class*='adsunit']", "div[class*='adswrapper']", "div[class*='adswrapper_inner']",
                    "div[id*='adsbox']", "div[id*='adscontainer']", "div[id*='adsslot']",
                    "div[id*='adsunit']", "div[id*='adswrapper']", "div[id*='adswrapper_inner']",
                    "div[class*='advert']", "div[id*='advert']", "div[class*='adv']", "div[id*='adv']",
                    "div[class*='advertisement']", "div[id*='advertisement']",
                    "div[class*='ad-box']", "div[class*='ad-container']", "div[class*='ad-slot']",
                    "div[class*='ad-unit']", "div[class*='ad-wrapper']", "div[class*='ad-wrapper-inner']",
                    "div[id*='ad-box']", "div[id*='ad-container']", "div[id*='ad-slot']",
                    "div[id*='ad-unit']", "div[id*='ad-wrapper']", "div[id*='ad-wrapper-inner']"
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

                // MutationObserver for better performance than setInterval
                const observer = new MutationObserver((mutations) => {
                    mutations.forEach((mutation) => {
                        if (mutation.addedNodes.length) {
                             mutation.addedNodes.forEach(node => {
                                 if (node.nodeType === 1) { // Element
                                     selectors.forEach(s => {
                                         if (node.matches(s)) {
                                             hideElement(node);
                                         }
                                         node.querySelectorAll(s).forEach(el => {
                                             hideElement(el);
                                         });
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
