package com.ssajudn.hushkeep.core.config

import java.util.Locale

object StoragePaths {
    const val PRIVATE_BUCKET = "hushkeep-private"
    const val ORIGINAL_DIRECTORY = "original"

    fun originalObjectPath(userId: String, mediaId: String, extension: String?): String {
        val normalizedExtension = extension
            ?.trim()
            ?.removePrefix(".")
            ?.takeIf(String::isNotBlank)
            ?.let { ".${it.lowercase(Locale.ROOT)}" }
            .orEmpty()

        return "$userId/$mediaId/$ORIGINAL_DIRECTORY$normalizedExtension"
    }
}
