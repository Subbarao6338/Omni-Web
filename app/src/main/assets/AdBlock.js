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
        // Video & Multimedia Ads
        "div[class*='video-ad']", "div[class*='player-ad']", "[id*='player-ads']",
        ".ytp-ad-module", ".ytp-ad-overlay-container",
        // Social & Tracking
        "iframe[src*='facebook.com/plugins']", "iframe[src*='twitter.com/widgets']",
        "[class*='tracking-pixel']", "img[src*='telemetry']",
        // Anti-Adblock / Popups
        "div[class*='cookie-banner']", "div[id*='consent-popup']",
        "[id*='newsletter-modal']", ".sp-newsletter-popup", "div[class*='paywall']",
        // General Ad classes
        ".ad-bar", ".ad-placer", ".ad-placeholder", ".ad-sense", ".ad-space",
        ".ad-zone", ".ad-unit", ".adbox", ".adframe", ".adsense", ".advert",
        ".banner-ad", ".sidebar-ad", ".top-ad", ".bottom-ad",
        ".ad-choice", ".ad-notice", ".ad-info",
        // Specific Providers
        ".yom-ad-help", "#ad-footer", ".ad_text", ".ad_unit", ".ad-header",
        ".commercial-ad-container", ".gpt-ad", ".dfp-ad", ".carbon-ad",
        "div[class*='outbrain_widget']", "div[class*='yahoogemini']"
    ];

    const style = document.createElement('style');
    style.id = 'omni-adblock-style';
    style.innerHTML = selectors.join(', ') + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; width: 0 !important; z-index: -9999 !important; }';
    document.head.appendChild(style);

    function hideAds(root = document) {
        // Handle Shadow DOM
        const allElements = root.querySelectorAll('*');
        allElements.forEach(el => {
            if (el.shadowRoot) {
                hideAds(el.shadowRoot);
            }
        });

        root.querySelectorAll(selectors.join(', ')).forEach(el => {
            el.style.setProperty('display', 'none', 'important');
        });

        // Heuristic: Hide elements that contain "Advertisement" text
        const textNodes = [];
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null, false);
        let node;
        while(node = walker.nextNode()) {
            if (node.textContent.trim() === 'Advertisement' || node.textContent.trim() === 'Sponsored') {
                textNodes.push(node);
            }
        }

        textNodes.forEach(textNode => {
            const el = textNode.parentElement;
            if (el && el.children.length === 0) {
                const container = el.closest('div, section, aside');
                if (container && container.innerText.length < 100) {
                    container.style.setProperty('display', 'none', 'important');
                }
            }
        });
    }

    let timeout = null;
    const observer = new MutationObserver((mutations) => {
        if (timeout) clearTimeout(timeout);
        timeout = setTimeout(() => {
            hideAds();
        }, 500);
    });

    hideAds();
    observer.observe(document.documentElement, { childList: true, subtree: true });

    window.addEventListener('load', () => hideAds());
})();
