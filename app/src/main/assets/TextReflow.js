(function() {
    if (window.omniTextReflowApplied) return;
    window.omniTextReflowApplied = true;

    const style = document.createElement('style');
    style.id = 'omni-text-reflow';
    style.innerHTML = `
        html, body {
            overflow-x: hidden !important;
            width: 100vw !important;
            position: relative !important;
        }
        * {
            max-width: 100vw !important;
            box-sizing: border-box !important;
            overflow-wrap: break-word !important;
        }
        img, video, iframe, table, canvas {
            max-width: 100% !important;
            height: auto !important;
        }
    `;
    document.head.appendChild(style);

    function applyReflow() {
        const elements = document.querySelectorAll('p, div, article, section, blockquote');
        const vw = window.innerWidth;

        elements.forEach(el => {
            if (el.offsetWidth > vw) {
                el.style.width = '100%';
                el.style.display = 'block';
                el.style.float = 'none';
                el.style.position = 'static';
            }
        });
    }

    // Run on load and on resize/orientation change
    applyReflow();
    window.addEventListener('resize', applyReflow);
    window.addEventListener('orientationchange', applyReflow);

    // Also observe DOM changes
    const observer = new MutationObserver((mutations) => {
        applyReflow();
    });
    observer.observe(document.body, { childList: true, subtree: true });
})();
