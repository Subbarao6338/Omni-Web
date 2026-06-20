package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * The Ask search engine.
 */
class AskSearch : BaseSearchEngine(
    "file:///android_asset/ask.webp",
    "http://www.ask.com/web?qsrc=0&o=0&l=dir&qo=NatureBrowser&q=",
    R.string.search_engine_ask
)
