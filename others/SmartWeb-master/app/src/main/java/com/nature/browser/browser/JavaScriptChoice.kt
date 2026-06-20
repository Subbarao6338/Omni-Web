package com.nature.browser.browser

import com.nature.browser.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class JavaScriptChoice(override val value: Int) : IntEnum {
    NONE(0),
    WHITELIST(1),
    BLACKLIST(2)
}
