package com.nature.browser.browser

import com.nature.browser.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class DrawerSizeChoice(override val value: Int) : IntEnum {
    AUTO(0),
    ONE(1),
    TWO(2),
    THREE(3)
}
