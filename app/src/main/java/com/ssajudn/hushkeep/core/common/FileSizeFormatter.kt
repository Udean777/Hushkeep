package com.ssajudn.hushkeep.core.common

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FileSizeFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes < 1024L) return "${bytes.coerceAtLeast(0L)} B"

        val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
            .coerceIn(1, units.lastIndex)
        val value = bytes / 1024.0.pow(exponent.toDouble())

        return String.format(Locale.getDefault(), "%.1f %s", value, units[exponent])
    }
}
