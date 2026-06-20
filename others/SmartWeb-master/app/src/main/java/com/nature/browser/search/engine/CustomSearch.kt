package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * A custom search engine.
 */
class CustomSearch(queryUrl: String) : BaseSearchEngine(
    "file:///android_asset/smartcookieweb.webp",
    queryUrl,
    R.string.search_engine_custom
)
