package com.nature.browser

import com.nature.browser.preference.IntEnum

/**
 * The available app themes.
 */
enum class AppTheme(override val value: Int) : IntEnum {
    LIGHT(0),
    DARK(1),
    BLACK(2)/*,
    BLUE(3),
    YELLOW(4),
    GREEN(5)*/
}
