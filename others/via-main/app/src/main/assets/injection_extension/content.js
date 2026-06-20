// Nature Browser injection script
console.log("Nature Browser injection script active.");

// Prefetch links in viewport for performance
const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting && entry.target.tagName === 'A') {
            const url = entry.target.href;
            if (url && url.startsWith('http')) {
                browser.runtime.sendMessage({ type: "prefetch", url: url });
            }
        }
    });
}, { threshold: 0.1 });

function observeLinks() {
    document.querySelectorAll('a').forEach(link => observer.observe(link));
}

observeLinks();

// Re-observe on DOM changes
const domObserver = new MutationObserver((mutations) => {
    observeLinks();
});
domObserver.observe(document.body, { childList: true, subtree: true });

// Message handling for custom settings
browser.runtime.onMessage.addListener((message) => {
    if (message.type === "inject_css") {
        const style = document.createElement('style');
        style.textContent = message.css;
        document.head.appendChild(style);
    } else if (message.type === "inject_js") {
        const script = document.createElement('script');
        script.textContent = message.js;
        document.body.appendChild(script);
    } else if (message.type === "set_video_speed") {
        const videos = document.querySelectorAll('video');
        videos.forEach(v => v.playbackRate = message.speed);
    } else if (message.type === "show_annotation") {
        highlightText(message.text, message.color);
    } else if (message.type === "get_dom") {
        browser.runtime.sendMessage({ type: "dom_content", html: document.body.innerHTML });
    }
});

document.addEventListener('selectionchange', () => {
    const selection = window.getSelection().toString();
    browser.runtime.sendMessage({ type: "selection_change", text: selection });
});

function highlightText(text, color) {
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
    let node;
    while (node = walker.nextNode()) {
        const index = node.nodeValue.indexOf(text);
        if (index >= 0) {
            const range = document.createRange();
            range.setStart(node, index);
            range.setEnd(node, index + text.length);
            const span = document.createElement('span');
            span.style.backgroundColor = color || '#57CC99';
            span.style.color = 'white';
            range.surroundContents(span);
            break; // For simplicity, only highlight first match
        }
    }
}
