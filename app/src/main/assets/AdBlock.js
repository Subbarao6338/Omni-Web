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

    const joinedSelector = selectors.join(', ');
    const style = document.createElement('style');
    style.id = 'omni-adblock-style';
    style.innerHTML = joinedSelector + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; width: 0 !important; z-index: -9999 !important; }';
    document.head.appendChild(style);

    const observer = new MutationObserver((mutations) => {
        for (let i = 0; i < mutations.length; i++) {
            const addedNodes = mutations[i].addedNodes;
            for (let j = 0; j < addedNodes.length; j++) {
                const node = addedNodes[j];
                if (node.nodeType === 1) {
                    if (node.matches(joinedSelector)) {
                        node.style.display = 'none';
                    }
                    // For performance, we don't querySelectorAll on every addition
                    // The CSS rule handles most cases automatically.
                }
            }
        }
    });

    observer.observe(document.documentElement, { childList: true, subtree: true });
})();
