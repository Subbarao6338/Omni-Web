package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * The Google search engine.
 *
 * See https://www.google.com/images/srpr/logo11w.png for the icon.
 */
class GoogleSearch : BaseSearchEngine(
    "file:///android_asset/google.webp",
    "https://www.google.com/search?client=smartcookieweb&ie=UTF-8&oe=UTF-8&q=",
    R.string.search_engine_google
)
