import express from "express";
import axios from "axios";
import * as cheerio from "cheerio";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

// Markdown conversion endpoint
app.get("/api/markdown", async (req, res) => {
  const targetUrl = req.query.url as string;
  if (!targetUrl) return res.status(400).send("URL is required");
  try {
    const response = await axios.get(targetUrl);
    const $ = cheerio.load(response.data);

    // Remove noise
    $("script, style, nav, footer, header, ads, .ads, #ads").remove();

    const html = $("article").html() || $("main").html() || $("body").html() || "";
    res.send(html); // We'll convert to MD on client side for better control
  } catch (error: any) {
    res.status(500).send(`Error fetching for markdown: ${error.message}`);
  }
});

// Proxy endpoint
app.get("/api/proxy", async (req, res) => {
  let targetUrl = req.query.url as string;
  const adBlock = req.query.adblock === 'true';

  // Robust URL extraction from raw request
  // This handles cases where & in the target URL wasn't properly escaped
  const rawUrl = req.url;
  const urlParamIndex = rawUrl.indexOf('url=');
  if (urlParamIndex !== -1) {
    let extracted = rawUrl.substring(urlParamIndex + 4);
    // Remove other proxy params if they exist at the end
    const adBlockIndex = extracted.indexOf('&adblock=');
    if (adBlockIndex !== -1) {
      extracted = extracted.substring(0, adBlockIndex);
    }

    try {
      const decoded = decodeURIComponent(extracted);
      // If the decoded version looks more like a full URL, use it
      if (decoded.startsWith('http') && (!targetUrl || decoded.length > targetUrl.length)) {
        targetUrl = decoded;
      }
    } catch (e) {
      // If decode fails, use the raw extracted if it looks like a URL
      if (extracted.startsWith('http') && (!targetUrl || extracted.length > targetUrl.length)) {
        targetUrl = extracted;
      }
    }
  }

  if (!targetUrl || targetUrl === 'undefined' || targetUrl === 'null' || targetUrl.trim() === '') {
    console.error(`[Proxy] Missing URL. Query:`, req.query, `Raw URL:`, req.url);
    return res.status(400).send("URL is required. Please try refreshing the page or re-entering the URL.");
  }

  // Clean up the URL (sometimes it gets double encoded or has trailing junk)
  targetUrl = targetUrl.trim();
  if (targetUrl.startsWith('"') && targetUrl.endsWith('"')) {
    targetUrl = targetUrl.substring(1, targetUrl.length - 1);
  }

  try {
    const response = await axios.get(targetUrl, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.9",
      },
      responseType: "arraybuffer", // Use arraybuffer to handle images/binary
      maxRedirects: 10,
      validateStatus: () => true,
    });

    // Get the final URL after redirects
    const finalUrl = response.request.res.responseUrl || targetUrl;
    const baseUrl = new URL(finalUrl);
    const contentType = response.headers['content-type'] || 'text/html';

    // Aggressively set permissive security headers
    res.setHeader('X-Frame-Options', 'ALLOWALL');
    res.setHeader('Content-Security-Policy', "frame-ancestors *;");
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Type', contentType);

    // Remove any potentially conflicting headers
    res.removeHeader('X-Content-Security-Policy');
    res.removeHeader('X-WebKit-CSP');
    res.removeHeader('Strict-Transport-Security');

    // If not HTML, just send the data as is
    if (!contentType.includes('text/html')) {
      return res.send(response.data);
    }

    const html = Buffer.from(response.data).toString('utf-8');
    const $ = cheerio.load(html);

    // Remove ALL meta tags that could prevent framing
    $('meta').each((_, el) => {
      const httpEquiv = $(el).attr('http-equiv');
      const name = $(el).attr('name');
      const content = $(el).attr('content');

      if (httpEquiv && /^(content-security-policy|x-frame-options|frame-options|x-ua-compatible)$/i.test(httpEquiv)) {
        $(el).remove();
      }
      if (name && /^(content-security-policy|x-frame-options|frame-options)$/i.test(name)) {
        $(el).remove();
      }
      if (content && /frame-ancestors/i.test(content)) {
        $(el).remove();
      }
    });

    // Anti-frame-busting and location interception
    $('head').prepend(`
      <script>
        (function() {
          // Anti-frame-busting
          try {
            Object.defineProperty(window, "top", { get: function() { return window.self; } });
            Object.defineProperty(window, "parent", { get: function() { return window.self; } });
          } catch (e) {
            window.top = window.self;
            window.parent = window.self;
          }
          window.onbeforeunload = function() { return null; };

          // Intercept location changes (best effort)
          const originalLocation = window.location;
          // We can't easily intercept window.location = '...' but we can try to catch some
        })();
      </script>
    `);

    if (adBlock) {
      // Remove common ad selectors
      const adSelectors = [
        'ins.adsbygoogle', 'div[id^="google_ads"]', 'div[id^="div-gpt-ad"]',
        '.ad-unit', '.ad-box', '.ad-container', '.ad-wrapper', '.adsbox',
        'aside.sidebar-ads', 'div.sponsor-content', 'div.promoted-content',
        'iframe[src*="doubleclick.net"]', 'iframe[src*="googlesyndication.com"]',
        'iframe[src*="adnxs.com"]', 'iframe[src*="taboola.com"]',
        'iframe[src*="outbrain.com"]', 'iframe[src*="amazon-adsystem.com"]'
      ];
      $(adSelectors.join(', ')).remove();
    }

    // Rewrite URLs to stay within proxy
    $("[href], [src], [action], [data-href], [data-src]").each((_, el) => {
      const attrs = ["href", "src", "action", "data-href", "data-src"];
      attrs.forEach(attr => {
        let val = $(el).attr(attr);
        if (!val || val.startsWith('javascript:') || val.startsWith('data:') || val.startsWith('#')) return;

        try {
          let absoluteUrl = "";
          if (val.startsWith("http") || val.startsWith("//")) {
            absoluteUrl = val.startsWith("//") ? "https:" + val : val;
          } else {
            // Use the final URL after redirects for relative resolution
            absoluteUrl = new URL(val, finalUrl).href;
          }

          // Proxy ALL resources to avoid CORS/Mixed Content and broken links
          $(el).attr(attr, `/api/proxy?url=${encodeURIComponent(absoluteUrl)}`);
        } catch (e) {
          // Ignore invalid URLs
        }
      });
    });

    // Handle meta refresh
    $('meta[http-equiv="refresh"]').each((_, el) => {
      const content = $(el).attr('content');
      if (content) {
        const parts = content.split('url=');
        if (parts.length > 1) {
          try {
            const refreshUrl = new URL(parts[1].trim(), finalUrl).href;
            $(el).attr('content', `${parts[0]}url=/api/proxy?url=${encodeURIComponent(refreshUrl)}`);
          } catch (e) {}
        }
      }
    });

    // Inject sniffer script
    const snifferScript = '<script>' +
      '(function() {' +
      '  function sniff() {' +
      '    const media = [];' +
      '    document.querySelectorAll("video, audio, source, [data-src]").forEach(el => {' +
      '      const src = el.src || el.getAttribute("src") || el.getAttribute("data-src");' +
      '      if (src && src.startsWith("http")) {' +
      '        const type = (el.tagName.toLowerCase().includes("video") || src.match(/\\.(mp4|webm|ogg|mov)$|video/i)) ? "video" : "audio";' +
      '        media.push({' +
        '          id: Math.random().toString(36).substr(2, 9),' +
        '          type: type,' +
        '          src: src,' +
        '          title: el.getAttribute("title") || el.getAttribute("aria-label") || document.title || (type.charAt(0).toUpperCase() + type.slice(1) + " File")' +
        '        });' +
        '      }' +
        '    });' +
        '    document.querySelectorAll("img, [style*=\'background-image\'], picture source").forEach(el => {' +
        '      let src = "";' +
        '      if (el.tagName === "IMG") src = el.src || el.getAttribute("src");' +
        '      else if (el.tagName === "SOURCE") src = el.srcset || el.getAttribute("srcset");' +
        '      else {' +
        '        const bg = window.getComputedStyle(el).backgroundImage;' +
        '        if (bg && bg !== "none") {' +
        '          const match = bg.match(/url\\(["\']?(.*?)["\']?\\)/);' +
        '          if (match) src = match[1];' +
        '        }' +
        '      }' +
        '      if (src && src.startsWith("http")) {' +
        '        const isLarge = el.naturalWidth > 100 || el.naturalHeight > 100 || !el.naturalWidth;' +
        '        if (isLarge) {' +
        '          media.push({' +
        '            id: Math.random().toString(36).substr(2, 9),' +
        '            type: "image",' +
        '            src: src,' +
        '            title: el.alt || el.title || "Image File"' +
        '          });' +
        '        }' +
        '      }' +
        '    });' +
        '    const uniqueMedia = [];' +
        '    const seen = new Set();' +
        '    for (const item of media) {' +
        '      if (!seen.has(item.src)) {' +
        '        seen.add(item.src);' +
        '        uniqueMedia.push(item);' +
        '      }' +
        '    }' +
        '    if (uniqueMedia.length > 0) {' +
        '      window.parent.postMessage({ type: "MEDIA_DETECTED", media: uniqueMedia }, "*");' +
        '    }' +
        '  }' +
        '  setInterval(sniff, 3000);' +
        '  sniff();' +
        '  ' +
        '  document.addEventListener("click", (e) => {' +
        '    const link = e.target.closest("a");' +
        '    if (link && link.href && !link.href.startsWith("javascript:") && !link.href.startsWith("#")) {' +
        '      e.preventDefault();' +
        '      let targetUrl = link.href;' +
        '      if (targetUrl.includes("/api/proxy?url=")) {' +
        '        try {' +
          '          const urlObj = new URL(targetUrl, window.location.origin);' +
          '          const extracted = urlObj.searchParams.get("url");' +
          '          if (extracted) targetUrl = extracted;' +
          '        } catch (err) {' +
          '          const match = targetUrl.match(/[?&]url=([^&]+)/);' +
          '          if (match) targetUrl = decodeURIComponent(match[1]);' +
          '        }' +
          '      }' +
          '      if (targetUrl) {' +
          '        window.parent.postMessage({ type: "NAVIGATE_TO", url: targetUrl }, "*");' +
          '      }' +
          '    }' +
          '  }, true);' +
          '  window.addEventListener("message", (event) => {' +
          '    if (event.data?.type === "EXECUTE_SCRIPT") {' +
          '      try { eval(event.data.code); } catch (e) { console.error("User Script Error:", e); }' +
          '    }' +
          '  });' +
          '})();' +
          '</script>';
      $("body").append(snifferScript);

      res.send($.html());
    } catch (error: any) {
      res.status(500).send(`Proxy error: ${error.message}`);
    }
  });

  // Source view endpoint
  app.get("/api/source", async (req, res) => {
    const targetUrl = req.query.url as string;
    if (!targetUrl) return res.status(400).send("URL is required");
    try {
      const response = await axios.get(targetUrl);
      res.set("Content-Type", "text/plain");
      res.send(response.data);
    } catch (error: any) {
      res.status(500).send(`Error fetching source: ${error.message}`);
    }
  });

  // Reader mode endpoint (simplified)
  app.get(["/api/reader", "/reader"], async (req, res) => {
    const targetUrl = getTargetUrl(req);
  app.get("/api/reader", async (req, res) => {
    const targetUrl = req.query.url as string;
    const adBlock = req.query.adblock === 'true';
    if (!targetUrl) return res.status(400).send("URL is required");
    try {
      const response = await axios.get(targetUrl);
      const $ = cheerio.load(response.data);

      // Remove noise
      const noiseSelectors = ["script", "style", "nav", "footer", "header"];
      if (adBlock) {
        noiseSelectors.push("ads", ".ads", "#ads", "ins.adsbygoogle", "div[id^='google_ads']");
      }
      $(noiseSelectors.join(', ')).remove();

      const title = $("title").text() || $("h1").first().text();
      const content = $("article").html() || $("main").html() || $("body").html();

      const readerHtml = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>${title}</title>
          <style>
            body { font-family: 'Georgia', serif; line-height: 1.6; max-width: 800px; margin: 40px auto; padding: 20px; background: #fdfdfd; color: #1a1a1a; }
            h1 { font-size: 2.5em; margin-bottom: 0.5em; }
            img { max-width: 100%; height: auto; border-radius: 8px; }
            pre { background: #f4f4f4; padding: 15px; overflow-x: auto; border-radius: 4px; }
          </style>
          <script>
            window.addEventListener('message', (event) => {
              if (event.data?.type === 'EXECUTE_SCRIPT') {
                try {
                  eval(event.data.code);
                } catch (e) {
                  console.error('User Script Error:', e);
                }
              }
            });
          </script>
        </head>
        <body>
          <h1>${title}</h1>
          <hr>
          ${content}
        </body>
        </html>
      `;
      res.send(readerHtml);
    } catch (error: any) {
      res.status(500).send(`Error fetching reader mode: ${error.message}`);
    }
  });

// Setup development or production environment
async function setupApp() {
  if (process.env.NODE_ENV !== "production") {
    try {
      // @ts-ignore
      const { createServer: createViteServer } = await import("vite");
      const vite = await createViteServer({
        server: { middlewareMode: true },
        appType: "spa",
      });
      app.use(vite.middlewares);
    } catch (e) {
      console.warn("Vite not found, skipping development middleware");
    }
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }
}

setupApp();

// Only listen if not on Vercel
if (process.env.NODE_ENV !== "production" || !process.env.VERCEL) {
  app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

export default app;
