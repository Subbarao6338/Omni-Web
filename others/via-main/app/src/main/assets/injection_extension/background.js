browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === "inject_css") {
        browser.tabs.insertCSS({ code: message.code });
    } else if (message.type === "inject_js") {
        browser.tabs.executeScript({ code: message.code });
    } else if (message.type === "prefetch") {
        // Message to host to prefetch URI
        browser.runtime.sendNativeMessage("nature", { type: "prefetch", url: message.url });
    }
});
