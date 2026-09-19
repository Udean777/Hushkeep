package com.ssajudn.hushkeep.core.common

import java.util.Locale

object UsernamePolicy {
    const val errorMessage: String =
        "Username 3-32 karakter, gunakan huruf, angka, titik, garis bawah, atau tanda hubung."

    fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun isValid(value: String): Boolean =
        USERNAME_PATTERN.matches(normalize(value))

    private val USERNAME_PATTERN = Regex("^[a-z0-9._-]{3,32}$")
}
