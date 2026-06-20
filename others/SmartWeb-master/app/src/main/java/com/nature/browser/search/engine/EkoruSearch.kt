package com.nature.browser.search.engine

import com.nature.browser.R

/**
 * The Baidu search engine.
 *
 * See http://www.baidu.com/img/bdlogo.gif for the icon.
 */
class EkoruSearch : BaseSearchEngine(
    "file:///android_asset/ekoru.webp",
    "https://www.ekoru.org/?ext=smartcookie&q=",
    R.string.ekoru
)
