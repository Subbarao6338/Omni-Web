package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * The StartPage search engine.
 */
class StartPageSearch : BaseSearchEngine(
    "file:///android_asset/startpage.webp",
    "https://startpage.com/do/search?language=english&query=",
    R.string.search_engine_startpage
)
