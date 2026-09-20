package com.ssajudn.hushkeep.core.common

object OtpPolicy {
    const val LENGTH = 6

    fun normalize(value: String): String = value.filter(Char::isDigit).take(LENGTH)

    fun isValid(value: String): Boolean = value.length == LENGTH && value.all(Char::isDigit)
}
