package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * The Searx search engine.
 *
 */
class SearxSearch : BaseSearchEngine(
    "file:///android_asset/searx.webp",
    "https://www.searx.be/?q=",
    R.string.search_engine_searx
)
