(function() {
    if (window.omniAdBlockApplied) return;
    window.omniAdBlockApplied = true;

    const selectors = [
        // Common Ad Containers
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
        "[id*='-ad-']", "[class*='-ad-']", "div[class*='sponsored']",
        // Anti-Adblock / Popups
        "div[class*='cookie-banner']", "div[id*='consent-popup']",
        "[id*='newsletter-modal']", ".sp-newsletter-popup", "div[class*='paywall']",
        // General Ad classes
        ".ad-bar", ".ad-placer", ".ad-placeholder", ".ad-sense", ".ad-space",
        ".ad-zone", ".ad-unit", ".adbox", ".adframe", ".adsense", ".advert",
        ".banner-ad", ".sidebar-ad", ".top-ad", ".bottom-ad",
        // Specific Providers
        ".yom-ad-help", "#ad-footer", ".ad_text", ".ad_unit", ".ad-header",
        ".commercial-ad-container", ".gpt-ad", ".dfp-ad", ".carbon-ad"
    ];

    const joinedSelector = selectors.join(', ');
    const style = document.createElement('style');
    style.id = 'omni-adblock-style';
    // Use more aggressive hiding and also target elements with 'Ad' in text
    style.innerHTML = joinedSelector + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; width: 0 !important; z-index: -9999 !important; }';
    document.head.appendChild(style);

    function hideAds() {
        document.querySelectorAll(joinedSelector).forEach(el => {
            el.style.setProperty('display', 'none', 'important');
        });

        // Hide elements that contain "Advertisement" text and are likely ads
        document.querySelectorAll('div, span, p').forEach(el => {
            if (el.children.length === 0 && (el.innerText === 'Advertisement' || el.innerText === 'Sponsored')) {
                const parent = el.parentElement;
                if (parent && parent.children.length < 3) {
                    parent.style.setProperty('display', 'none', 'important');
                }
            }
        });
    }

    const observer = new MutationObserver((mutations) => {
        hideAds();
    });

    hideAds();
    observer.observe(document.documentElement, { childList: true, subtree: true });

    // Additional cleanup after load
    window.addEventListener('load', hideAds);
    setTimeout(hideAds, 2000);
    setTimeout(hideAds, 5000);
})();
