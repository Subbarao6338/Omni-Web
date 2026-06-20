package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * The StartPage mobile search engine.
 */
class StartPageMobileSearch : BaseSearchEngine(
    "file:///android_asset/startpage.webp",
    "https://startpage.com/do/m/mobilesearch?language=english&query=",
    R.string.search_engine_startpage_mobile
)
