package com.nature.browser.browser

import com.nature.browser.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class PasswordChoice(override val value: Int) : IntEnum {
    NONE(0),
    CUSTOM(1)
}
